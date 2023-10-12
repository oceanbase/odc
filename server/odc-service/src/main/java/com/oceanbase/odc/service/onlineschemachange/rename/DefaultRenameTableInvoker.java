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
package com.oceanbase.odc.service.onlineschemachange.rename;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.service.db.browser.DBObjectOperators;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OriginTableCleanStrategy;
import com.oceanbase.odc.service.session.DBSessionManageFacade;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-08-04
 * @since 4.2.0
 */
@Slf4j
public class DefaultRenameTableInvoker implements RenameTableInvoker {

    private final List<RenameTableInterceptor> interceptors;

    private final RenameTableHandler renameTableHandler;
    private final ConnectionSession connectionSession;
    private final RenameBackHandler renameBackHandler;

    public DefaultRenameTableInvoker(ConnectionSession connSession,
            DBSessionManageFacade dbSessionManageFacade) {
        List<RenameTableInterceptor> interceptors = new LinkedList<>();

        LockRenameTableFactory lockRenameTableFactory = new LockRenameTableFactory();
        RenameTableInterceptor lockInterceptor =
                lockRenameTableFactory.generate(connSession, dbSessionManageFacade);
        interceptors.add(lockInterceptor);
        interceptors.add(new ForeignKeyInterceptor(connSession));
        this.interceptors = interceptors;
        this.connectionSession = connSession;
        this.renameTableHandler = RenameTableHandlers.getForeignKeyHandler(connSession);
        this.renameBackHandler = new RenameBackHandler(renameTableHandler);
    }

    @Override
    public void invoke(OnlineSchemaChangeScheduleTaskParameters taskParameters,
            OnlineSchemaChangeParameters parameters) {
        RenameTableParameters renameTableParameters = getRenameTableParameters(taskParameters, parameters);
        try {
            retryRename(taskParameters, parameters, renameTableParameters);
            dropOldTable(renameTableParameters, taskParameters);
        } finally {
            renameBackHandler.renameBack(connectionSession, taskParameters);
        }
    }

    private void retryRename(OnlineSchemaChangeScheduleTaskParameters taskParameters,
            OnlineSchemaChangeParameters parameters, RenameTableParameters renameTableParameters) {

        Integer swapTableNameRetryTimes = parameters.getSwapTableNameRetryTimes();
        if (swapTableNameRetryTimes == 0) {
            swapTableNameRetryTimes = 1;
        }

        AtomicInteger retryTime = new AtomicInteger();
        boolean succeed;
        do {
            succeed = doTryRename(taskParameters, renameTableParameters, retryTime);
        } while (!succeed && retryTime.incrementAndGet() < swapTableNameRetryTimes);

        if (!succeed) {
            throw new IllegalStateException(
                    MessageFormat.format("Swap table name failed after {0} times", retryTime.get()));
        } else {
            log.info("Table has successfully swapped, new schema takes effect now");
        }
    }

    private boolean doTryRename(OnlineSchemaChangeScheduleTaskParameters taskParameters,
            RenameTableParameters renameTableParameters, AtomicInteger retryTime) {
        boolean succeed = false;
        try {
            preRename(renameTableParameters);
            renameTableHandler.rename(taskParameters.getDatabaseName(), taskParameters.getOriginTableName(),
                    taskParameters.getRenamedTableName(), taskParameters.getNewTableName());
            succeed = true;
            renameSucceed(renameTableParameters);
        } catch (Exception e) {
            log.warn(MessageFormat.format("Swap table name occur error, retry time {0}",
                    retryTime.get()), e);
            renameBackHandler.renameBack(connectionSession, taskParameters);
            renameFailed(renameTableParameters);
        } finally {
            try {
                postRenamed(renameTableParameters);
            } catch (Throwable throwable) {
                // ignore
            }
        }
        return succeed;
    }


    private RenameTableParameters getRenameTableParameters(OnlineSchemaChangeScheduleTaskParameters taskParameters,
            OnlineSchemaChangeParameters parameters) {
        // set lock table max timeout is 120s
        Integer lockTableTimeOutSeconds =
                Math.min(parameters.getLockTableTimeOutSeconds() == null ? 10 : parameters.getLockTableTimeOutSeconds(),
                        60);

        return RenameTableParameters.builder()
                .schemaName(taskParameters.getDatabaseName())
                .renamedTableName(taskParameters.getRenamedTableName())
                .originTableName(taskParameters.getOriginTableName())
                .newTableName(taskParameters.getNewTableName())
                .originTableCleanStrategy(parameters.getOriginTableCleanStrategy())
                .lockTableTimeOutSeconds(lockTableTimeOutSeconds)
                .build();
    }

    private void preRename(RenameTableParameters parameters) {
        interceptors.forEach(r -> r.preRename(parameters));
    }

    private void renameSucceed(RenameTableParameters parameters) {
        reverseConsumerInterceptor(r -> r.renameSucceed(parameters));
    }

    private void renameFailed(RenameTableParameters parameters) {
        reverseConsumerInterceptor(r -> r.renameFailed(parameters));
    }

    private void postRenamed(RenameTableParameters parameters) {
        reverseConsumerInterceptor(r -> r.postRenamed(parameters));
    }

    private void reverseConsumerInterceptor(Consumer<RenameTableInterceptor> interceptorConsumer) {
        ListIterator<RenameTableInterceptor> listIterator = interceptors.listIterator(interceptors.size());
        while (listIterator.hasPrevious()) {
            interceptorConsumer.accept(listIterator.previous());
        }
    }

    private void dropOldTable(RenameTableParameters parameters,
            OnlineSchemaChangeScheduleTaskParameters taskParameters) {
        if (parameters.getOriginTableCleanStrategy() == OriginTableCleanStrategy.ORIGIN_TABLE_DROP) {
            log.info("Because origin table clean strategy is {}, so we drop the old table. ",
                    parameters.getOriginTableCleanStrategy());
            DBObjectOperators.create(connectionSession)
                    .drop(DBObjectType.TABLE, parameters.getSchemaName(),
                            taskParameters.getRenamedTableNameUnwrapped());
        }
    }
}
