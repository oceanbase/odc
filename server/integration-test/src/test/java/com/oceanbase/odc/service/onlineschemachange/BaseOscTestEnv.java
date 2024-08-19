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

import static com.oceanbase.odc.service.schedule.model.ScheduleType.ONLINE_SCHEMA_CHANGE_COMPLETE;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.Mockito;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties.OmsProperties;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskResult;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsProjectStatusEnum;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.DataSourceOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.OmsProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.request.CreateOceanBaseDataSourceRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.CreateOmsProjectRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ListOmsProjectFullVerifyResultRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.OmsProjectControlRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectFullVerifyResultResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectProgressResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectStepVO;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.oms.OmsSwapTableAction;
import com.oceanbase.odc.service.quartz.model.MisfireStrategy;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;
import com.oceanbase.odc.service.schedule.model.ScheduleType;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;
import com.oceanbase.odc.service.schedule.model.TriggerStrategy;
import com.oceanbase.odc.service.session.DBSessionManageFacade;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

/**
 * @author yaobin
 * @date 2023-07-17
 * @since 4.2.0
 */
public abstract class BaseOscTestEnv extends ServiceTestEnv {

    @Autowired
    protected ScheduleRepository scheduleRepository;

    @Autowired
    protected ScheduleTaskRepository scheduleTaskRepository;

    @Autowired
    protected OnlineSchemaChangeTaskHandler onlineSchemaChangeTaskHandler;
    @MockBean
    protected ConnectionService connectionService;

    @MockBean
    protected OmsProjectOpenApiService projectOpenApiService;
    @MockBean
    protected DataSourceOpenApiService dataSourceOpenApiService;

    @MockBean
    protected DBSessionManageFacade dbSessionManager;

    @Qualifier(value = "defaultScheduler")
    @Autowired
    protected Scheduler scheduler;

    protected ConnectionConfig config;
    protected ConnectionSession connectionSession;
    protected SyncJdbcExecutor jdbcTemplate;
    protected String oscCheckTaskCronExpression = "0/3 * * * * ?";
    protected OnlineSchemaChangeProperties onlineSchemaChangeProperties;
    protected OmsSwapTableAction swapTableNameValve;

    @Before
    public void beforeEveryTestCase() {
        ConnectType connectType = ConnectType.from(getDialectType());
        config = TestConnectionUtil.getTestConnectionConfig(connectType);
        connectionSession = TestConnectionUtil.getTestConnectionSession(connectType);
        jdbcTemplate = connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
        this.onlineSchemaChangeProperties = new OnlineSchemaChangeProperties();
        onlineSchemaChangeProperties.setEnableFullVerify(false);
        OmsProperties omsProperties = new OmsProperties();
        omsProperties.setUrl("127.0.0.1:8089");
        omsProperties.setRegion("default");
        omsProperties.setAuthorization("auth");
        onlineSchemaChangeProperties.setOms(omsProperties);
        swapTableNameValve =
                new OmsSwapTableAction(dbSessionManager, projectOpenApiService, onlineSchemaChangeProperties);
        mock();
    }

    private void mock() {
        String datasourceId = StringUtils.uuidNoHyphen();
        String projectId = StringUtils.uuidNoHyphen();

        when(dataSourceOpenApiService.createOceanBaseDataSource(Mockito.any(CreateOceanBaseDataSourceRequest.class)))
                .thenReturn(datasourceId);

        when(projectOpenApiService.createProject(Mockito.any(CreateOmsProjectRequest.class)))
                .thenReturn(projectId);

        doNothing().when(projectOpenApiService).startProject(Mockito.any(OmsProjectControlRequest.class));
        ProjectStepResultTest projectStepResultTest = new ProjectStepResultTest();
        List<OmsProjectStepVO> projectSteps = projectStepResultTest.projectSteps();

        OmsProjectProgressResponse projectProgressResponse = new OmsProjectProgressResponse();
        projectProgressResponse.setStatus(OmsProjectStatusEnum.DELETED);
        projectProgressResponse.setIncrSyncCheckpoint(System.currentTimeMillis() / 1000 + 1000);
        doReturn(projectProgressResponse).when(projectOpenApiService)
                .describeProjectProgress(Mockito.any(OmsProjectControlRequest.class));

        OmsProjectFullVerifyResultResponse fullVerifyResultResponse = projectStepResultTest.verifyResult();
        doReturn(projectSteps).when(projectOpenApiService)
                .describeProjectSteps(Mockito.any(OmsProjectControlRequest.class));


        doReturn(fullVerifyResultResponse).when(projectOpenApiService)
                .listProjectFullVerifyResult(Mockito.any(ListOmsProjectFullVerifyResultRequest.class));

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
        OnlineSchemaChangeScheduleTaskResult result = new OnlineSchemaChangeScheduleTaskResult();
        scheduleTaskEntity.setResultJson(JsonUtils.toJson(result));
        return scheduleTaskRepository.save(scheduleTaskEntity);
    }

