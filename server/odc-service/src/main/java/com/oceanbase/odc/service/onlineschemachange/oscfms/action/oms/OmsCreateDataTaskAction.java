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
package com.oceanbase.odc.service.onlineschemachange.oscfms.action.oms;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.exception.OmsException;
import com.oceanbase.odc.service.onlineschemachange.fsm.Action;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.DataSourceOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.OmsProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.request.CommonTransferConfig;
import com.oceanbase.odc.service.onlineschemachange.oms.request.CreateOceanBaseDataSourceRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.CreateOmsProjectRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.DatabaseTransferObject;
import com.oceanbase.odc.service.onlineschemachange.oms.request.FullTransferConfig;
import com.oceanbase.odc.service.onlineschemachange.oms.request.IncrTransferConfig;
import com.oceanbase.odc.service.onlineschemachange.oms.request.OmsProjectControlRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.SpecificTransferMapping;
import com.oceanbase.odc.service.onlineschemachange.oms.request.TableTransferObject;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionContext;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionResult;
import com.oceanbase.odc.service.onlineschemachange.oscfms.state.OscStates;

import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2024/7/8 19:00
 * @since 4.3.1
 */
@Slf4j
public class OmsCreateDataTaskAction implements Action<OscActionContext, OscActionResult> {
    protected final DataSourceOpenApiService dataSourceOpenApiService;

    protected final OmsProjectOpenApiService projectOpenApiService;

    protected final OnlineSchemaChangeProperties oscProperties;

    public OmsCreateDataTaskAction(@NotNull DataSourceOpenApiService dataSourceOpenApiService,
            @NotNull OmsProjectOpenApiService projectOpenApiService,
            @NotNull OnlineSchemaChangeProperties oscProperties) {
        this.dataSourceOpenApiService = dataSourceOpenApiService;
        this.projectOpenApiService = projectOpenApiService;
        this.oscProperties = oscProperties;
    }

    @Override
    public OscActionResult execute(OscActionContext context) throws Exception {
        Long taskId = context.getScheduleTask().getId();
        OnlineSchemaChangeScheduleTaskParameters scheduleTaskParameters = context.getTaskParameter();
        log.info("Start execute {}, schedule task id {}", getClass().getSimpleName(), taskId);
        // create oms data source id
        String omsDsId = tryCreateOMSDataSource(scheduleTaskParameters, context);
        // create oms project
        String projectId = tryCreateOMSDataProject(scheduleTaskParameters, omsDsId, context);

        //// update it immediately
        scheduleTaskParameters.setOmsProjectId(projectId);
        scheduleTaskParameters.setOmsDataSourceId(omsDsId);
        context.getScheduleTaskRepository().updateTaskParameters(taskId, JsonUtils.toJson(scheduleTaskParameters));

        // start project
        startOMSDataProject(projectId, scheduleTaskParameters.getUid());
        // transfer to next stage
        return new OscActionResult(OscStates.CREATE_DATA_TASK.getState(), null, OscStates.MONITOR_DATA_TASK.getState());
    }

    /**
     * create oms data source
     */
    private String tryCreateOMSDataSource(OnlineSchemaChangeScheduleTaskParameters scheduleTaskParameters,
            OscActionContext context) {
        String omsDsId = scheduleTaskParameters.getOmsDataSourceId();
        // not created datasource yet, try create it
        if (StringUtils.isEmpty(omsDsId)) {
            ConnectionConfig connectionConfig = context.getConnectionProvider().connectionConfig();
            ConnectionSession connectionSession = null;
            CreateOceanBaseDataSourceRequest dataSourceRequest = null;
            try {
                connectionSession = context.getConnectionProvider().createConnectionSession();
                dataSourceRequest = OmsRequestUtil.getCreateDataSourceRequest(connectionConfig, connectionSession,
                        context.getTaskParameter(), oscProperties);
                omsDsId = dataSourceOpenApiService.createOceanBaseDataSource(dataSourceRequest);
                log.info("OSC create oms data source, omsDSId {}, request {}", omsDsId, dataSourceRequest);
            } catch (OmsException ex) {
                log.warn("Create database occur error {}", ex.getMessage());
                omsDsId = reCreateDataSourceRequestAfterThrowsException(context.getTaskParameter(), dataSourceRequest,
                        ex);
            } finally {
                // release source
                if (null != connectionSession) {
                    connectionSession.expire();
                }
            }
        }
        PreConditions.notNull(omsDsId, "Oms datasource id");
        return omsDsId;
    }

