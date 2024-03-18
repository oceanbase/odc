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
package com.oceanbase.odc.service.partitionplan;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanEntity;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanRepository;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanTableEntity;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanTablePartitionKeyEntity;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanTablePartitionKeyRepository;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanTableRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.plugin.task.api.partitionplan.datatype.TimeDataType;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.partitionname.PartitionNameGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.PartitionPlanVariableKey;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.SqlExprBasedGeneratorConfig;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.TimeIncreaseGeneratorConfig;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanConfig;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanKeyConfig;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanStrategy;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanTableConfig;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;
import com.oceanbase.odc.test.tool.TestRandom;

/**
 * Test cases for {@link PartitionPlanScheduleService}
 *
 * @author yh263208
 * @date 2024-02-07 13:52
 * @since ODC_release_4.2.4
 */
public class PartitionPlanScheduleServiceTest extends ServiceTestEnv {

    public static final String MYSQL_REAL_RANGE_TABLE_NAME = "range_svc_parti_tbl";
    @MockBean
    private ScheduleService scheduleService;
    @MockBean
    private DatabaseService databaseService;
    @MockBean
    private FlowInstanceService flowInstanceService;
    @Autowired
    private PartitionPlanService partitionPlanService;
    @Autowired
    private PartitionPlanScheduleService partitionPlanScheduleService;
    @Autowired
    private PartitionPlanTableRepository partitionPlanTableRepository;
    @Autowired
    private PartitionPlanTablePartitionKeyRepository partitionPlanTablePartitionKeyRepository;
    @Autowired
    private PartitionPlanRepository partitionPlanRepository;

    @Before
    public void setUp() {
        this.partitionPlanRepository.deleteAll();
        this.partitionPlanTableRepository.deleteAll();
        this.partitionPlanTablePartitionKeyRepository.deleteAll();
    }

    @Test
    public void disablePartitionPlanTables_enableEntitiesExists_disableSucceed() throws SchedulerException {
        PartitionPlanTableEntity ppt = TestRandom.nextObject(PartitionPlanTableEntity.class);
        ppt.setId(null);
        ppt.setEnabled(true);
        ppt = this.partitionPlanTableRepository.save(ppt);

        PartitionPlanTablePartitionKeyEntity pptk1 = TestRandom.nextObject(PartitionPlanTablePartitionKeyEntity.class);
        pptk1.setId(null);
        pptk1.setPartitionplanTableId(ppt.getId());
        pptk1 = this.partitionPlanTablePartitionKeyRepository.save(pptk1);

        PartitionPlanTablePartitionKeyEntity pptk2 = TestRandom.nextObject(PartitionPlanTablePartitionKeyEntity.class);
        pptk2.setId(null);
        pptk2.setPartitionplanTableId(ppt.getId());
        pptk2 = this.partitionPlanTablePartitionKeyRepository.save(pptk2);
        Mockito.when(this.scheduleService.nullSafeGetById(Mockito.anyLong()))
                .thenReturn(TestRandom.nextObject(ScheduleEntity.class));
        Mockito.doNothing().when(this.scheduleService).terminate(Mockito.isA(ScheduleEntity.class));
        this.partitionPlanScheduleService.disablePartitionPlanTables(Collections.singletonList(ppt.getId()));
        List<PartitionPlanTablePartitionKeyEntity> expect1 = this.partitionPlanTablePartitionKeyRepository
                .findByIdIn(Arrays.asList(pptk1.getId(), pptk2.getId()));
        Optional<PartitionPlanTableEntity> optional = this.partitionPlanTableRepository.findById(ppt.getId());
        Assert.assertTrue(Boolean.FALSE.equals(optional.get().getEnabled()));
    }

