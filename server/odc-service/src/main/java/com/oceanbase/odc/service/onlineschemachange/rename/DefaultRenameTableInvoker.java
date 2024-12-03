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
import java.util.function.Supplier;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.db.browser.DBObjectOperators;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OriginTableCleanStrategy;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.ConnectionProvider;
import com.oceanbase.odc.service.session.DBSessionManageFacade;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-08-04
 * @since 4.2.0
 */
@Slf4j
public class DefaultRenameTableInvoker implements RenameTableInvoker {
    private final ConnectionProvider connectionProvider;
    private final DBSessionManageFacade dbSessionManageFacade;
    /**
     * supply if oms project has all data replicated
     */
    private final Supplier<Boolean> dataReplicatedSupplier;
    /**
     * supply if lock table can be supported
     */
    private final Supplier<LockTableSupportDecider> lockTableSupportDeciderSupplier;

    public DefaultRenameTableInvoker(ConnectionProvider connectionProvider,
            DBSessionManageFacade dbSessionManageFacade,
            Supplier<Boolean> dataReplicatedSupplier,
            Supplier<LockTableSupportDecider> lockTableSupportDeciderSupplier) {
        this.connectionProvider = connectionProvider;
        this.dbSessionManageFacade = dbSessionManageFacade;
        this.dataReplicatedSupplier = dataReplicatedSupplier;
        this.lockTableSupportDeciderSupplier = lockTableSupportDeciderSupplier;
    }

    @Override
    public void invoke(OnlineSchemaChangeScheduleTaskParameters taskParameters,
            OnlineSchemaChangeParameters parameters) {
        Integer swapTableNameRetryTimes = parameters.getSwapTableNameRetryTimes();
        if (swapTableNameRetryTimes == 0) {
            swapTableNameRetryTimes = 1;
        }
        AtomicInteger retryTime = new AtomicInteger();
        RenameTableParameters renameTableParameters = getRenameTableParameters(taskParameters, parameters);

        boolean succeed = false;
        do {
            // try whole rename table flow
            succeed = tryRenameTable(connectionProvider, taskParameters, renameTableParameters, retryTime);
        } while (!succeed && retryTime.incrementAndGet() < swapTableNameRetryTimes);

        if (!succeed) {
            throw new IllegalStateException(
                    MessageFormat.format("Swap table name failed after {0} times", retryTime.get()));
        } else {
            log.info("Table has successfully swapped, new schema takes effect now");
        }
    }

    private boolean tryRenameTable(ConnectionProvider connectionProvider,
            OnlineSchemaChangeScheduleTaskParameters taskParameters,
            RenameTableParameters renameTableParameters, AtomicInteger retryTime) {
        // every retry use whole new connection session, in case session not valid for some reason (eg
        // server not valid)
        ConnectionSession connectionSession = null;
        try {
            connectionSession = connectionProvider.createConnectionSession();
            // set session schema env
            ConnectionSessionUtil.setCurrentSchema(connectionSession, taskParameters.getDatabaseName());
            // check if swap table has done
            boolean hasSwapTableDone = hasSwapTableDone(connectionSession, renameTableParameters.getSchemaName(),
                    renameTableParameters.getOriginTableName(), renameTableParameters.getNewTableName());
            // prepare swap table environment
            List<RenameTableInterceptor> interceptors = new LinkedList<>();
            LockRenameTableFactory lockRenameTableFactory = new LockRenameTableFactory();
            RenameTableInterceptor lockInterceptor =
                    lockRenameTableFactory.generate(connectionSession, dbSessionManageFacade,
                            lockTableSupportDeciderSupplier);
            interceptors.add(lockInterceptor);
            interceptors.add(new ForeignKeyInterceptor(connectionSession));
            RenameTableHandler renameTableHandler = RenameTableHandlers.getForeignKeyHandler(connectionSession);
            RenameBackHandler renameBackHandler = new RenameBackHandler(renameTableHandler);
            // do try rename table
            if (!hasSwapTableDone) {
                hasSwapTableDone = doTryRename(connectionSession, taskParameters, renameTableParameters,
                        renameTableHandler, renameBackHandler, interceptors,
                        retryTime);
            }
            // swap table failed
            if (!hasSwapTableDone) {
                // try to rollback rename action if failed
                renameBackHandler.renameBack(connectionSession, taskParameters);
            } else {
                // try drop old table if succeed
                dropOldTable(connectionSession, renameTableParameters, taskParameters);
            }
            return hasSwapTableDone;
        } finally {
            if (null != connectionSession) {
                connectionSession.expire();
            }
        }
    }