    /**
     * create oms data project
     */
    private String tryCreateOMSDataProject(OnlineSchemaChangeScheduleTaskParameters scheduleTaskParameters,
            String omsDataSourceID, OscActionContext context) {
        String projectId = scheduleTaskParameters.getOmsProjectId();
        // not create projectID yet
        if (StringUtils.isEmpty(projectId)) {
            CreateOmsProjectRequest createProjectRequest = getCreateProjectRequest(omsDataSourceID,
                    context.getSchedule().getId(), context.getTaskParameter(), context.getParameter());
            projectId = projectOpenApiService.createProject(createProjectRequest);
            log.info("OSC create oms project, omsDSId {}, projectID {}, request {}", omsDataSourceID, projectId,
                    createProjectRequest);
        }
        PreConditions.notNull(projectId, "Oms project id");
        return projectId;
    }

    private void startOMSDataProject(String projectId, String uid) {
        OmsProjectControlRequest request = new OmsProjectControlRequest();
        request.setId(projectId);
        request.setUid(uid);
        projectOpenApiService.startProject(request);
    }

    protected CreateOmsProjectRequest getCreateProjectRequest(String omsDsId, Long scheduleId,
            OnlineSchemaChangeScheduleTaskParameters oscScheduleTaskParameters,
            OnlineSchemaChangeParameters oscParameters) {
        CreateOmsProjectRequest request = new CreateOmsProjectRequest();
        fillCreateProjectRequest(omsDsId, scheduleId, oscScheduleTaskParameters, request);
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
        // remove column founded
        if (CollectionUtils.isNotEmpty(oscScheduleTaskParameters.getFilterColumns())) {
            tableTransferObject.setFilterColumns(oscScheduleTaskParameters.getFilterColumns());
        }
        tables.add(tableTransferObject);
        databaseTransferObject.setTables(tables);

        SpecificTransferMapping transferMapping = new SpecificTransferMapping();
        transferMapping.setDatabases(databaseTransferObjects);
        request.setTransferMapping(transferMapping);

        CommonTransferConfig commonTransferConfig = new CommonTransferConfig();
        request.setCommonTransferConfig(commonTransferConfig);
        FullTransferConfig fullTransferConfig = new FullTransferConfig();
        IncrTransferConfig incrTransferConfig = new IncrTransferConfig();
        fullTransferConfig.setThrottleIOPS(oscParameters.getRateLimitConfig().getDataSizeLimit());
        incrTransferConfig.setThrottleIOPS(oscParameters.getRateLimitConfig().getDataSizeLimit());
        fullTransferConfig.setThrottleRps(oscParameters.getRateLimitConfig().getRowLimit());
        incrTransferConfig.setThrottleRps(oscParameters.getRateLimitConfig().getRowLimit());
        request.setFullTransferConfig(fullTransferConfig);
        request.setIncrTransferConfig(incrTransferConfig);
        request.setUid(oscScheduleTaskParameters.getUid());
        return request;
    }

    /**
     * fill remain variables if needed
     */
    protected void fillCreateProjectRequest(String omsDsId, Long scheduleId,
            OnlineSchemaChangeScheduleTaskParameters oscScheduleTaskParameters, CreateOmsProjectRequest request) {}

    protected String reCreateDataSourceRequestAfterThrowsException(
            OnlineSchemaChangeScheduleTaskParameters oscScheduleTaskParameters,
            CreateOceanBaseDataSourceRequest request, OmsException ex) {
        return null;
    }

}