    protected ScheduleEntity getScheduleEntity(ConnectionConfig config, OnlineSchemaChangeParameters changeParameters) {
        ScheduleEntity scheduleEntity = new ScheduleEntity();
        scheduleEntity.setStatus(ScheduleStatus.ENABLED);
        scheduleEntity.setDataSourceId(config.getId());
        scheduleEntity.setDatabaseName(config.getDefaultSchema());
        scheduleEntity.setDatabaseId(1L);
        scheduleEntity.setProjectId(1L);
        scheduleEntity.setOrganizationId(1L);
        scheduleEntity.setCreatorId(1L);
        scheduleEntity.setModifierId(1L);
        scheduleEntity.setStatus(ScheduleStatus.ENABLED);
        scheduleEntity.setAllowConcurrent(false);
        scheduleEntity.setMisfireStrategy(MisfireStrategy.MISFIRE_INSTRUCTION_DO_NOTHING);
        scheduleEntity.setType(ScheduleType.ONLINE_SCHEMA_CHANGE_COMPLETE);
        scheduleEntity.setJobParametersJson(JsonUtils.toJson(changeParameters));
        TriggerConfig triggerConfig = new TriggerConfig();
        triggerConfig.setTriggerStrategy(TriggerStrategy.CRON);
        triggerConfig.setCronExpression(oscCheckTaskCronExpression);
        scheduleEntity.setTriggerConfigJson(JsonUtils.toJson(triggerConfig));
        return scheduleRepository.saveAndFlush(scheduleEntity);
    }


    protected void checkSwapTableAndRenameReserved(OnlineSchemaChangeScheduleTaskParameters taskParameters) {
        DBSchemaAccessor dbSchemaAccessor = DBSchemaAccessors.create(connectionSession);
        List<String> renamedTable = dbSchemaAccessor.showTablesLike(taskParameters.getDatabaseName(),
                taskParameters.getRenamedTableNameUnwrapped());

        List<String> originTable = dbSchemaAccessor.showTablesLike(taskParameters.getDatabaseName(),
                taskParameters.getOriginTableNameUnwrapped());

        Assert.assertFalse(CollectionUtils.isEmpty(renamedTable));
        Assert.assertFalse(CollectionUtils.isEmpty(originTable));

        // if swap table successful
        List<DBTableColumn> tableColumnFromNew = dbSchemaAccessor.listTableColumns(taskParameters.getDatabaseName(),
                taskParameters.getOriginTableNameUnwrapped());

        Optional<DBTableColumn> name1Col = tableColumnFromNew.stream()
                .filter(a -> a.getName().equalsIgnoreCase("name1"))
                .findFirst();
        Assert.assertTrue(name1Col.isPresent());
        Assert.assertEquals(30L, name1Col.get().getMaxLength().longValue());


        List<DBTableColumn> renamedTableColumns = dbSchemaAccessor.listTableColumns(taskParameters.getDatabaseName(),
                taskParameters.getRenamedTableNameUnwrapped());

        Optional<DBTableColumn> name2Col = renamedTableColumns.stream()
                .filter(a -> a.getName().equalsIgnoreCase("name1"))
                .findFirst();
        Assert.assertTrue(name2Col.isPresent());
        Assert.assertEquals(20L, name2Col.get().getMaxLength().longValue());

    }

    protected void checkSwapTableAndRenameDrop(OnlineSchemaChangeScheduleTaskParameters taskParameters) {
        DBSchemaAccessor dbSchemaAccessor = DBSchemaAccessors.create(connectionSession);
        List<String> renamedTable = dbSchemaAccessor.showTablesLike(taskParameters.getDatabaseName(),
                taskParameters.getRenamedTableNameUnwrapped());

        List<String> originTable = dbSchemaAccessor.showTablesLike(taskParameters.getDatabaseName(),
                taskParameters.getOriginTableNameUnwrapped());

        Assert.assertTrue(CollectionUtils.isEmpty(renamedTable));
        Assert.assertFalse(CollectionUtils.isEmpty(originTable));
    }

    protected abstract DialectType getDialectType();

}