    private boolean doTryRename(ConnectionSession connectionSession,
            OnlineSchemaChangeScheduleTaskParameters taskParameters,
            RenameTableParameters renameTableParameters, RenameTableHandler renameTableHandler,
            RenameBackHandler renameBackHandler,
            List<RenameTableInterceptor> interceptors,
            AtomicInteger retryTime) {
        boolean succeed = false;
        try {
            preRename(interceptors, renameTableParameters);
            if (!dataReplicatedSupplier.get()) {
                throw new RuntimeException("increment data not applied to ghost table yet");
            }
            renameTableHandler.rename(taskParameters.getDatabaseName(), taskParameters.getOriginTableName(),
                    taskParameters.getRenamedTableName(), taskParameters.getNewTableName());
            succeed = true;
            renameSucceed(interceptors, renameTableParameters);
        } catch (Exception e) {
            log.warn(MessageFormat.format("Swap table name occur error, retry time {0}",
                    retryTime.get()), e);
            renameBackHandler.renameBack(connectionSession, taskParameters);
            renameFailed(interceptors, renameTableParameters);
        } finally {
            try {
                postRenamed(interceptors, renameTableParameters);
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
                .lockUsers(parameters.getLockUsers())
                .build();
    }

    private void preRename(List<RenameTableInterceptor> interceptors, RenameTableParameters parameters) {
        interceptors.forEach(r -> r.preRename(parameters));
    }

    private void renameSucceed(List<RenameTableInterceptor> interceptors, RenameTableParameters parameters) {
        reverseConsumerInterceptor(interceptors, r -> r.renameSucceed(parameters));
    }

    private void renameFailed(List<RenameTableInterceptor> interceptors, RenameTableParameters parameters) {
        reverseConsumerInterceptor(interceptors, r -> r.renameFailed(parameters));
    }

    private void postRenamed(List<RenameTableInterceptor> interceptors, RenameTableParameters parameters) {
        reverseConsumerInterceptor(interceptors, r -> r.postRenamed(parameters));
    }

    private void reverseConsumerInterceptor(List<RenameTableInterceptor> interceptors,
            Consumer<RenameTableInterceptor> interceptorConsumer) {
        ListIterator<RenameTableInterceptor> listIterator = interceptors.listIterator(interceptors.size());
        while (listIterator.hasPrevious()) {
            interceptorConsumer.accept(listIterator.previous());
        }
    }

    protected void dropOldTable(ConnectionSession connectionSession, RenameTableParameters parameters,
            OnlineSchemaChangeScheduleTaskParameters taskParameters) {
        if (parameters.getOriginTableCleanStrategy() == OriginTableCleanStrategy.ORIGIN_TABLE_DROP) {
            log.info("Because origin table clean strategy is {}, so we drop the old table. ",
                    parameters.getOriginTableCleanStrategy());
            // table has been dropped
            if (!isTableExisted(connectionSession, parameters.getSchemaName(),
                    taskParameters.getRenamedTableNameUnwrapped())) {
                log.info("{}.{} has been dropped, drop table not needed", parameters.getSchemaName(),
                        taskParameters.getRenamedTableNameUnwrapped());
            } else {
                // try real drop table
                DBObjectOperators.create(connectionSession)
                        .drop(DBObjectType.TABLE, parameters.getSchemaName(),
                                taskParameters.getRenamedTableNameUnwrapped());
            }
        }
    }

    /**
     * detect if table existed
     */
    protected boolean isTableExisted(ConnectionSession connectionSession, String schemaName, String tableName) {
        DBSchemaAccessor dbSchemaAccessor = DBSchemaAccessors.create(connectionSession);
        DialectType dialectType = connectionSession.getDialectType();
        List<String> renamedTable = dbSchemaAccessor.showTablesLike(SwapTableUtil.unquoteName(schemaName, dialectType),
                SwapTableUtil.unquoteName(tableName, dialectType));
        return CollectionUtils.isNotEmpty(renamedTable);
    }

    /**
     * if origin table exists and ghost table not exists, then we assume that rename operation has done.
     * the following rename operation should be ignored, cause it will always failed.
     *
     * @param schemaName schema name
     * @param originTable table name
     * @param ghostTable ghost table prepare switch to origin table, eg __osc_gho_origin_table
     * @return true, if has swapped
     */
    protected boolean hasSwapTableDone(ConnectionSession connectionSession, String schemaName, String originTable,
            String ghostTable) {
        DBSchemaAccessor dbSchemaAccessor = DBSchemaAccessors.create(connectionSession);
        DialectType dialectType = connectionSession.getDialectType();
        List<String> originTableList =
                dbSchemaAccessor.showTablesLike(SwapTableUtil.unquoteName(schemaName, dialectType),
                        SwapTableUtil.unquoteName(originTable, dialectType));
        List<String> ghostTableList = dbSchemaAccessor.showTablesLike(
                SwapTableUtil.unquoteName(schemaName, dialectType), SwapTableUtil.unquoteName(ghostTable, dialectType));
        // origin table exist, ghost table not exist
        return CollectionUtils.isNotEmpty(originTableList)
                && !CollectionUtils.isNotEmpty(ghostTableList);
    }
}
