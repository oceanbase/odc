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
package com.oceanbase.odc.service.db;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.core.sql.split.OffsetString;
import com.oceanbase.odc.service.session.ConnectConsoleService;
import com.oceanbase.odc.service.session.OdcStatementCallBack;
import com.oceanbase.odc.service.session.model.AsyncExecuteContext;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteReq;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.NonNull;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2024/10/17 18:59
 * @since: 4.3.3
 */
@Component
public class DBPLModifyHelper {
    public static final String ODC_TEMPORARY_PROCEDURE = "_ODC_TEMPORARY_PROCEDURE";
    public static final String ODC_TEMPORARY_TRIGGER = "_ODC_TEMPORARY_TRIGGER";
    public static final String ODC_TEMPORARY_FUNCTION = "_ODC_TEMPORARY_FUNCTION";
    public static final String OB_VERSION_SUPPORT_MULTIPLE_SAME_TRIGGERS = "4.2";
    @Autowired
    @Lazy
    private ConnectConsoleService connectConsoleService;
    @Autowired
    private JdbcLockRegistry jdbcLockRegistry;

    public OdcStatementCallBack generateEditPLSqlODCStatementCallBackForOBMysql(@NotEmpty List<SqlTuple> sqlTuples,
            @NotNull ConnectionSession connectionSession, @NotNull SqlAsyncExecuteReq request, Integer queryLimit,
            boolean stopOnError, AsyncExecuteContext context)
            throws Exception {
        sqlTuples = getWrappedSqlTuplesForOBMysql(sqlTuples, connectionSession, request);
        OdcStatementCallBack statementCallBack = new OdcStatementCallBack(sqlTuples, connectionSession,
                request.getAutoCommit(), queryLimit, stopOnError, context);
        statementCallBack.setDBPLModifyHelper(this);
        return statementCallBack;
    }

    private List<SqlTuple> getWrappedSqlTuplesForOBMysql(@NotEmpty List<SqlTuple> sqlTuples,
            @NotNull ConnectionSession connectionSession,
            SqlAsyncExecuteReq request) throws Exception {
        if (sqlTuples.size() != 1) {
            throw new IllegalArgumentException("the sql for editing procedure must generate single sql tuple");
        }
        SqlTuple sqlTuple = sqlTuples.get(0);
        if (sqlTuple == null) {
            throw new IllegalArgumentException("the sql of editing procedure is null");
        }
        String plName = request.getPlName();
        DBObjectType plType = request.getPlType();
        sqlTuple.setPlName(plName);
        sqlTuple.setPlType(plType);
        switch (plType) {
            case PROCEDURE:
                return getWrappedSqlTuplesByPLType(sqlTuples, connectionSession, request, plName, plType,
                        ODC_TEMPORARY_PROCEDURE);
            case FUNCTION:
                return getWrappedSqlTuplesByPLType(sqlTuples, connectionSession, request, plName, plType,
                        ODC_TEMPORARY_FUNCTION);
            case TRIGGER:
                if (VersionUtils.isLessThan(ConnectionSessionUtil.getVersion(connectionSession),
                        OB_VERSION_SUPPORT_MULTIPLE_SAME_TRIGGERS)) {
                    throw new BadRequestException(
                            "editing trigger in mysql mode is not supported in ob version less than "
                                    + OB_VERSION_SUPPORT_MULTIPLE_SAME_TRIGGERS);
                }
                return getWrappedSqlTuplesByPLType(sqlTuples, connectionSession, request, plName, plType,
                        ODC_TEMPORARY_TRIGGER);
            default:
                throw new IllegalArgumentException("the pl type of editing procedure is not supported");
        }
    }

    private List<SqlTuple> getWrappedSqlTuplesByPLType(List<SqlTuple> sqlTuples, ConnectionSession connectionSession,
            SqlAsyncExecuteReq request, String plName, DBObjectType plType, String tempPL) {
        String tempSql = sqlTuples.get(0).getExecutedSql().replaceFirst(plName, tempPL);
        List<SqlTuple> tempSqlTuples = connectConsoleService.generateSqlTuple(
                Collections.singletonList(new OffsetString(0, tempSql)), connectionSession, request);
        String dropTempSql = "DROP " + plType + " IF EXISTS " + tempPL;
        List<SqlTuple> dropTempSqlTuples = connectConsoleService.generateSqlTuple(
                Collections.singletonList(new OffsetString(0, dropTempSql)), connectionSession, request);
        String dropSql = "DROP " + plType + " IF EXISTS " + plName;
        List<SqlTuple> dropSqlTuples = connectConsoleService.generateSqlTuple(
                Collections.singletonList(new OffsetString(0, dropSql)), connectionSession, request);
        tempSqlTuples.addAll(dropTempSqlTuples);
        tempSqlTuples.addAll(dropSqlTuples);
        tempSqlTuples.addAll(sqlTuples);
        sqlTuples = tempSqlTuples;
        return sqlTuples;
    }

    public Lock getEditPLLock(@NotNull Long dataSourceId, @NonNull DBObjectType plType) {
        String lockKeyString;
        switch (plType) {
            case PROCEDURE:
            case FUNCTION:
            case TRIGGER:
                lockKeyString = String.format("%s-%d", plType, dataSourceId);
                break;
            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported pl type %s for dataSourceId %d", plType, dataSourceId));
        }
        return jdbcLockRegistry.obtain(lockKeyString);
    }

}