    @Test
    public void disablePartitionPlan_enableEntitiesExists_disableSucceed() throws SchedulerException {
        PartitionPlanEntity pp = TestRandom.nextObject(PartitionPlanEntity.class);
        pp.setId(null);
        pp.setEnabled(true);
        pp = this.partitionPlanRepository.save(pp);

        PartitionPlanTableEntity ppt = TestRandom.nextObject(PartitionPlanTableEntity.class);
        ppt.setId(null);
        ppt.setEnabled(true);
        ppt.setPartitionPlanId(pp.getId());
        ppt = this.partitionPlanTableRepository.save(ppt);

        PartitionPlanTablePartitionKeyEntity pptk1 = TestRandom.nextObject(PartitionPlanTablePartitionKeyEntity.class);
        pptk1.setId(null);
        pptk1.setPartitionplanTableId(ppt.getId());
        pptk1 = this.partitionPlanTablePartitionKeyRepository.save(pptk1);

        PartitionPlanTablePartitionKeyEntity pptk2 = TestRandom.nextObject(PartitionPlanTablePartitionKeyEntity.class);
        pptk2.setId(null);
        pptk2.setPartitionplanTableId(ppt.getId());
        pptk2 = this.partitionPlanTablePartitionKeyRepository.save(pptk2);
        Mockito.when(this.scheduleService.nullSafeGetById(Mockito.anyLong()))
                .thenReturn(TestRandom.nextObject(ScheduleEntity.class));
        Mockito.doNothing().when(this.scheduleService).terminate(Mockito.isA(ScheduleEntity.class));
        this.partitionPlanScheduleService.disablePartitionPlan(pp.getDatabaseId());
        List<PartitionPlanTablePartitionKeyEntity> expect1 = this.partitionPlanTablePartitionKeyRepository
                .findByIdIn(Arrays.asList(pptk1.getId(), pptk2.getId()));
        Optional<PartitionPlanTableEntity> optional = this.partitionPlanTableRepository.findById(ppt.getId());
        Optional<PartitionPlanEntity> optional1 = this.partitionPlanRepository.findById(pp.getId());
        Assert.assertTrue(Boolean.FALSE.equals(optional.get().getEnabled())
                && Boolean.FALSE.equals(optional1.get().getEnabled()));
    }

    @Test
    public void submit_onlyCreateTrigger_submitSucceed() throws ClassNotFoundException, SchedulerException {
        PartitionPlanTableConfig tableConfig = new PartitionPlanTableConfig();
        tableConfig.setTableName(MYSQL_REAL_RANGE_TABLE_NAME);
        tableConfig.setPartitionNameInvoker("CUSTOM_PARTITION_NAME_GENERATOR");
        SqlExprBasedGeneratorConfig config = new SqlExprBasedGeneratorConfig();
        config.setGenerateExpr("concat('p', date_format(from_unixtime(unix_timestamp("
                + "STR_TO_DATE(20240125, '%Y%m%d')) + "
                + PartitionPlanVariableKey.INTERVAL.getVariable() + "), '%Y%m%d'))");
        config.setIntervalGenerateExpr("86400");
        tableConfig.setPartitionNameInvokerParameters(getSqlExprBasedNameGeneratorParameters(config));
        PartitionPlanKeyConfig c3Create = getMysqlc3CreateConfig();
        PartitionPlanKeyConfig datekeyCreate = getMysqldatekeyCreateConfig();
        PartitionPlanKeyConfig dropConfig = getDropConfig();
        tableConfig.setPartitionKeyConfigs(Arrays.asList(c3Create, datekeyCreate, dropConfig));

        PartitionPlanConfig partitionPlanConfig = new PartitionPlanConfig();
        partitionPlanConfig.setPartitionTableConfigs(Collections.singletonList(tableConfig));
        partitionPlanConfig.setFlowInstanceId(1L);
        partitionPlanConfig.setTimeoutMillis(180000L);
        partitionPlanConfig.setDatabaseId(1L);
        partitionPlanConfig.setEnabled(true);
        partitionPlanConfig.setCreationTrigger(TestRandom.nextObject(TriggerConfig.class));
        Database database = TestRandom.nextObject(Database.class);
        database.setId(1L);
        Mockito.when(this.databaseService.detail(1L)).thenReturn(database);
        Mockito.when(this.scheduleService.create(Mockito.any()))
                .thenReturn(TestRandom.nextObject(ScheduleEntity.class));
        Mockito.doNothing().when(this.scheduleService).enable(Mockito.isA(ScheduleEntity.class));
        this.partitionPlanScheduleService.submit(partitionPlanConfig);
        List<PartitionPlanTableEntity> actuals = this.partitionPlanTableRepository.findAll();
        Set<Long> scheduleIds = actuals.stream()
                .map(PartitionPlanTableEntity::getScheduleId).collect(Collectors.toSet());
        Assert.assertEquals(scheduleIds.size(), 1);
    }

