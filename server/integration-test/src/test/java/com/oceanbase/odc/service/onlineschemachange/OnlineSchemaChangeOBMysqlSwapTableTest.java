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

import java.text.MessageFormat;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeSqlType;
import com.oceanbase.odc.service.onlineschemachange.model.OriginTableCleanStrategy;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionContext;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionResult;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.ConnectionProvider;
import com.oceanbase.odc.service.onlineschemachange.oscfms.state.OscStates;
import com.oceanbase.odc.service.onlineschemachange.rename.SwapTableUtil;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-06-12
 * @since 4.2.0
 */
@Slf4j
public class OnlineSchemaChangeOBMysqlSwapTableTest extends OBMySqlOscTestEnv {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void test_osc_swap_table_origin_table_reserved_successful() throws Exception {
        String originTableName = getOriginTableName();
        executeOscSwapTable(
                originTableName,
                (orgTableName, ghostTableDDL, ghostTableName, renameTable) -> {
                    // create ghost table only
                    jdbcTemplate.execute(ghostTableDDL);
                },
                c -> c.setOriginTableCleanStrategy(OriginTableCleanStrategy.ORIGIN_TABLE_RENAME_AND_RESERVED),

                this::checkSwapTableAndRenameReserved);
    }

    // table has renamed
    @Test
    public void test_osc_swap_table_origin_table_reserved_successful2() throws Exception {
        String originTableName = getOriginTableName();
        executeOscSwapTable(
                originTableName,
                (orgTableName, ghostTableDDL, ghostTableName, renameTable) -> {
                    // create ghost table
                    jdbcTemplate.execute(ghostTableDDL);
                    // rename to orgTable
                    renameTable(orgTableName, renameTable);
                    // rename ghost to orgTable
                    renameTable(ghostTableName, orgTableName);
                },
                c -> c.setOriginTableCleanStrategy(OriginTableCleanStrategy.ORIGIN_TABLE_RENAME_AND_RESERVED),

                this::checkSwapTableAndRenameReserved);
    }

    @Test
    public void test_osc_swap_table_origin_table_drop_successful() throws Exception {
        String originTableName = getOriginTableName();
        executeOscSwapTable(
                originTableName,
                (orgTableName, ghostTableDDL, ghostTableName, renameTable) -> {
                    // create ghost table
                    jdbcTemplate.execute(ghostTableDDL);
                },
                c -> c.setOriginTableCleanStrategy(OriginTableCleanStrategy.ORIGIN_TABLE_DROP),
                this::checkSwapTableAndRenameDrop);
    }

    // table has renamed
    @Test
    public void test_osc_swap_table_origin_table_drop_successful2() throws Exception {
        String originTableName = getOriginTableName();
        executeOscSwapTable(
                originTableName,
                (orgTableName, ghostTableDDL, ghostTableName, renameTable) -> {
                    // create ghost table
                    jdbcTemplate.execute(ghostTableDDL);
                    // rename to orgTable
                    renameTable(orgTableName, renameTable);
                    // rename ghost to orgTable
                    renameTable(ghostTableName, orgTableName);
                },
                c -> c.setOriginTableCleanStrategy(OriginTableCleanStrategy.ORIGIN_TABLE_DROP),
                this::checkSwapTableAndRenameDrop);
    }

    // table has renamed and old table is dropped
    @Test
    public void test_osc_swap_table_origin_table_drop_successful3() throws Exception {
        String originTableName = getOriginTableName();
        executeOscSwapTable(
                originTableName,
                (orgTableName, ghostTableDDL, ghostTableName, renameTable) -> {
                    // create ghost table
                    jdbcTemplate.execute(ghostTableDDL);
                    // rename to orgTable
                    renameTable(orgTableName, renameTable);
                    // rename ghost to orgTable
                    renameTable(ghostTableName, orgTableName);
                    // drop old table
                    dropTableForTask(renameTable);
                },
                c -> c.setOriginTableCleanStrategy(OriginTableCleanStrategy.ORIGIN_TABLE_DROP),
                this::checkSwapTableAndRenameDrop);
    }

    // table has renamed and old table is dropped
    @Test
    public void test_osc_swap_table_exception() throws Exception {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Swap table name failed after");
        String originTableName = getOriginTableName();
        executeOscSwapTable(
                originTableName,
                (orgTableName, ghostTableDDL, ghostTableName, renameTable) -> {
                    // create ghost table
                    jdbcTemplate.execute(ghostTableDDL);
                    // rename to orgTable
                    renameTable(orgTableName, renameTable);
                    // create origin table again
                    createTableForTask(orgTableName);
                },
                c -> c.setOriginTableCleanStrategy(OriginTableCleanStrategy.ORIGIN_TABLE_DROP),
                this::checkSwapTableAndRenameDrop);
    }


    private void executeOscSwapTable(String originTableName,
            TestTableInitializer testTableInitializer,
            Consumer<OnlineSchemaChangeParameters> changeParametersConsumer,
            Consumer<OnlineSchemaChangeScheduleTaskParameters> resultAssert) throws Exception {
        createTableForTask(originTableName);
        try {
            OnlineSchemaChangeParameters changeParameters = getOnlineSchemaChangeParameters(originTableName);
            changeParametersConsumer.accept(changeParameters);

            ScheduleEntity scheduleEntity = getScheduleEntity(config, changeParameters);
            List<OnlineSchemaChangeScheduleTaskParameters> subTaskParameters =
                    changeParameters.generateSubTaskParameters(config, config.getDefaultSchema());

            Assert.assertEquals(1, subTaskParameters.size());
            OnlineSchemaChangeScheduleTaskParameters taskParameters = subTaskParameters.get(0);

            ScheduleTaskEntity scheduleTaskEntity = getScheduleTaskEntity(scheduleEntity.getId(), taskParameters);
            testTableInitializer.initTable(taskParameters.getOriginTableNameUnwrapped(),
                    taskParameters.getNewTableCreateDdl(),
                    taskParameters.getNewTableNameUnwrapped(), taskParameters.getRenamedTableNameUnwrapped());
            OscActionContext swapTableAction = new OscActionContext();
            swapTableAction.setScheduleTaskRepository(scheduleTaskRepository);
            swapTableAction.setParameter(changeParameters);
            swapTableAction.setTaskParameter(taskParameters);
            swapTableAction.setSchedule(scheduleEntity);
            swapTableAction.setScheduleTask(scheduleTaskEntity);
            swapTableAction.setConnectionProvider(new ConnectionProvider() {
                @Override
                public ConnectionConfig connectionConfig() {
                    return config;
                }

                @Override
                public ConnectionSession createConnectionSession() {
                    // connection will released after use
                    return new DefaultConnectSessionFactory(config).generateSession();
                }
            });
            OscActionResult actionResult = swapTableNameValve.execute(swapTableAction);
            Assert.assertEquals(actionResult.getNextState(), OscStates.CLEAN_RESOURCE.getState());
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

    private void renameTable(String originTable, String targetTable) {
        String sql = String.format("rename table %s to %s", SwapTableUtil.quoteMySQLName(originTable),
                SwapTableUtil.quoteMySQLName(targetTable));
        jdbcTemplate.execute(sql);
    }

    private interface TestTableInitializer {
        void initTable(String originTable, String ghostTableDDL, String ghostTableName, String renamedTableName);
    }
}
