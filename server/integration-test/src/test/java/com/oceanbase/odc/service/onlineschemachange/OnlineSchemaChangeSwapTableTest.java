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
package com.oceanbase.odc.service.onlineschemachange;

import static org.mockito.Mockito.doThrow;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeSqlType;
import com.oceanbase.odc.service.onlineschemachange.model.OriginTableCleanStrategy;
import com.oceanbase.odc.service.onlineschemachange.pipeline.OscValveContext;
import com.oceanbase.odc.service.onlineschemachange.pipeline.SwapTableNameValve;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-06-12
 * @since 4.2.0
 */
@Slf4j
public class OnlineSchemaChangeSwapTableTest extends OscTestEnv {

    @Autowired
    private SwapTableNameValve swapTableNameValve;

    @Test
    public void test_osc_swap_table_origin_table_reserved_successful() {
        String originTableName = getOriginTableName();
        executeOscSwapTable(
                originTableName,
                c -> c.setOriginTableCleanStrategy(OriginTableCleanStrategy.ORIGIN_TABLE_RENAME_AND_RESERVED),
                this::checkSwapTableAndRenameReserved);
    }

    @Test
    public void test_osc_swap_table_origin_table_drop_successful() {
        String originTableName = getOriginTableName();
        executeOscSwapTable(
                originTableName,
                c -> c.setOriginTableCleanStrategy(OriginTableCleanStrategy.ORIGIN_TABLE_DROP),
                this::checkSwapTableAndRenameDrop);
    }

    @Test
    public void test_osc_swap_table_failed_drop_new_table() {

        String originTableName = getOriginTableName();
        doThrow(new BadArgumentException(ErrorCodes.BadArgument, "bad argument"))
                .when(dbSessionManager)
                .killAllSessions(Mockito.any(ConnectionSession.class), Mockito.any(Predicate.class), Mockito.anyInt());

        executeOscSwapTable(
                originTableName,
                c -> c.setOriginTableCleanStrategy(OriginTableCleanStrategy.ORIGIN_TABLE_DROP),
                this::checkSwapTableFailedAndDropNewTable);
    }

    private void executeOscSwapTable(String originTableName,
            Consumer<OnlineSchemaChangeParameters> changeParametersConsumer,
            Consumer<OnlineSchemaChangeScheduleTaskParameters> resultAssert) {
        createTableForTask(originTableName);
        try {
            OnlineSchemaChangeParameters changeParameters = getOnlineSchemaChangeParameters(originTableName);
            changeParametersConsumer.accept(changeParameters);

            ScheduleEntity scheduleEntity = getScheduleEntity(config, changeParameters);
            List<OnlineSchemaChangeScheduleTaskParameters> subTaskParameters =
                    changeParameters.generateSubTaskParameters(config, config.defaultSchema());

            Assert.assertEquals(1, subTaskParameters.size());
            OnlineSchemaChangeScheduleTaskParameters taskParameters = subTaskParameters.get(0);

            ScheduleTaskEntity scheduleTaskEntity = getScheduleTaskEntity(scheduleEntity.getId(), taskParameters);
            // create new Table
            jdbcTemplate.execute(taskParameters.getNewTableCreateDdl());

            OscValveContext context = new OscValveContext();
            context.setSchedule(scheduleEntity);
            context.setScheduleTask(scheduleTaskEntity);
            context.setTaskParameter(taskParameters);
            context.setParameter(changeParameters);
            context.setConnectionConfig(config);
            swapTableNameValve.invoke(context);

            resultAssert.accept(taskParameters);
        } finally {
            dropTableForTask(originTableName);
        }
    }

    private String getOriginTableName() {
        return "`" + StringUtils.uuidNoHyphen() + "`";
    }

    private OnlineSchemaChangeParameters getOnlineSchemaChangeParameters(String originTableName) {
        OnlineSchemaChangeParameters changeParameters = new OnlineSchemaChangeParameters();
        changeParameters.setSwapTableNameRetryTimes(3);
        changeParameters.setSqlType(OnlineSchemaChangeSqlType.CREATE);
        changeParameters.setErrorStrategy(TaskErrorStrategy.ABORT);
        changeParameters.setOriginTableCleanStrategy(OriginTableCleanStrategy.ORIGIN_TABLE_DROP);
        String newTableTemplate = "create table if not exists {0} (id int(20) primary key, name1 varchar(30))";
        String newTableNameDdl = MessageFormat.format(newTableTemplate, originTableName);
        changeParameters.setSqlContent(newTableNameDdl);
        return changeParameters;
    }

