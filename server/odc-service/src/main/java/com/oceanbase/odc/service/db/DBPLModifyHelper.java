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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.ConflictException;
import com.oceanbase.odc.core.sql.execute.model.SqlExecuteStatus;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.model.EditPLReq;
import com.oceanbase.odc.service.db.model.EditPLResp;
import com.oceanbase.odc.service.session.ConnectConsoleService;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.odc.service.session.model.AsyncExecuteResultResp;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteReq;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteResp;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
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
    public static final int LOCK_TIMEOUT_SECONDS = 3;
    @Autowired
    private ConnectConsoleService connectConsoleService;
    @Autowired
    private JdbcLockRegistry jdbcLockRegistry;
    @Autowired
    private ConnectSessionService sessionService;

    public EditPLResp editPL(@NotNull String sessionId, @NotNull @Valid EditPLReq editPLReq)
            throws Exception {
        DBObjectType plType = editPLReq.getObjectType();
        switch (plType) {
            case PROCEDURE:
                return executeWrappedEditPL(sessionId, editPLReq, DBObjectType.PROCEDURE, ODC_TEMPORARY_PROCEDURE);
            case FUNCTION:
                return executeWrappedEditPL(sessionId, editPLReq, DBObjectType.FUNCTION, ODC_TEMPORARY_FUNCTION);
            case TRIGGER:
                if (VersionUtils.isLessThan(
                        ConnectionSessionUtil.getVersion(sessionService.nullSafeGet(sessionId, true)),
                        OB_VERSION_SUPPORT_MULTIPLE_SAME_TRIGGERS)) {
                    throw new BadRequestException(
                            "editing trigger in mysql mode is not supported in ob version less than "
                                    + OB_VERSION_SUPPORT_MULTIPLE_SAME_TRIGGERS);
                }
                return executeWrappedEditPL(sessionId, editPLReq, DBObjectType.TRIGGER, ODC_TEMPORARY_TRIGGER);
            default:
                throw new IllegalArgumentException(
                        String.format("the pl type %s of editing procedure is not supported", plType));
        }
    }

    private EditPLResp executeWrappedEditPL(String sessionId, EditPLReq editPLReq,
            DBObjectType plType, String tempPlName) throws Exception {
        String plName = editPLReq.getObjectName();
        String editPLSql = editPLReq.getSql();
        String tempPLSql = editPLSql.replaceFirst(plName, tempPlName);
        StringBuilder wrappedSqlBuilder = new StringBuilder();
        ConnectionSession connectionSession = sessionService.nullSafeGet(sessionId, true);
        SqlCommentProcessor processor = ConnectionSessionUtil.getSqlCommentProcessor(connectionSession);
        String delimiter = processor.getDelimiter();
        wrappedSqlBuilder.append("DELIMITER $$\n")
                .append(tempPLSql).append(" $$\n")
                .append("drop ").append(plType).append(" if exists ").append(tempPlName).append(" $$\n")
                .append("drop ").append(plType).append(" if exists ").append(plName).append(" $$\n")
                .append(editPLSql).append(" $$\n")
                .append("DELIMITER " + delimiter);
        String wrappedSql = wrappedSqlBuilder.toString();
        SqlAsyncExecuteReq sqlAsyncExecuteReq = new SqlAsyncExecuteReq();
        sqlAsyncExecuteReq.setSql(wrappedSql);
        sqlAsyncExecuteReq.setSplit(true);
        sqlAsyncExecuteReq.setContinueExecutionOnError(false);

        Lock editPLLock = obtainEditPLLock(connectionSession, plType);
        if (!editPLLock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new ConflictException(ErrorCodes.ResourceModifying, "Can not acquire jdbc lock");
        }
        try {
            SqlAsyncExecuteResp sqlAsyncExecuteResp = connectConsoleService.streamExecute(sessionId, sqlAsyncExecuteReq,
                    true);
            EditPLResp editPLResp = new EditPLResp();
            editPLResp.setWrappedSql(wrappedSql);
            editPLResp.setSqls(sqlAsyncExecuteResp.getSqls());
            editPLResp.setUnauthorizedDBResources(sqlAsyncExecuteResp.getUnauthorizedDBResources());
            editPLResp.setViolatedRules(sqlAsyncExecuteResp.getViolatedRules());
            editPLResp.setApprovalRequired(sqlAsyncExecuteResp.isApprovalRequired());
            if (editPLResp.isApprovalRequired()) {
                return editPLResp;
            }
            AsyncExecuteResultResp moreResults;
            List<SqlExecuteResult> results = new ArrayList<>();
            do {
                moreResults = connectConsoleService.getMoreResults(sessionId,
                        sqlAsyncExecuteResp.getRequestId());
                results.addAll(moreResults.getResults());
            } while (!moreResults.isFinished());
            for (SqlExecuteResult result : results) {
                if (result.getStatus() != SqlExecuteStatus.SUCCESS) {
                    editPLResp.setErrorMessage(result.getTrack());
                    return editPLResp;
                }
            }
            return editPLResp;
        } finally {
            editPLLock.unlock();
        }
    }

    private Lock obtainEditPLLock(@NotNull ConnectionSession connectionSession, @NonNull DBObjectType plType) {
        ConnectionConfig connConfig =
                (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(connectionSession);
        Long dataSourceId = connConfig.getId();
        return jdbcLockRegistry.obtain(String.format("%s-%d", plType, dataSourceId));
    }
}
