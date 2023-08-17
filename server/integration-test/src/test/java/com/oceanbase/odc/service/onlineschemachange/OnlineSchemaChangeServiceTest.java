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

import static com.oceanbase.odc.service.schedule.model.JobType.ONLINE_SCHEMA_CHANGE_COMPLETE;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.TriggerKey;
import org.quartz.TriggerListener;
import org.quartz.listeners.TriggerListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.test.context.TestPropertySource;

import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.collaboration.environment.EnvironmentService;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.onlineschemachange.ddl.DdlUtils;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeSqlType;
import com.oceanbase.odc.service.onlineschemachange.model.OriginTableCleanStrategy;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.DataSourceOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.ProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.request.CreateOceanBaseDataSourceRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.CreateProjectRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ListProjectFullVerifyResultRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ProjectControlRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectFullVerifyResultResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectProgressResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectStepVO;
import com.oceanbase.odc.service.onlineschemachange.pipeline.OscValveContext;
import com.oceanbase.odc.service.onlineschemachange.pipeline.SwapTableNameValve;
import com.oceanbase.odc.service.quartz.QuartzJobService;
import com.oceanbase.odc.service.quartz.model.MisfireStrategy;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.schedule.model.CreateQuartzJobReq;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.schedule.model.QuartzKeyGenerator;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;
import com.oceanbase.odc.service.schedule.model.TriggerStrategy;
import com.oceanbase.odc.service.session.DBSessionManageFacade;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-06-12
 * @since 4.2.0
 */
@Slf4j
@TestPropertySource(properties = "check-osc-task-cron-expression=0/5 * * * * ?")
public class OnlineSchemaChangeServiceTest extends OscTestEnv {

    @Autowired
    private OnlineSchemaChangeTaskHandler onlineSchemaChangeTaskHandler;
    @MockBean
    private ConnectionService connectionService;

    @MockBean
    private ProjectOpenApiService projectOpenApiService;
    @MockBean
    private DataSourceOpenApiService dataSourceOpenApiService;

    @Autowired
    private QuartzJobService quartzJobService;

    @Autowired
    private ScheduleTaskRepository scheduleTaskRepository;
    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private SwapTableNameValve swapTableNameValve;

    @MockBean
    private DBSessionManageFacade dbSessionManager;

    @MockBean
    private EnvironmentService environmentService;

    @Autowired
    private ScheduleTaskService scheduleTaskService;

    private final static Long CONNECTION_ID = 1001L;
    private static Long SCHEDULE_ID;
    private static Long SCHEDULE_TASK_ID;

    private static String originTableName;
    private static String newTableName;
    private static String oldTableName;
    private static SyncJdbcExecutor jdbcTemplate;

    private static String newDdl;
    private static String originDdl;
    private static ConnectionConfig config;

    private ScheduleEntity scheduleEntity;
    private ScheduleTaskEntity scheduleTaskEntity;
    private OnlineSchemaChangeScheduleTaskParameters taskParameters;
    private OriginTableCleanStrategy originTableCleanStrategy;
    private static ConnectionSession connectionSession;

    /**
     * the main thread will terminate after #{scheduleTimeOut} second
     */

    @BeforeClass
    public static void before() {
        config = TestConnectionUtil.getTestConnectionConfig(ConnectType.OB_MYSQL);
        connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        jdbcTemplate = connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);

        Assert.assertNotNull(jdbcTemplate);
        originTableName = "t1";
        newTableName = DdlUtils.getNewTableName(originTableName);
        oldTableName = DdlUtils.getRenamedTableName(originTableName);

