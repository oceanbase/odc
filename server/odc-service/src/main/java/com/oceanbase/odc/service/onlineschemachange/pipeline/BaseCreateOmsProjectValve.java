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
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.ddl.DdlConstants;
import com.oceanbase.odc.service.onlineschemachange.exception.OmsException;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsOceanBaseType;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.DataSourceOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.ProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.request.CommonTransferConfig;
import com.oceanbase.odc.service.onlineschemachange.oms.request.CreateOceanBaseDataSourceRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.CreateProjectRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.DatabaseTransferObject;
import com.oceanbase.odc.service.onlineschemachange.oms.request.FullTransferConfig;
import com.oceanbase.odc.service.onlineschemachange.oms.request.IncrTransferConfig;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ProjectControlRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.SpecificTransferMapping;
import com.oceanbase.odc.service.onlineschemachange.oms.request.TableTransferObject;
import com.oceanbase.odc.service.quartz.QuartzJobService;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.schedule.model.QuartzKeyGenerator;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-06-11
 * @since 4.2.0
 */
@Slf4j
public abstract class BaseCreateOmsProjectValve extends BaseValve {

    @Autowired
    protected DataSourceOpenApiService dataSourceOpenApiService;
    @Autowired
    private ProjectOpenApiService projectOpenApiService;

    @Autowired
    private QuartzJobService quartzJobService;

    @Autowired
    private ScheduleTaskRepository scheduleTaskRepository;

    @Autowired
    protected OnlineSchemaChangeProperties oscProperties;

    @Autowired
    private ScheduleService scheduleService;

    @Override
    public void invoke(ValveContext valveContext) {
        OscValveContext context = (OscValveContext) valveContext;
        Long taskId = context.getScheduleTask().getId();
        log.info("Start execute {}, schedule task id {}", getClass().getSimpleName(), taskId);

        CreateOceanBaseDataSourceRequest dataSourceRequest =
                getCreateDataSourceRequest(context.getConnectionConfig(), context.getConnectionSession(),
                        context.getTaskParameter());
        String omsDsId = null;
        try {
            omsDsId = dataSourceOpenApiService.createOceanBaseDataSource(dataSourceRequest);
        } catch (OmsException ex) {
            log.warn("Create database occur error {}", ex.getMessage());
            omsDsId = reCreateDataSourceRequestAfterThrowsException(context.getTaskParameter(), dataSourceRequest, ex);
        }

        PreConditions.notNull(omsDsId, "Oms datasource id");

        CreateProjectRequest createProjectRequest = getCreateProjectRequest(omsDsId,
                context.getSchedule().getId(), context.getTaskParameter());

        String projectId = projectOpenApiService.createProject(createProjectRequest);

        OnlineSchemaChangeScheduleTaskParameters scheduleTaskParameters = context.getTaskParameter();
        scheduleTaskParameters.setOmsProjectId(projectId);
        scheduleTaskParameters.setOmsDataSourceId(omsDsId);

        scheduleTaskRepository.updateTaskParameters(taskId, JsonUtils.toJson(scheduleTaskParameters));

        ProjectControlRequest request = new ProjectControlRequest();
        request.setId(projectId);
        request.setUid(scheduleTaskParameters.getUid());
        try {
            projectOpenApiService.startProject(request);
        } finally {
            scheduleCheckOmsProject(context.getSchedule(), taskId);
        }
    }

