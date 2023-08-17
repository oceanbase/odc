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
package com.oceanbase.odc.service.onlineschemachange.pipeline;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.rename.DefaultRenameTableInvoker;
import com.oceanbase.odc.service.onlineschemachange.rename.RenameTableHandler;
import com.oceanbase.odc.service.onlineschemachange.rename.RenameTableHandlers;
import com.oceanbase.odc.service.onlineschemachange.rename.RenameTableParameters;
import com.oceanbase.odc.service.session.DBSessionManageFacade;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-06-11
 * @since 4.2.0
 */
@Slf4j
@Component
public class SwapTableNameValve extends BaseValve {
    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private DBSessionManageFacade dbSessionManageFacade;

    @Override
    public void invoke(ValveContext valveContext) {
        OscValveContext context = (OscValveContext) valveContext;
        ScheduleTaskEntity scheduleTask = context.getScheduleTask();
        log.info("Start execute {}, schedule task id {}", getClass().getSimpleName(), scheduleTask.getId());

        OnlineSchemaChangeScheduleTaskParameters taskParameters = context.getTaskParameter();
        PreConditions.notNull(taskParameters, "OnlineSchemaChangeScheduleTaskParameters is null");

        OnlineSchemaChangeParameters parameters = context.getParameter();

        ConnectionConfig config = context.getConnectionConfig();
        RenameTableHandler renameTableExecutor = null;
        ConnectionSession connectionSession = new DefaultConnectSessionFactory(config).generateSession();
        ConnectionSessionUtil.setCurrentSchema(connectionSession, taskParameters.getDatabaseName());

        try {
            renameTableExecutor = RenameTableHandlers.getForeignKeyHandler(connectionSession);

            swapTable(taskParameters, parameters, renameTableExecutor, connectionSession);

            context.setSwapSucceedCallBack(true);
        } finally {
            cleanUp(connectionSession, taskParameters, renameTableExecutor);
        }
    }

    private void swapTable(OnlineSchemaChangeScheduleTaskParameters taskParameters,
            OnlineSchemaChangeParameters parameters, RenameTableHandler renameTableHandler,
            ConnectionSession connSession) {
        DefaultRenameTableInvoker renameTableInvoker = new DefaultRenameTableInvoker(connSession,
                dbSessionManageFacade);
        renameTableInvoker.invoke(
                () -> {
                    retryRename(taskParameters, parameters, renameTableHandler, connSession);
                    return null;
                },
                getRenameTableParameters(taskParameters, parameters));
    }

    private RenameTableParameters getRenameTableParameters(OnlineSchemaChangeScheduleTaskParameters taskParameters,
            OnlineSchemaChangeParameters parameters) {
        // set lock table max timeout is 120s
        Integer lockTableTimeOutSeconds =
                Math.min(parameters.getLockTableTimeOutSeconds() == null ? 15 : parameters.getLockTableTimeOutSeconds(),
                        120);

        return RenameTableParameters.builder()
                .schemaName(taskParameters.getDatabaseName())
                .renamedTableName(taskParameters.getRenamedTableName())
                .originTableName(taskParameters.getOriginTableName())
                .newTableName(taskParameters.getNewTableName())
                .lockTableTimeOutSeconds(lockTableTimeOutSeconds)
                .build();

    }

    private void retryRename(OnlineSchemaChangeScheduleTaskParameters taskParameters,
            OnlineSchemaChangeParameters parameters, RenameTableHandler renameTableHandler,
            ConnectionSession connectionSession) {

        Integer swapTableNameRetryTimes = parameters.getSwapTableNameRetryTimes();
        if (swapTableNameRetryTimes == 0) {
            swapTableNameRetryTimes = 1;
        }

        AtomicInteger retryTime = new AtomicInteger();
        boolean succeed = false;

        while (retryTime.getAndIncrement() < swapTableNameRetryTimes) {
            try {
                renameTableHandler.rename(taskParameters.getDatabaseName(), taskParameters.getOriginTableName(),
                        taskParameters.getRenamedTableName(), taskParameters.getNewTableName());
                succeed = true;
                break;
            } catch (Exception e) {
                log.warn(MessageFormat.format("Swap table name occur error, retry time {0}",
                        retryTime.get()), e);
                renameBack(connectionSession, taskParameters, renameTableHandler);
            }
        }
        if (!succeed) {
            throw new IllegalStateException(
                    MessageFormat.format("Swap table name failed after {0} times", retryTime.get()));
        } else {
            log.info("Table has successfully swapped, new schema takes effect now");
        }
    }

    private void cleanUp(ConnectionSession connectionSession,
            OnlineSchemaChangeScheduleTaskParameters taskParameters,
            RenameTableHandler renameTableExecutor) {
        renameBack(connectionSession, taskParameters, renameTableExecutor);
        connectionSession.expire();
    }

    private void renameBack(ConnectionSession connectionSession,
            OnlineSchemaChangeScheduleTaskParameters taskParameters, RenameTableHandler renameTableExecutor) {

        /*
         * If the original table was successfully renamed to _old but the second rename operation failed,
         * rollback the first renaming
         */
        try {
            DBSchemaAccessor dbSchemaAccessor = DBSchemaAccessors.create(connectionSession);
            List<String> renamedTable = dbSchemaAccessor.showTablesLike(taskParameters.getDatabaseName(),
                    taskParameters.getRenamedTableName());

            List<String> originTable = dbSchemaAccessor.showTablesLike(taskParameters.getDatabaseName(),
                    taskParameters.getOriginTableNameUnWrapped());

            if (!CollectionUtils.isEmpty(renamedTable) && CollectionUtils.isEmpty(originTable)) {

                renameTableExecutor.rename(taskParameters.getDatabaseName(),
                        taskParameters.getRenamedTableName(), taskParameters.getOriginTableName());
                log.info("Because previous swap occur error, so we rename back, renamed {} TO {}",
                        taskParameters.getRenamedTableNameWithSchema(), taskParameters.getOriginTableNameWithSchema());
            }
        } catch (Exception exception) {
            log.warn("Rename back occur error", exception);
        }
    }

}