    @Test
    public void submit_bothCreateAndDropTrigger_submitSucceed() throws ClassNotFoundException, SchedulerException {
        PartitionPlanTableConfig tableConfig = new PartitionPlanTableConfig();
        tableConfig.setTableName(MYSQL_REAL_RANGE_TABLE_NAME);
        tableConfig.setPartitionNameInvoker("CUSTOM_PARTITION_NAME_GENERATOR");
        SqlExprBasedGeneratorConfig config = new SqlExprBasedGeneratorConfig();
        config.setGenerateExpr("concat('p', date_format(from_unixtime(unix_timestamp("
                + "STR_TO_DATE(20240125, '%Y%m%d')) + "
                + PartitionPlanVariableKey.INTERVAL.getVariable() + "), '%Y%m%d'))");
        config.setIntervalGenerateExpr("86400");
        tableConfig.setPartitionNameInvokerParameters(getSqlExprBasedNameGeneratorParameters(config));
        PartitionPlanKeyConfig c3Create = getMysqlc3CreateConfig();
        PartitionPlanKeyConfig datekeyCreate = getMysqldatekeyCreateConfig();
        PartitionPlanKeyConfig dropConfig = getDropConfig();
        tableConfig.setPartitionKeyConfigs(Arrays.asList(c3Create, datekeyCreate, dropConfig));

        PartitionPlanConfig partitionPlanConfig = new PartitionPlanConfig();
        partitionPlanConfig.setPartitionTableConfigs(Collections.singletonList(tableConfig));
        partitionPlanConfig.setFlowInstanceId(1L);
        partitionPlanConfig.setTimeoutMillis(180000L);
        partitionPlanConfig.setEnabled(true);
        partitionPlanConfig.setDatabaseId(1L);

        long t1 = System.currentTimeMillis();
        long t2 = System.currentTimeMillis() + 1000;
        TriggerConfig createTrigger = TestRandom.nextObject(TriggerConfig.class);
        createTrigger.setStartAt(new Date(t1));
        partitionPlanConfig.setCreationTrigger(createTrigger);

        TriggerConfig dropTrigger = TestRandom.nextObject(TriggerConfig.class);
        dropTrigger.setStartAt(new Date(t2));
        partitionPlanConfig.setDroppingTrigger(dropTrigger);
        Database database = TestRandom.nextObject(Database.class);
        database.setId(1L);
        Mockito.when(this.databaseService.detail(1L)).thenReturn(database);
        Mockito.when(this.scheduleService.create(Mockito.argThat(s -> {
            if (s == null) {
                return true;
            }
            TriggerConfig config1 = JsonUtils.fromJson(s.getTriggerConfigJson(), TriggerConfig.class);
            return config1.getStartAt().getTime() == t1;
        }))).thenReturn(TestRandom.nextObject(ScheduleEntity.class));
        Mockito.when(this.scheduleService.create(Mockito.argThat(s -> {
            if (s == null) {
                return true;
            }
            TriggerConfig config1 = JsonUtils.fromJson(s.getTriggerConfigJson(), TriggerConfig.class);
            return config1.getStartAt().getTime() == t2;
        }))).thenReturn(TestRandom.nextObject(ScheduleEntity.class));
        Mockito.doNothing().when(this.scheduleService).enable(Mockito.isA(ScheduleEntity.class));
        this.partitionPlanScheduleService.submit(partitionPlanConfig);
        List<PartitionPlanTableEntity> actuals = this.partitionPlanTableRepository.findAll();
        Set<Long> scheduleIds = actuals.stream()
                .map(PartitionPlanTableEntity::getScheduleId).collect(Collectors.toSet());
        Assert.assertEquals(scheduleIds.size(), 2);
    }