    private void checkSwapTableAndRenameReserved(OnlineSchemaChangeScheduleTaskParameters taskParameters) {
        DBSchemaAccessor dbSchemaAccessor = DBSchemaAccessors.create(connectionSession);
        List<String> renamedTable = dbSchemaAccessor.showTablesLike(taskParameters.getDatabaseName(),
                taskParameters.getRenamedTableName());

        List<String> originTable = dbSchemaAccessor.showTablesLike(taskParameters.getDatabaseName(),
                taskParameters.getOriginTableNameUnWrapped());

        Assert.assertFalse(CollectionUtils.isEmpty(renamedTable));
        Assert.assertFalse(CollectionUtils.isEmpty(originTable));

        // if swap table successful
        List<DBTableColumn> tableColumnFromNew = dbSchemaAccessor.listTableColumns(taskParameters.getDatabaseName(),
                taskParameters.getOriginTableNameUnWrapped());

        Optional<DBTableColumn> name1Col = tableColumnFromNew.stream()
                .filter(a -> a.getName().equalsIgnoreCase("name1"))
                .findFirst();
        Assert.assertTrue(name1Col.isPresent());
        Assert.assertEquals(30L, name1Col.get().getMaxLength().longValue());


        List<DBTableColumn> renamedTableColumns = dbSchemaAccessor.listTableColumns(taskParameters.getDatabaseName(),
                taskParameters.getRenamedTableName());

        Optional<DBTableColumn> name2Col = renamedTableColumns.stream()
                .filter(a -> a.getName().equalsIgnoreCase("name1"))
                .findFirst();
        Assert.assertTrue(name2Col.isPresent());
        Assert.assertEquals(20L, name2Col.get().getMaxLength().longValue());

    }

    private void checkSwapTableAndRenameDrop(OnlineSchemaChangeScheduleTaskParameters taskParameters) {
        DBSchemaAccessor dbSchemaAccessor = DBSchemaAccessors.create(connectionSession);
        List<String> renamedTable = dbSchemaAccessor.showTablesLike(taskParameters.getDatabaseName(),
                taskParameters.getRenamedTableName());

        List<String> originTable = dbSchemaAccessor.showTablesLike(taskParameters.getDatabaseName(),
                taskParameters.getOriginTableNameUnWrapped());

        Assert.assertTrue(CollectionUtils.isEmpty(renamedTable));
        Assert.assertFalse(CollectionUtils.isEmpty(originTable));
    }

    private void checkSwapTableFailedAndDropNewTable(OnlineSchemaChangeScheduleTaskParameters taskParameters) {
        DBSchemaAccessor dbSchemaAccessor = DBSchemaAccessors.create(connectionSession);
        List<String> renamedTable = dbSchemaAccessor.showTablesLike(taskParameters.getDatabaseName(),
                taskParameters.getRenamedTableName());

        List<String> originTable = dbSchemaAccessor.showTablesLike(taskParameters.getDatabaseName(),
                taskParameters.getOriginTableNameUnWrapped());

        List<String> newTable = dbSchemaAccessor.showTablesLike(taskParameters.getDatabaseName(),
                taskParameters.getNewTableNameUnWrapped());

        Assert.assertTrue(CollectionUtils.isEmpty(renamedTable));
        Assert.assertFalse(CollectionUtils.isEmpty(originTable));
        Assert.assertTrue(CollectionUtils.isEmpty(newTable));

        List<DBTableColumn> tableColumnFromNew = dbSchemaAccessor.listTableColumns(taskParameters.getDatabaseName(),
                taskParameters.getOriginTableNameUnWrapped());

        Optional<DBTableColumn> name1Col = tableColumnFromNew.stream()
                .filter(a -> a.getName().equalsIgnoreCase("name1"))
                .findFirst();
        Assert.assertTrue(name1Col.isPresent());
        Assert.assertEquals(20L, name1Col.get().getMaxLength().longValue());
    }

}
