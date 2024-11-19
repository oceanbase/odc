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
package com.oceanbase.odc.service.onlineschemachange.oscfms.action;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.google.common.annotations.VisibleForTesting;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.onlineschemachange.OscTableUtil;
import com.oceanbase.odc.service.onlineschemachange.ddl.DdlUtils;
import com.oceanbase.odc.service.onlineschemachange.fsm.Action;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskResult;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeSqlType;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionContext;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionResult;
import com.oceanbase.odc.service.onlineschemachange.oscfms.state.OscStates;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;

import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2024/7/8 11:23
 * @since 4.3.1
 */
@Slf4j
public class CreateGhostTableAction implements Action<OscActionContext, OscActionResult> {

    @Override
    public OscActionResult execute(OscActionContext context) throws Exception {
        ConnectionSession connectionSession = null;
        Long scheduleTaskId = context.getScheduleTask().getId();
        try {
            connectionSession = context.getConnectionProvider().createConnectionSession();
            ConnectionSessionUtil.setCurrentSchema(connectionSession,
                    context.getTaskParameter().getDatabaseName());
            prepareSchema(context.getParameter(), context.getTaskParameter(),
                    connectionSession, scheduleTaskId, context);

        } catch (Exception e) {
            log.warn("Failed to start osc job with taskId={}.", scheduleTaskId, e);
            throw e;
        } finally {
            if (connectionSession != null) {
                connectionSession.expire();
            }
        }
        return new OscActionResult(OscStates.CREATE_GHOST_TABLES.getState(), null,
                OscStates.CREATE_DATA_TASK.getState());
    }

    @VisibleForTesting
    protected void prepareSchema(OnlineSchemaChangeParameters param, OnlineSchemaChangeScheduleTaskParameters taskParam,
            ConnectionSession session, Long scheduleTaskId, OscActionContext oscContext) throws SQLException {
        // drop is required, cause the following sql may failed, and we do retry from the beginning
        OscTableUtil.dropNewTableIfExits(taskParam.getDatabaseName(), taskParam.getNewTableNameUnwrapped(), session);
        SyncJdbcExecutor executor = session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        String finalTableDdl;
        executor.execute(taskParam.getNewTableCreateDdl());
        if (CollectionUtils.isNotEmpty(taskParam.getSqlsToBeExecuted())) {
            taskParam.getSqlsToBeExecuted().forEach(executor::execute);
        }
        if (param.getSqlType() == OnlineSchemaChangeSqlType.ALTER) {

            // update new table ddl for display
            finalTableDdl = DdlUtils.queryOriginTableCreateDdl(session, taskParam.getNewTableName());

            String ddlForDisplay = DdlUtils.replaceTableName(finalTableDdl, taskParam.getOriginTableName(),
                    session.getDialectType(), OnlineSchemaChangeSqlType.CREATE).getNewSql();
            taskParam.setNewTableCreateDdlForDisplay(ddlForDisplay);
            oscContext.getScheduleTaskRepository().updateTaskResult(scheduleTaskId,
                    JsonUtils.toJson(new OnlineSchemaChangeScheduleTaskResult(taskParam)));
        } else {
            finalTableDdl = DdlUtils.queryOriginTableCreateDdl(session, taskParam.getNewTableName());
        }
        log.info("Successfully created new table, ddl: {}", finalTableDdl);
        validateColumnDifferent(taskParam, session);
        oscContext.getScheduleTaskRepository().updateTaskParameters(scheduleTaskId, JsonUtils.toJson(taskParam));
    }

    private void validateColumnDifferent(OnlineSchemaChangeScheduleTaskParameters taskParam,
            ConnectionSession session) {
        List<String> originTableColumns =
                DBSchemaAccessors.create(session).listTableColumns(taskParam.getDatabaseName(),
                        DdlUtils.getUnwrappedName(taskParam.getOriginTableNameUnwrapped())).stream()
                        .map(DBTableColumn::getName).collect(Collectors.toList());

        List<String> newTableColumns =
                DBSchemaAccessors.create(session).listTableColumns(taskParam.getDatabaseName(),
                        DdlUtils.getUnwrappedName(taskParam.getNewTableNameUnwrapped())).stream()
                        .map(DBTableColumn::getName).collect(Collectors.toList());

        // Check drop column
        List<String> aMinusB = new ArrayList<>(originTableColumns);
        aMinusB.removeAll(new ArrayList<>(newTableColumns));

        if (!aMinusB.isEmpty()) {
            throw new UnsupportedException(ErrorCodes.OscColumnNameInconsistent,
                    null, String.format("Column [%s] is not found in new table.", String.join(",", aMinusB)));
        }

        List<String> bMinusA = new ArrayList<>(newTableColumns);
        bMinusA.removeAll(new ArrayList<>(originTableColumns));

        // Check add primary key column
        List<DBTableConstraint> constraints = DBSchemaAccessors.create(session)
                .listTableConstraints(taskParam.getDatabaseName(), taskParam.getNewTableNameUnwrapped());
        constraints.stream()
                .filter(c -> c.getType() == DBConstraintType.PRIMARY_KEY)
                .forEach(p -> {
                    if (new HashSet<>(bMinusA).containsAll(p.getColumnNames())) {
                        throw new UnsupportedException(ErrorCodes.OscAddPrimaryKeyColumnNotAllowed, null,
                                String.format("Add primary key column [%s] in new table is not allowed.",
                                        String.join(",", p.getColumnNames())));
                    }
                });

    }
}
