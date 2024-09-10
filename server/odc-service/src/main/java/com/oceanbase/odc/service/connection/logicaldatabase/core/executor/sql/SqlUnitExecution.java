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
package com.oceanbase.odc.service.connection.logicaldatabase.core.executor.sql;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.jdbc.core.StatementCallback;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.sql.execute.model.JdbcGeneralResult;
import com.oceanbase.odc.core.sql.execute.model.SqlExecuteStatus;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionGroupContext;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionResult;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionStatus;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.UnitExecution;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.util.ConnectionInfoUtil;
import com.oceanbase.odc.service.session.OdcStatementCallBack;
import com.oceanbase.odc.service.session.factory.DruidDataSourceFactory;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/8/28 21:04
 * @Description: []
 */
@Slf4j
public class SqlUnitExecution implements UnitExecution<SqlExecuteReq, SqlExecutionResultWrapper> {
    private ConnectionSession connectionSession;
    private OdcStatementCallBack statementCallBack;
    private long timeoutMillis;
    private SqlExecuteReq req;

    public SqlUnitExecution(@NonNull ConnectionSession connectionSession, SqlExecuteReq req) {
        this.connectionSession = connectionSession;
        this.statementCallBack =
                new OdcStatementCallBack(Arrays.asList(SqlTuple.newTuple(req.getSql())), connectionSession, true, 1000);
        this.timeoutMillis = req.getTimeoutMillis();
        this.req = req;
    }

    @Override
    public ExecutionResult<SqlExecutionResultWrapper> beforeExecute(
            ExecutionGroupContext<SqlExecuteReq, SqlExecutionResultWrapper> context) {
        SqlExecutionResultWrapper resultWrapper =
                new SqlExecutionResultWrapper(req.getLogicalDatabaseId(), req.getPhysicalDatabaseId(),
                        req.getScheduleTaskId(), null);
        resultWrapper.setExecuteSql(req.getSql());
        resultWrapper.setStatus(SqlExecuteStatus.CREATED);
        return new ExecutionResult<>(resultWrapper, ExecutionStatus.PENDING, req.getOrder());
    }

    @Override
    public ExecutionResult<SqlExecutionResultWrapper> execute(
            ExecutionGroupContext<SqlExecuteReq, SqlExecutionResultWrapper> context)
            throws SQLException {
        List<JdbcGeneralResult> results = connectionSession
                .getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY)
                .execute((StatementCallback<List<JdbcGeneralResult>>) stmt -> {
                    stmt.setQueryTimeout(
                            (int) TimeUnit.MILLISECONDS.toSeconds(timeoutMillis));
                    return statementCallBack.doInStatement(stmt);
                });
        JdbcGeneralResult result = results.get(0);
        log.info("SqlExecutionCallback execute result, connectionReset={}", result.isConnectionReset());
        return new ExecutionResult<>(
                new SqlExecutionResultWrapper(req.getLogicalDatabaseId(), req.getPhysicalDatabaseId(),
                        req.getScheduleTaskId(), new SqlExecuteResult(result)),
                getExecutionStatus(result.getStatus()), req.getOrder());
    }

    @Override
    public void terminate(ExecutionGroupContext<SqlExecuteReq, SqlExecutionResultWrapper> context)
            throws Exception {
        String connectionId = ConnectionSessionUtil.getConsoleConnectionId(connectionSession);
        Verify.notNull(connectionId, "ConnectionId");
        ConnectionConfig conn = (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(connectionSession);
        Verify.notNull(conn, "ConnectionConfig");
        DruidDataSourceFactory factory = new DruidDataSourceFactory(conn);
        try {
            ConnectionInfoUtil.killQuery(connectionId, factory, connectionSession.getDialectType());
        } catch (Exception e) {
            if (connectionSession.getDialectType().isOceanbase()) {
                ConnectionSessionUtil.killQueryByDirectConnect(connectionId, factory);
                log.info("Kill query by direct connect succeed, connectionId={}", connectionId);
            } else {
                log.warn("Kill query occur error, connectionId={}", connectionId, e);
            }
        }
    }

    private ExecutionStatus getExecutionStatus(@NonNull SqlExecuteStatus sqlStatus) {
        ExecutionStatus executionStatus = null;
        switch (sqlStatus) {
            case SUCCESS:
                executionStatus = ExecutionStatus.SUCCESS;
                break;
            case FAILED:
            case CANCELED:
                executionStatus = ExecutionStatus.FAILED;
                break;
            case RUNNING:
                executionStatus = ExecutionStatus.RUNNING;
                break;
            case CREATED:
                executionStatus = ExecutionStatus.PENDING;
                break;
        }
        return executionStatus;
    }
}
