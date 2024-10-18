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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.LimitMetric;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.ConflictException;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.core.sql.split.OffsetString;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.model.EditPLReq;
import com.oceanbase.odc.service.db.model.EditPLResp;
import com.oceanbase.odc.service.session.ConnectConsoleService;
import com.oceanbase.odc.service.session.SessionProperties;
import com.oceanbase.odc.service.session.interceptor.SqlCheckInterceptor;
import com.oceanbase.odc.service.session.interceptor.SqlConsoleInterceptor;
import com.oceanbase.odc.service.session.interceptor.SqlExecuteInterceptorService;
import com.oceanbase.odc.service.session.model.AsyncExecuteContext;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteReq;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteResp;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.NonNull;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2024/10/17 18:59
 * @since: 4.3.3
 */
@Validated
@Component
@SkipAuthorize("permission check inside")
public class DBPLModifyHelper {
    public static final String ODC_TEMPORARY_PROCEDURE = "_ODC_TEMPORARY_PROCEDURE";
    public static final String ODC_TEMPORARY_TRIGGER = "_ODC_TEMPORARY_TRIGGER";
    public static final String ODC_TEMPORARY_FUNCTION = "_ODC_TEMPORARY_FUNCTION";
    public static final String OB_VERSION_SUPPORT_MULTIPLE_SAME_TRIGGERS = "4.2";
    public static final int OBTAIN_LOCK_TIME = 3;
    @Autowired
    @Lazy
    private ConnectConsoleService connectConsoleService;
    @Autowired
    private JdbcLockRegistry jdbcLockRegistry;
    @Autowired
    private SessionProperties sessionProperties;
    @Autowired
    private SqlExecuteInterceptorService sqlInterceptService;

    public EditPLResp editPL(@NotNull ConnectionSession connectionSession, @NotNull @Valid EditPLReq editPLReq,
            boolean needSqlRuleCheck)
            throws InterruptedException {
        long maxSqlLength = sessionProperties.getMaxSqlLength();
        if (maxSqlLength > 0) {
            PreConditions.lessThanOrEqualTo("sqlLength", LimitMetric.SQL_LENGTH,
                    StringUtils.length(editPLReq.getSql()), maxSqlLength);
        }
        EditPLResp editPLResp = adaptSqlIntercept(editPLReq, connectionSession, needSqlRuleCheck);
        if (editPLResp.isShouldIntercepted()) {
            return editPLResp;
        }
        DBObjectType plType = editPLReq.getPlType();
        switch (plType) {
            case PROCEDURE:
                return executeWrappedEditPL(connectionSession, editPLReq, editPLResp, DBObjectType.PROCEDURE);
            case TRIGGER:
                return executeWrappedEditPL(connectionSession, editPLReq, editPLResp, DBObjectType.TRIGGER);
            case FUNCTION:
                if (VersionUtils.isLessThan(ConnectionSessionUtil.getVersion(connectionSession),
                        OB_VERSION_SUPPORT_MULTIPLE_SAME_TRIGGERS)) {
                    throw new BadRequestException(
                            "editing trigger in mysql mode is not supported in ob version less than "
                                    + OB_VERSION_SUPPORT_MULTIPLE_SAME_TRIGGERS);
                }
                return executeWrappedEditPL(connectionSession, editPLReq, editPLResp, DBObjectType.FUNCTION);
            default:
                throw new IllegalArgumentException(
                        String.format("the pl type %s of editing procedure is not supported", plType));
        }
    }

    private EditPLResp executeWrappedEditPL(ConnectionSession connectionSession, EditPLReq editPLReq,
            EditPLResp editPLResp,
            DBObjectType plType) throws InterruptedException {
        String plName = editPLReq.getPlName();
        String editPLSql = editPLReq.getSql();
        String tempPLSql = editPLSql.replaceFirst(plName, ODC_TEMPORARY_PROCEDURE);
        SyncJdbcExecutor syncJdbcExecutor = connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY);
        Lock editPLLock = obtainEditPLLock(connectionSession, plType);
        if (!editPLLock.tryLock(OBTAIN_LOCK_TIME, TimeUnit.SECONDS)) {
            throw new ConflictException(ErrorCodes.ResourceModifying, "Can not acquire jdbc lock");
        }
        try {
            syncJdbcExecutor.execute(tempPLSql);
            String dropTempPLSql = "drop " + plType + " if exists " + ODC_TEMPORARY_PROCEDURE;
            syncJdbcExecutor.execute(dropTempPLSql);
            String dropPLSql = "drop " + plType + " if exists " + plName;
            syncJdbcExecutor.execute(dropPLSql);
            syncJdbcExecutor.execute(editPLSql);
            return editPLResp;
        } finally {
            editPLLock.unlock();
        }
    }

    private Lock obtainEditPLLock(@NotNull ConnectionSession connectionSession, @NonNull DBObjectType plType) {
        ConnectionConfig connConfig =
                (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(connectionSession);
        Long dataSourceId = connConfig.getId();
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

    private EditPLResp adaptSqlIntercept(@NotNull EditPLReq editPLReq, @NotNull ConnectionSession connectionSession,
            boolean needSqlRuleCheck) {
        SqlAsyncExecuteReq sqlAsyncExecuteReq = new SqlAsyncExecuteReq();
        sqlAsyncExecuteReq.setSql(editPLReq.getSql());
        sqlAsyncExecuteReq.setSplit(false);
        List<OffsetString> sqls = sqlAsyncExecuteReq.ifSplitSqls()
                ? SqlUtils.splitWithOffset(connectionSession, sqlAsyncExecuteReq.getSql(),
                        sessionProperties.isOracleRemoveCommentPrefix())
                : Collections.singletonList(new OffsetString(0, sqlAsyncExecuteReq.getSql()));
        List<SqlTuple> sqlTuples = connectConsoleService.generateSqlTuple(sqls, connectionSession, sqlAsyncExecuteReq);
        SqlAsyncExecuteResp response = SqlAsyncExecuteResp.newSqlAsyncExecuteResp(sqlTuples);
        Map<String, Object> context = new HashMap<>();
        context.put(SqlCheckInterceptor.NEED_SQL_CHECK_KEY, needSqlRuleCheck);
        context.put(SqlConsoleInterceptor.NEED_SQL_CONSOLE_CHECK, needSqlRuleCheck);
        AsyncExecuteContext executeContext = new AsyncExecuteContext(sqlTuples, context);
        EditPLResp editPLResp = new EditPLResp();
        try {
            boolean shouldIntercepted =
                    !sqlInterceptService.preHandle(sqlAsyncExecuteReq, response, connectionSession, executeContext);
            editPLResp.setViolatedRules(response.getViolatedRules());
            editPLResp.setSqls(response.getSqls());
            editPLResp.setUnauthorizedDBResources(response.getUnauthorizedDBResources());
            if (shouldIntercepted) {
                editPLResp.setShouldIntercepted(true);
            }
        } catch (Exception e) {
            // eat exception
        }
        return editPLResp;
    }
}
