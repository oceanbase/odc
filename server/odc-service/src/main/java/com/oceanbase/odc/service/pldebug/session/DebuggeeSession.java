/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oceanbase.odc.service.pldebug.session;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.sql.util.OBUtils;
import com.oceanbase.odc.service.db.model.DBMSOutput;
import com.oceanbase.odc.service.pldebug.model.PLDebugResult;
import com.oceanbase.odc.service.pldebug.model.StartPLDebugReq;
import com.oceanbase.odc.service.pldebug.util.PLDebugTask;
import com.oceanbase.odc.service.pldebug.util.PLUtils;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBPLParamMode;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2021/11/18
 */

@Slf4j
public class DebuggeeSession extends AbstractDebugSession {
    // 2 代表 nodebug_on_timeout
    private static final Integer DEBUGGEE_TIMEOUT_BEHAVIOUR = 2;
    private Future<PLDebugResult> resultFuture;
    private static final Integer ERROR_CODE = 600;
    private static final String PL_DEBUGGING_ERROR_CODE = "5036";
    @Setter
    private Integer dbmsoutputMaxRows = null;

    public DebuggeeSession(ConnectionSession connectionSession, ThreadPoolExecutor debugThreadPoolExecutor,
            StartPLDebugReq req) throws Exception {
        // 设置超时时间, 单位：us
        // 设置debug工作线程的超时时间，单位：s 2min
        List<String> initSqls = Arrays.asList(
                String.format("set session ob_query_timeout = %s;", DEBUG_TIMEOUT_MS * 1000),
                String.format("select dbms_debug.set_timeout(%s) from dual;", 120));
        acquireNewConnection(connectionSession, () -> acquireDataSource(connectionSession, initSqls));
        // OceanBaseConnection can accept null as executor
        // 0 for timeout means wait infinitely
        connection.setNetworkTimeout(null, 0);
        try (Statement stmt = connection.createStatement()) {
            // 设置debug工作线程的超时行为
            DBPLParam param = DBPLParam.of("behaviour", DBPLParamMode.IN, "int");
            param.setDefaultValue(String.valueOf(DEBUGGEE_TIMEOUT_BEHAVIOUR));
            DBProcedure dbProcedure =
                    DBProcedure.of("DBMS_DEBUG", "SET_TIMEOUT_BEHAVIOUR", Collections.singletonList(param));
            executeProcedure(dbProcedure);
            // 初始化debug_id
            stmt.executeQuery(PLUtils.getSpecifiedRoute(this.plDebugODPSpecifiedRoute)
                    + "select dbms_debug.initialize() from dual;");
            try (ResultSet resultSet = stmt.getResultSet()) {
                if (resultSet.next()) {
                    debugId = resultSet.getString(1);
                }
            }
            // 打开pl的日志输出
            enableDbmsOutput(stmt);
            // 打开调试开关
            stmt.execute(PLUtils.getSpecifiedRoute(this.plDebugODPSpecifiedRoute) + "call dbms_debug.debug_on();");
        } catch (SQLSyntaxErrorException e) {
            if (Objects.equals(e.getErrorCode(), ERROR_CODE)
                    && StringUtils.contains(e.getMessage(), PL_DEBUGGING_ERROR_CODE)) {
                throw new BadRequestException(ErrorCodes.InsufficientPrivilege, new Object[] {"debug"}, e.getMessage());
            }
            throw e;
        }
        asyncCallPLObject(debugThreadPoolExecutor, req);
    }

    private void asyncCallPLObject(ThreadPoolExecutor debugThreadPoolExecutor, StartPLDebugReq req) {
        PLDebugTask task = new PLDebugTask(req, debugId, this);
        resultFuture = debugThreadPoolExecutor.submit(task);
    }

    public DBMSOutput getOutput() {
        // only after call dbms_debug.debug_off()
        // we can acquire dbms output from debuggee connection
        // get log will be stucked when debug is on
        DBMSOutput dbmsOutput = null;
        try {
            dbmsOutput = DBMSOutput.of(OBUtils.queryDBMSOutput(connection, dbmsoutputMaxRows));
        } catch (SQLException e) {
            log.warn("failed to get pl log, errorMessage={}", e.getMessage());
        }
        log.debug("Dbms fetching finished");
        return dbmsOutput;
    }

    public PLDebugResult getResult() {
        PLDebugResult result = null;
        if (resultFuture.isDone()) {
            try {
                result = resultFuture.get();
            } catch (Exception e) {
                log.warn("Error occurs when fetching pl debug result, debug id={}", debugId, e);
                if (e instanceof ExecutionException) {
                    result = PLDebugResult.of(e.getCause().getLocalizedMessage());
                }
            }
        }
        return result;
    }

    @Override
    public boolean detectSessionAlive() {
        boolean alive = true;
        if (Objects.isNull(resultFuture) || resultFuture.isDone() || resultFuture.isCancelled()) {
            alive = false;
        }
        log.info("Debuggee session_id={} alive={}", this.debugId, alive);
        return alive;
    }

    @Override
    public void close() {
        // ensure dbms_output could be clear before quit
        getOutput();
        if (Objects.nonNull(resultFuture)) {
            resultFuture.cancel(true);
        }
        super.close();
    }

}
