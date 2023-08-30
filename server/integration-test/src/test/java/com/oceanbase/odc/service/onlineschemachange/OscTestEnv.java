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
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Mockito;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
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
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeSqlType;
import com.oceanbase.odc.service.onlineschemachange.model.OriginTableCleanStrategy;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.ProjectStatusEnum;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.DataSourceOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.ProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.request.CreateOceanBaseDataSourceRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.CreateProjectRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ListProjectFullVerifyResultRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ProjectControlRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectFullVerifyResultResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectProgressResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectStepVO;
import com.oceanbase.odc.service.quartz.model.MisfireStrategy;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;
import com.oceanbase.odc.service.schedule.model.TriggerStrategy;
import com.oceanbase.odc.service.session.DBSessionManageFacade;

/**
 * @author yaobin
 * @date 2023-07-17
 * @since 4.2.0
 */
public class OscTestEnv extends ServiceTestEnv {

    @Autowired
    protected ScheduleRepository scheduleRepository;

    @Autowired
    protected ScheduleTaskRepository scheduleTaskRepository;

    @Autowired
    protected OnlineSchemaChangeTaskHandler onlineSchemaChangeTaskHandler;
    @MockBean
    protected ConnectionService connectionService;

    @MockBean
    protected ProjectOpenApiService projectOpenApiService;
    @MockBean
    protected DataSourceOpenApiService dataSourceOpenApiService;

    @MockBean
    protected DBSessionManageFacade dbSessionManager;

    @Autowired
    protected Scheduler scheduler;

    protected static ConnectionConfig config;
    protected static ConnectionSession connectionSession;
    protected static SyncJdbcExecutor jdbcTemplate;
    protected String oscCheckTaskCronExpression = "0/3 * * * * ?";

    @BeforeClass
    public static void before() {
        config = TestConnectionUtil.getTestConnectionConfig(ConnectType.OB_MYSQL);
        connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        jdbcTemplate = connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
    }

    @Before
    public void beforeEveryTestCase() {
        mock();
    }

    private void mock() {
        String datasourceId = StringUtils.uuidNoHyphen();
        String projectId = StringUtils.uuidNoHyphen();

        when(dataSourceOpenApiService.createOceanBaseDataSource(Mockito.any(CreateOceanBaseDataSourceRequest.class)))
                .thenReturn(datasourceId);

        when(projectOpenApiService.createProject(Mockito.any(CreateProjectRequest.class)))
                .thenReturn(projectId);

        doNothing().when(projectOpenApiService).startProject(Mockito.any(ProjectControlRequest.class));
        ProjectStepResultTest projectStepResultTest = new ProjectStepResultTest();
        List<ProjectStepVO> projectSteps = projectStepResultTest.projectSteps();

        ProjectProgressResponse projectProgressResponse = new ProjectProgressResponse();
        projectProgressResponse.setStatus(ProjectStatusEnum.DELETED);

        doReturn(projectProgressResponse).when(projectOpenApiService)
                .describeProjectProgress(Mockito.any(ProjectControlRequest.class));

        ProjectFullVerifyResultResponse fullVerifyResultResponse = projectStepResultTest.verifyResult();
        doReturn(projectSteps).when(projectOpenApiService)
                .describeProjectSteps(Mockito.any(ProjectControlRequest.class));


        doReturn(fullVerifyResultResponse).when(projectOpenApiService)
                .listProjectFullVerifyResult(Mockito.any(ListProjectFullVerifyResultRequest.class));

        doNothing().when(dbSessionManager)
                .killAllSessions(Mockito.any(ConnectionSession.class), Mockito.any(Predicate.class), Mockito.anyInt());

        when(connectionService.getForConnectionSkipPermissionCheck(eq(config.getId()))).thenReturn(config);

    }


    protected ScheduleTaskEntity getScheduleTaskEntity(Long scheduleId,
            OnlineSchemaChangeScheduleTaskParameters oscScheduleTaskParameters) {
        ScheduleTaskEntity scheduleTaskEntity = new ScheduleTaskEntity();
        scheduleTaskEntity.setParametersJson(JsonUtils.toJson(oscScheduleTaskParameters));
        scheduleTaskEntity.setStatus(TaskStatus.PREPARING);
        scheduleTaskEntity.setJobName(scheduleId + "");
        scheduleTaskEntity.setJobGroup(ONLINE_SCHEMA_CHANGE_COMPLETE.name());
        scheduleTaskEntity.setProgressPercentage(0.0);
        scheduleTaskEntity.setResultJson("");
        scheduleTaskEntity.setFireTime(new Date());

        return scheduleTaskRepository.save(scheduleTaskEntity);
    }

    protected ScheduleEntity getScheduleEntity(ConnectionConfig config, OnlineSchemaChangeParameters changeParameters) {
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
        triggerConfig.setTriggerStrategy(TriggerStrategy.CRON);
        triggerConfig.setCronExpression(oscCheckTaskCronExpression);
        scheduleEntity.setTriggerConfigJson(JsonUtils.toJson(triggerConfig));
        return scheduleRepository.saveAndFlush(scheduleEntity);
    }

    protected void createTableForTask(String tableName) {
        String createTemplate = "create table if not exists {0} (id int(20) primary key, name1 varchar(20))";
        jdbcTemplate.execute(MessageFormat.format(createTemplate, tableName));
    }

    protected void dropTableForTask(String tableName) {
        String dropTemplate = "drop table if exists {0}";
        jdbcTemplate.execute(MessageFormat.format(dropTemplate, tableName));
    }

    protected void createTableForMultiTask() {
        createTableForTask("t1");
        createTableForTask("t2");
    }

    protected void dropTableForMultiTask() {
        dropTableForTask("t1");
        dropTableForTask("t2");
    }

    protected OnlineSchemaChangeParameters getOnlineSchemaChangeParameters() {
        OnlineSchemaChangeParameters changeParameters = new OnlineSchemaChangeParameters();
        changeParameters.setSwapTableNameRetryTimes(3);
        changeParameters.setSqlType(OnlineSchemaChangeSqlType.CREATE);
        changeParameters.setErrorStrategy(TaskErrorStrategy.ABORT);
        changeParameters.setOriginTableCleanStrategy(OriginTableCleanStrategy.ORIGIN_TABLE_RENAME_AND_RESERVED);
        changeParameters.setSqlContent("create table t1 (id int(20) primary key, name1 varchar(20));"
                + "create table t2 (id int(20) primary key, name1 varchar(20));");
        return changeParameters;
    }


}