        originDdl = MessageFormat.format("create table {0} (id int(20) primary key, name1 varchar(20))",
                originTableName);
        newDdl = MessageFormat.format("create table {0}(id int(20) primary key, "
                + "name1 varchar(20), name2 varchar(20))", newTableName);
    }

    @AfterClass
    public static void tearDown() {
        dropTable();
    }

    private static void createTable(boolean createNewTable) {
        jdbcTemplate.execute(originDdl);
        jdbcTemplate.execute(
                MessageFormat.format("insert into {0}(id, name1) values(1,''abc''),(2,''efg'')", originTableName));
        if (createNewTable) {
            jdbcTemplate.execute(newDdl);
        }
    }

    private static void dropTable() {
        if (jdbcTemplate != null) {
            jdbcTemplate.execute(MessageFormat.format("drop table if exists {0}", originTableName));
            jdbcTemplate.execute(MessageFormat.format("drop table if exists {0}", newTableName));
            jdbcTemplate.execute(MessageFormat.format("drop table if exists {0}", oldTableName));
        }
    }

    @Before
    public void prepareTaskEntity() {
        this.originTableCleanStrategy = OriginTableCleanStrategy.ORIGIN_TABLE_RENAME_AND_RESERVED;
        OnlineSchemaChangeParameters changeParameters = new OnlineSchemaChangeParameters();
        changeParameters.setSwapTableNameRetryTimes(3);
        changeParameters.setSqlType(OnlineSchemaChangeSqlType.CREATE);
        changeParameters.setErrorStrategy(TaskErrorStrategy.ABORT);
        changeParameters.setOriginTableCleanStrategy(OriginTableCleanStrategy.ORIGIN_TABLE_RENAME_AND_RESERVED);
        this.scheduleEntity = getScheduleEntity(config, changeParameters);
        this.taskParameters = getOnlineSchemaChangeScheduleTaskParameters(config);
        this.scheduleTaskEntity = getScheduleTaskEntity(SCHEDULE_ID, taskParameters);
        when(connectionService.getForConnectionSkipPermissionCheck(eq(CONNECTION_ID))).thenReturn(config);
        mock();
    }

    public void test_osc_task_step_finished() throws SchedulerException, InterruptedException {
        Mockito.when(environmentService.detailSkipPermissionCheck(Mockito.anyLong())).thenReturn(getEnvironment());
        dropTable();
        createTable(false);
        onlineSchemaChangeTaskHandler.start(SCHEDULE_ID, SCHEDULE_TASK_ID);
        listenTaskCompleted(SCHEDULE_TASK_ID, 1);
    }

    @Test
    public void test_osc_swap_table() {
        dropTable();
        createTable(true);
        OscValveContext context = new OscValveContext();
        context.setSchedule(scheduleEntity);
        context.setScheduleTask(scheduleTaskEntity);
        context.setTaskParameter(taskParameters);
        context.setParameter(
                JsonUtils.fromJson(scheduleEntity.getJobParametersJson(), OnlineSchemaChangeParameters.class));
        context.setConnectionConfig(config);
        doNothing().when(projectOpenApiService).releaseProject(Mockito.any(ProjectControlRequest.class));
        doNothing().when(projectOpenApiService).stopProject(Mockito.any(ProjectControlRequest.class));

        swapTableNameValve.invoke(context);
        checkSwapTableAndRename();
    }

    @Test
    public void test_osc_multi_task_triggered() {
        Mockito.when(environmentService.detailSkipPermissionCheck(Mockito.anyLong())).thenReturn(getEnvironment());
        dropTable();
        createTableForMultiTask();
        try {
            OnlineSchemaChangeParameters changeParameters = new OnlineSchemaChangeParameters();
            changeParameters.setSwapTableNameRetryTimes(3);
            changeParameters.setSqlType(OnlineSchemaChangeSqlType.CREATE);
            changeParameters.setErrorStrategy(TaskErrorStrategy.ABORT);
            changeParameters.setOriginTableCleanStrategy(OriginTableCleanStrategy.ORIGIN_TABLE_RENAME_AND_RESERVED);
            changeParameters.setSqlContent("create table t1 (id int(20) primary key, name1 varchar(20));"
                    + "create table t2 (id int(20) primary key, name1 varchar(20));");
            ScheduleEntity schedule = getScheduleEntity(config, changeParameters);
            List<OnlineSchemaChangeScheduleTaskParameters> subTaskParameters =
                    changeParameters.generateSubTaskParameters(config, config.defaultSchema());
            List<ScheduleTaskEntity> taskEntities = new ArrayList<>();
            subTaskParameters.forEach(
                    taskParameter -> taskEntities.add(getScheduleTaskEntity(schedule.getId(), taskParameter)));
            onlineSchemaChangeTaskHandler.start(schedule.getId(), taskEntities.get(0).getId());
            listenTaskCompleted(schedule.getId(), subTaskParameters.size());
        } catch (SchedulerException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            dropTableForMultiTask();
        }
    }

    private void checkSwapTableAndRename() {

        DBSchemaAccessor dbSchemaAccessor = DBSchemaAccessors.create(connectionSession);
        List<String> renamedTable = dbSchemaAccessor.showTablesLike(taskParameters.getDatabaseName(),
                taskParameters.getRenamedTableName());

        List<String> originTable = dbSchemaAccessor.showTablesLike(taskParameters.getDatabaseName(),
                taskParameters.getOriginTableNameUnWrapped());

        Assert.assertFalse(CollectionUtils.isEmpty(renamedTable));
        Assert.assertFalse(CollectionUtils.isEmpty(originTable));

    }

    private ScheduleTaskEntity getScheduleTaskEntity(Long scheduleId,
            OnlineSchemaChangeScheduleTaskParameters oscScheduleTaskParameters) {
        ScheduleTaskEntity scheduleTaskEntity = new ScheduleTaskEntity();
        scheduleTaskEntity.setParametersJson(JsonUtils.toJson(oscScheduleTaskParameters));
        scheduleTaskEntity.setStatus(TaskStatus.PREPARING);
        scheduleTaskEntity.setJobName(scheduleId + "");
        scheduleTaskEntity.setJobGroup(ONLINE_SCHEMA_CHANGE_COMPLETE.name());
        scheduleTaskEntity.setProgressPercentage(0.0);
        scheduleTaskEntity.setResultJson("");
        scheduleTaskEntity.setFireTime(new Date());

        ScheduleTaskEntity scheduleTaskEntitySaved = scheduleTaskRepository.save(scheduleTaskEntity);
        SCHEDULE_TASK_ID = scheduleTaskEntitySaved.getId();
        return scheduleTaskEntity;
    }

    private ScheduleEntity getScheduleEntity(ConnectionConfig config, OnlineSchemaChangeParameters changeParameters) {
        ScheduleEntity scheduleEntity = new ScheduleEntity();
        scheduleEntity.setStatus(ScheduleStatus.ENABLED);
        scheduleEntity.setConnectionId(config.getId());
        scheduleEntity.setDatabaseName(config.getDefaultSchema());
        scheduleEntity.setDatabaseId(1L);
        scheduleEntity.setProjectId(1L);
        scheduleEntity.setOrganizationId(1L);
        scheduleEntity.setCreatorId(1L);
        scheduleEntity.setModifierId(1L);
        scheduleEntity.setStatus(ScheduleStatus.ENABLED);
        scheduleEntity.setAllowConcurrent(false);
        scheduleEntity.setMisfireStrategy(MisfireStrategy.MISFIRE_INSTRUCTION_DO_NOTHING);
        scheduleEntity.setJobType(JobType.ONLINE_SCHEMA_CHANGE_COMPLETE);
        scheduleEntity.setJobParametersJson(JsonUtils.toJson(changeParameters));
        TriggerConfig triggerConfig = new TriggerConfig();
        triggerConfig.setTriggerStrategy(TriggerStrategy.START_NOW);
        scheduleEntity.setTriggerConfigJson(JsonUtils.toJson(triggerConfig));
        ScheduleEntity savedScheduleEntity = scheduleRepository.saveAndFlush(scheduleEntity);
        SCHEDULE_ID = savedScheduleEntity.getId();
        return savedScheduleEntity;
    }

    private void listenTaskCompleted(Long scheduleId, Integer taskNumber)
            throws SchedulerException, InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(taskNumber);
        TriggerKey triggerKey = QuartzKeyGenerator.generateTriggerKey(scheduleId, ONLINE_SCHEMA_CHANGE_COMPLETE);

        TriggerListener listener = getTriggerListener(triggerKey.toString(), countDownLatch);
        quartzJobService.addTriggerListener(triggerKey, listener);
        boolean triggerCompleted = countDownLatch.await(30, TimeUnit.SECONDS);
        Assert.assertTrue(triggerCompleted);
    }

    private void mock() {
        String datasourceId = UUID.randomUUID().toString();
        String projectId = UUID.randomUUID().toString();

        when(dataSourceOpenApiService.createOceanBaseDataSource(Mockito.any(CreateOceanBaseDataSourceRequest.class)))
                .thenReturn(datasourceId);

        when(projectOpenApiService.createProject(Mockito.any(CreateProjectRequest.class)))
                .thenReturn(projectId);

        doNothing().when(projectOpenApiService).startProject(Mockito.any(ProjectControlRequest.class));
        ProjectStepResultTest projectStepResultTest = new ProjectStepResultTest();
        List<ProjectStepVO> projectSteps = projectStepResultTest.projectSteps();

        ProjectFullVerifyResultResponse fullVerifyResultResponse = projectStepResultTest.verifyResult();

        doReturn(projectSteps).when(projectOpenApiService)
                .describeProjectSteps(Mockito.any(ProjectControlRequest.class));

        doReturn(new ProjectProgressResponse()).when(projectOpenApiService)
                .describeProjectProgress(Mockito.any(ProjectControlRequest.class));
        doReturn(fullVerifyResultResponse).when(projectOpenApiService)
                .listProjectFullVerifyResult(Mockito.any(ListProjectFullVerifyResultRequest.class));

        doNothing().when(dbSessionManager)
                .killAllSessions(Mockito.any(ConnectionSession.class), Mockito.any(Predicate.class), Mockito.anyInt());

    }

    private static OnlineSchemaChangeScheduleTaskParameters getOnlineSchemaChangeScheduleTaskParameters(
            ConnectionConfig config) {
        OnlineSchemaChangeScheduleTaskParameters oscScheduleTaskParameters =
                new OnlineSchemaChangeScheduleTaskParameters();
        oscScheduleTaskParameters.setConnectionId(config.getId());
        oscScheduleTaskParameters.setDatabaseName(config.getDefaultSchema());
        oscScheduleTaskParameters.setOriginTableName(originTableName);
        oscScheduleTaskParameters.setRenamedTableName(oldTableName);
        oscScheduleTaskParameters.setNewTableName(newTableName);
        oscScheduleTaskParameters.setNewTableCreateDdl(newDdl);
        oscScheduleTaskParameters.setOriginTableCreateDdl(originDdl);
        return oscScheduleTaskParameters;
    }

    private TriggerListener getTriggerListener(String triggerKeyName, CountDownLatch countDownLatch) {
        return new TriggerListenerSupport() {
            @Override
            public String getName() {
                return triggerKeyName;
            }

            @Override
            public void triggerComplete(Trigger trigger, JobExecutionContext context,
                    CompletedExecutionInstruction triggerInstructionCode) {
                countDownLatch.countDown();
            }
        };
    }

    private void createTableForMultiTask() {
        String createTemplate = "create table if not exists {0} (id int(20) primary key, name1 varchar(20))";
        jdbcTemplate.execute(MessageFormat.format(createTemplate, "t1"));
        jdbcTemplate.execute(MessageFormat.format(createTemplate, "t2"));
        jdbcTemplate.execute(MessageFormat.format(createTemplate, "t3"));
    }

    private void dropTableForMultiTask() {
        String dropTemplate = "drop table if exists {0}";
        jdbcTemplate.execute(MessageFormat.format(dropTemplate, "t1"));
        jdbcTemplate.execute(MessageFormat.format(dropTemplate, "t2"));
        jdbcTemplate.execute(MessageFormat.format(dropTemplate, "t3"));
    }

    private void createQuartzJob(ScheduleEntity schedule, JobType jobType) {
        JobDataMap jobDataMap = new JobDataMap(BeanMap.create(schedule));
        jobDataMap.put("CREATOR_ID", 1L);
        jobDataMap.put("FLOW_TASK_ID", 1L);
        TriggerConfig triggerConfig = new TriggerConfig();
        triggerConfig.setTriggerStrategy(TriggerStrategy.START_AT);
        triggerConfig.setStartAt(new Date(System.currentTimeMillis() - 365 * 24 * 60 * 60 * 1000L));
        CreateQuartzJobReq createQuartzJobReq = new CreateQuartzJobReq();
        createQuartzJobReq.setType(jobType);
        createQuartzJobReq.setJobDataMap(jobDataMap);
        createQuartzJobReq.setScheduleId(schedule.getId());
        createQuartzJobReq.setTriggerConfig(triggerConfig);
        createQuartzJobReq.setAllowConcurrent(false);

        try {
            quartzJobService.createJob(createQuartzJobReq);
        } catch (SchedulerException e) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "Create a quartz job check oms project occur error, jobParameters ={0}",
                    JsonUtils.toJson(createQuartzJobReq)), e);
        }
    }

    private Environment getEnvironment() {
        Environment environment = new Environment();
        environment.setId(1L);
        environment.setName("fake_env");
        return environment;
    }
}