    @Test
    public void getPartitionPlanByFlowInstanceId_onlyCreateTrigger_getSucceed()
            throws ClassNotFoundException, SchedulerException {
        PartitionPlanTableConfig tableConfig = new PartitionPlanTableConfig();
        tableConfig.setTableName(MYSQL_REAL_RANGE_TABLE_NAME);
        tableConfig.setPartitionNameInvoker("CUSTOM_PARTITION_NAME_GENERATOR");
        SqlExprBasedGeneratorConfig config = new SqlExprBasedGeneratorConfig();
        config.setGenerateExpr("concat('p', date_format(from_unixtime(unix_timestamp("
                + "STR_TO_DATE(20240125, '%Y%m%d')) + "
                + PartitionPlanVariableKey.INTERVAL.getVariable() + "), '%Y%m%d'))");
        config.setIntervalGenerateExpr("86400");
        tableConfig.setPartitionNameInvokerParameters(getSqlExprBasedNameGeneratorParameters(config));
        PartitionPlanKeyConfig c3Create = getMysqlc3CreateConfig();
        PartitionPlanKeyConfig datekeyCreate = getMysqldatekeyCreateConfig();
        PartitionPlanKeyConfig dropConfig = getDropConfig();
        tableConfig.setPartitionKeyConfigs(Arrays.asList(c3Create, datekeyCreate, dropConfig));

        PartitionPlanConfig partitionPlanConfig = new PartitionPlanConfig();
        partitionPlanConfig.setPartitionTableConfigs(Collections.singletonList(tableConfig));
        partitionPlanConfig.setFlowInstanceId(1L);
        partitionPlanConfig.setTimeoutMillis(180000L);
        partitionPlanConfig.setDatabaseId(1L);
        partitionPlanConfig.setEnabled(true);
        partitionPlanConfig.setCreationTrigger(TestRandom.nextObject(TriggerConfig.class));
        Database database = TestRandom.nextObject(Database.class);
        database.setId(1L);
        Mockito.when(this.databaseService.detail(1L)).thenReturn(database);
        Mockito.when(this.scheduleService.create(Mockito.any()))
                .thenReturn(TestRandom.nextObject(ScheduleEntity.class));
        Mockito.doNothing().when(this.scheduleService).enable(Mockito.isA(ScheduleEntity.class));
        this.partitionPlanScheduleService.submit(partitionPlanConfig);
        Mockito.when(this.flowInstanceService.mapFlowInstance(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(partitionPlanConfig.getFlowInstanceId());
        PartitionPlanConfig expect = this.partitionPlanScheduleService
                .getPartitionPlanByFlowInstanceId(partitionPlanConfig.getFlowInstanceId());
        Assert.assertEquals(1, expect.getPartitionTableConfigs().size());
    }

    @Test
    public void getPartitionPlanByDatabaseId_onlyCreateTrigger_getSucceed()
            throws ClassNotFoundException, SchedulerException {
        PartitionPlanTableConfig tableConfig = new PartitionPlanTableConfig();
        tableConfig.setTableName(MYSQL_REAL_RANGE_TABLE_NAME);
        tableConfig.setPartitionNameInvoker("CUSTOM_PARTITION_NAME_GENERATOR");
        SqlExprBasedGeneratorConfig config = new SqlExprBasedGeneratorConfig();
        config.setGenerateExpr("concat('p', date_format(from_unixtime(unix_timestamp("
                + "STR_TO_DATE(20240125, '%Y%m%d')) + "
                + PartitionPlanVariableKey.INTERVAL.getVariable() + "), '%Y%m%d'))");
        config.setIntervalGenerateExpr("86400");
        tableConfig.setPartitionNameInvokerParameters(getSqlExprBasedNameGeneratorParameters(config));
        PartitionPlanKeyConfig c3Create = getMysqlc3CreateConfig();
        PartitionPlanKeyConfig datekeyCreate = getMysqldatekeyCreateConfig();
        PartitionPlanKeyConfig dropConfig = getDropConfig();
        tableConfig.setPartitionKeyConfigs(Arrays.asList(c3Create, datekeyCreate, dropConfig));

        PartitionPlanConfig partitionPlanConfig = new PartitionPlanConfig();
        partitionPlanConfig.setPartitionTableConfigs(Collections.singletonList(tableConfig));
        partitionPlanConfig.setFlowInstanceId(1L);
        partitionPlanConfig.setTimeoutMillis(180000L);
        partitionPlanConfig.setDatabaseId(1L);
        partitionPlanConfig.setEnabled(true);
        partitionPlanConfig.setCreationTrigger(TestRandom.nextObject(TriggerConfig.class));
        Database database = TestRandom.nextObject(Database.class);
        database.setId(1L);
        Mockito.when(this.databaseService.detail(1L)).thenReturn(database);
        Mockito.when(this.scheduleService.create(Mockito.any()))
                .thenReturn(TestRandom.nextObject(ScheduleEntity.class));
        Mockito.doNothing().when(this.scheduleService).enable(Mockito.isA(ScheduleEntity.class));
        this.partitionPlanScheduleService.submit(partitionPlanConfig);
        Mockito.when(this.flowInstanceService.mapFlowInstance(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(partitionPlanConfig.getFlowInstanceId());
        PartitionPlanConfig expect = this.partitionPlanScheduleService
                .getPartitionPlanByDatabaseId(partitionPlanConfig.getFlowInstanceId());
        Assert.assertEquals(1, expect.getPartitionTableConfigs().size());
    }

    @Test
    public void getPartitionPlanTables_onlyCreateTrigger_getSucceed()
            throws ClassNotFoundException, SchedulerException {
        PartitionPlanTableConfig tableConfig = new PartitionPlanTableConfig();
        tableConfig.setTableName(MYSQL_REAL_RANGE_TABLE_NAME);
        tableConfig.setPartitionNameInvoker("CUSTOM_PARTITION_NAME_GENERATOR");
        SqlExprBasedGeneratorConfig config = new SqlExprBasedGeneratorConfig();
        config.setGenerateExpr("concat('p', date_format(from_unixtime(unix_timestamp("
                + "STR_TO_DATE(20240125, '%Y%m%d')) + "
                + PartitionPlanVariableKey.INTERVAL.getVariable() + "), '%Y%m%d'))");
        config.setIntervalGenerateExpr("86400");
        tableConfig.setPartitionNameInvokerParameters(getSqlExprBasedNameGeneratorParameters(config));
        PartitionPlanKeyConfig c3Create = getMysqlc3CreateConfig();
        PartitionPlanKeyConfig datekeyCreate = getMysqldatekeyCreateConfig();
        PartitionPlanKeyConfig dropConfig = getDropConfig();
        tableConfig.setPartitionKeyConfigs(Arrays.asList(c3Create, datekeyCreate, dropConfig));

        PartitionPlanConfig partitionPlanConfig = new PartitionPlanConfig();
        partitionPlanConfig.setPartitionTableConfigs(Collections.singletonList(tableConfig));
        partitionPlanConfig.setFlowInstanceId(1L);
        partitionPlanConfig.setTimeoutMillis(180000L);
        partitionPlanConfig.setEnabled(true);
        partitionPlanConfig.setDatabaseId(1L);
        partitionPlanConfig.setCreationTrigger(TestRandom.nextObject(TriggerConfig.class));
        Database database = TestRandom.nextObject(Database.class);
        database.setId(1L);
        Mockito.when(this.databaseService.detail(1L)).thenReturn(database);
        Mockito.when(this.scheduleService.create(Mockito.any()))
                .thenReturn(TestRandom.nextObject(ScheduleEntity.class));
        Mockito.doNothing().when(this.scheduleService).enable(Mockito.isA(ScheduleEntity.class));
        this.partitionPlanScheduleService.submit(partitionPlanConfig);
        Mockito.when(this.flowInstanceService.mapFlowInstance(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(partitionPlanConfig.getFlowInstanceId());
        PartitionPlanConfig cfg = this.partitionPlanScheduleService
                .getPartitionPlanByFlowInstanceId(partitionPlanConfig.getFlowInstanceId());
        List<PartitionPlanTableConfig> expects = this.partitionPlanScheduleService.getPartitionPlanTables(
                cfg.getPartitionTableConfigs().stream()
                        .map(PartitionPlanTableConfig::getId).collect(Collectors.toList()));
        Assert.assertEquals(1, expects.size());
    }

    private Map<String, Object> getSqlExprBasedNameGeneratorParameters(SqlExprBasedGeneratorConfig config) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(PartitionNameGenerator.PARTITION_NAME_GENERATOR_KEY, config);
        return parameters;
    }

    private PartitionPlanKeyConfig getMysqlc3CreateConfig() {
        PartitionPlanKeyConfig c3Create = new PartitionPlanKeyConfig();
        c3Create.setPartitionKey("c3");
        c3Create.setStrategy(PartitionPlanStrategy.CREATE);
        c3Create.setPartitionKeyInvoker("TIME_INCREASING_GENERATOR");
        TimeIncreaseGeneratorConfig config1 = new TimeIncreaseGeneratorConfig();
        long current = 1706180200490L;// 2024-01-25 18:57
        config1.setBaseTimestampMillis(current);
        config1.setInterval(1);
        config1.setIntervalPrecision(TimeDataType.DAY);
        c3Create.setPartitionKeyInvokerParameters(new HashMap<>());
        return c3Create;
    }

    private PartitionPlanKeyConfig getMysqldatekeyCreateConfig() {
        PartitionPlanKeyConfig datekeyCreate = new PartitionPlanKeyConfig();
        datekeyCreate.setPartitionKey("`datekey`");
        datekeyCreate.setStrategy(PartitionPlanStrategy.CREATE);
        datekeyCreate.setPartitionKeyInvoker("CUSTOM_GENERATOR");
        SqlExprBasedGeneratorConfig config = new SqlExprBasedGeneratorConfig();
        config.setIntervalGenerateExpr("86400");
        config.setGenerateExpr("cast(date_format(from_unixtime(unix_timestamp(STR_TO_DATE("
                + PartitionPlanVariableKey.LAST_PARTITION_VALUE.getVariable()
                + ", '%Y%m%d')) + " + PartitionPlanVariableKey.INTERVAL.getVariable()
                + "), '%Y%m%d') as signed)");
        datekeyCreate.setPartitionKeyInvokerParameters(new HashMap<>());
        return datekeyCreate;
    }

    private PartitionPlanKeyConfig getDropConfig() {
        PartitionPlanKeyConfig dropConfig = new PartitionPlanKeyConfig();
        dropConfig.setPartitionKey(null);
        dropConfig.setStrategy(PartitionPlanStrategy.DROP);
        dropConfig.setPartitionKeyInvoker("KEEP_MOST_LATEST_GENERATOR");
        dropConfig.setPartitionKeyInvokerParameters(new HashMap<>());
        return dropConfig;
    }

}