    private void scheduleCheckOmsProject(ScheduleEntity scheduleEntity, Long scheduleTaskId) {
        Long scheduleId = scheduleEntity.getId();
        JobKey jobKey = QuartzKeyGenerator.generateJobKey(scheduleId, JobType.ONLINE_SCHEMA_CHANGE_COMPLETE);
        Map<String, Object> triggerData = getStringObjectMap(scheduleTaskId);
        try {
            if (quartzJobService.checkExists(jobKey)) {
                scheduleService.innerUpdateTriggerData(scheduleId, triggerData);
                return;
            }
        } catch (SchedulerException e) {
            throw new IllegalArgumentException(e);
        }

        try {
            scheduleService.innerEnable(scheduleId, triggerData);
            log.info("Start check oms project status by quartz job, jobParameters={}",
                    JsonUtils.toJson(scheduleEntity.getJobParametersJson()));
        } catch (SchedulerException e) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "Create a quartz job check oms project occur error, jobParameters ={0}",
                    JsonUtils.toJson(scheduleEntity.getJobParametersJson())), e);
        }
    }

    private Map<String, Object> getStringObjectMap(Long scheduleTaskId) {
        Map<String, Object> dataMap = new HashMap<>(2);
        dataMap.put(OdcConstants.SCHEDULE_TASK_ID, scheduleTaskId);
        dataMap.put(DdlConstants.MDC_CONTEXT, JsonUtils.toJson(MDC.getCopyOfContextMap()));
        return dataMap;
    }

    private CreateProjectRequest getCreateProjectRequest(String omsDsId, Long scheduleId,
            OnlineSchemaChangeScheduleTaskParameters oscScheduleTaskParameters) {
        CreateProjectRequest request = new CreateProjectRequest();
        doCreateProjectRequest(omsDsId, scheduleId, oscScheduleTaskParameters, request);
        if (oscProperties.isEnableFullVerify()) {
            request.setEnableFullVerify(oscProperties.isEnableFullVerify());
        }

        request.setSourceEndpointId(omsDsId);
        request.setSinkEndpointId(omsDsId);

        List<DatabaseTransferObject> databaseTransferObjects = new ArrayList<>();
        DatabaseTransferObject databaseTransferObject = new DatabaseTransferObject();
        databaseTransferObject.setName(oscScheduleTaskParameters.getDatabaseName());
        databaseTransferObject.setMappedName(oscScheduleTaskParameters.getDatabaseName());
        databaseTransferObjects.add(databaseTransferObject);

        List<TableTransferObject> tables = new ArrayList<>();
        TableTransferObject tableTransferObject = new TableTransferObject();
        tableTransferObject.setName(oscScheduleTaskParameters.getOriginTableNameUnwrapped());
        tableTransferObject.setMappedName(oscScheduleTaskParameters.getNewTableNameUnwrapped());
        tables.add(tableTransferObject);
        databaseTransferObject.setTables(tables);

        SpecificTransferMapping transferMapping = new SpecificTransferMapping();
        transferMapping.setDatabases(databaseTransferObjects);
        request.setTransferMapping(transferMapping);

        CommonTransferConfig commonTransferConfig = new CommonTransferConfig();
        request.setCommonTransferConfig(commonTransferConfig);
        FullTransferConfig fullTransferConfig = new FullTransferConfig();
        request.setFullTransferConfig(fullTransferConfig);
        IncrTransferConfig incrTransferConfig = new IncrTransferConfig();
        request.setIncrTransferConfig(incrTransferConfig);
        request.setUid(oscScheduleTaskParameters.getUid());
        return request;
    }

    private CreateOceanBaseDataSourceRequest getCreateDataSourceRequest(ConnectionConfig config,
            ConnectionSession connectionSession, OnlineSchemaChangeScheduleTaskParameters oscScheduleTaskParameters) {

        CreateOceanBaseDataSourceRequest request = new CreateOceanBaseDataSourceRequest();
        request.setName(UUID.randomUUID().toString().replace("-", ""));
        request.setType(OmsOceanBaseType.from(config.getType()).name());
        request.setTenant(config.getTenantName());
        request.setCluster(config.getClusterName());
        request.setUserName(config.getUsername());
        if (config.getPassword() != null) {
            request.setPassword(Base64.getEncoder().encodeToString(config.getPassword().getBytes()));
        }
        doCreateDataSourceRequest(config, connectionSession, oscScheduleTaskParameters, request);
        return request;
    }

    protected abstract void doCreateDataSourceRequest(ConnectionConfig config,
            ConnectionSession connectionSession, OnlineSchemaChangeScheduleTaskParameters oscScheduleTaskParameters,
            CreateOceanBaseDataSourceRequest request);

    protected abstract String reCreateDataSourceRequestAfterThrowsException(
            OnlineSchemaChangeScheduleTaskParameters oscScheduleTaskParameters,
            CreateOceanBaseDataSourceRequest request, OmsException ex);

    protected abstract void doCreateProjectRequest(String omsDsId, Long scheduleId,
            OnlineSchemaChangeScheduleTaskParameters oscScheduleTaskParameters, CreateProjectRequest request);
}
