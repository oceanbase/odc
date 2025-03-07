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

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.exception.OmsException;
import com.oceanbase.odc.service.onlineschemachange.fsm.Action;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsProjectStatusEnum;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.OmsProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.request.FullTransferConfig;
import com.oceanbase.odc.service.onlineschemachange.oms.request.IncrTransferConfig;
import com.oceanbase.odc.service.onlineschemachange.oms.request.OmsProjectControlRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.UpdateProjectConfigRequest;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionContext;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionResult;
import com.oceanbase.odc.service.onlineschemachange.oscfms.state.OscStates;

import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2024/7/9 10:33
 * @since 4.3.1
 */
@Slf4j
public class OmsModifyDataTaskAction implements Action<OscActionContext, OscActionResult> {
    private final OmsProjectOpenApiService projectOpenApiService;

    private final OnlineSchemaChangeProperties onlineSchemaChangeProperties;

    public OmsModifyDataTaskAction(@NotNull OmsProjectOpenApiService projectOpenApiService,
            @NotNull OnlineSchemaChangeProperties onlineSchemaChangeProperties) {
        this.projectOpenApiService = projectOpenApiService;
        this.onlineSchemaChangeProperties = onlineSchemaChangeProperties;
    }

    public OscActionResult execute(OscActionContext context) throws Exception {
        ScheduleTaskEntity scheduleTask = context.getScheduleTask();
        log.debug("Start execute {}, schedule task id={}", getClass().getSimpleName(), scheduleTask.getId());

        OnlineSchemaChangeScheduleTaskParameters taskParameter = context.getTaskParameter();
        OnlineSchemaChangeParameters inputParameters = context.getParameter();
        OmsProjectStatusEnum projectStatusEnum = OmsRequestUtil.getProjectProjectStatus(taskParameter.getUid(),
                taskParameter.getOmsProjectId(),
                projectOpenApiService);
        // If config update is in processing, wait until the process done
        // This behavior may cause process blocked if OMS update failed or resume failed
        return updateOmsProjectConfig(scheduleTask.getId(), taskParameter, inputParameters, projectStatusEnum, context);
    }

    private OscActionResult updateOmsProjectConfig(Long scheduleTaskId,
            OnlineSchemaChangeScheduleTaskParameters taskParameters,
            OnlineSchemaChangeParameters inputParameters, OmsProjectStatusEnum omsProjectStatus,
            OscActionContext context) {
        // if rate limiter parameters is changed, try to stop and restart project
        if (Objects.equals(inputParameters.getRateLimitConfig(), taskParameters.getRateLimitConfig())) {
            log.info("Rate limiter not changed,rateLimiterConfig = {}, update oms project not required",
                    inputParameters.getRateLimitConfig());
            // swap to monitor state
            return new OscActionResult(OscStates.MODIFY_DATA_TASK.getState(), null,
                    OscStates.MONITOR_DATA_TASK.getState());
        }
        log.info("Input rate limiter has changed, currentOmsProjectStatus={}, rateLimiterConfig={}, "
                + "oldRateLimiterConfig={}.",
                omsProjectStatus.name(), JsonUtils.toJson(taskParameters.getRateLimitConfig()),
                JsonUtils.toJson(inputParameters.getRateLimitConfig()));

        if (omsProjectStatus == OmsProjectStatusEnum.RUNNING) {
            OmsProjectControlRequest controlRequest = new OmsProjectControlRequest();
            controlRequest.setId(taskParameters.getOmsProjectId());
            controlRequest.setUid(taskParameters.getUid());
            log.info("Try to stop oms project, omsProjectId={}, scheduleTaskId={}.",
                    taskParameters.getOmsProjectId(), scheduleTaskId);
            try {
                projectOpenApiService.stopProject(controlRequest);
                log.info("Stop oms project completed, omsProjectId={}, scheduleTaskId={}.",
                        taskParameters.getOmsProjectId(), scheduleTaskId);
            } catch (Exception e) {
                log.warn("Stop oms project failed, omsProjectId={}, scheduleTaskId={}.",
                        taskParameters.getOmsProjectId(), scheduleTaskId, e);
            }
            // keep in same state
            return new OscActionResult(OscStates.MODIFY_DATA_TASK.getState(), null,
                    OscStates.MODIFY_DATA_TASK.getState());
        } else if (omsProjectStatus == OmsProjectStatusEnum.SUSPEND) {
            try {
                doUpdateOmsProjectConfig(scheduleTaskId, taskParameters, inputParameters, context);
            } catch (Exception e) {
                log.warn("Update oms project config failed, omsProjectId={}, scheduleTaskId={}.",
                        taskParameters.getOmsProjectId(), scheduleTaskId, e);
            }
            // jump to monitor state
            return new OscActionResult(OscStates.MODIFY_DATA_TASK.getState(), null,
                    OscStates.MONITOR_DATA_TASK.getState());
        } else {
            log.info("Undetermined oms project status, omsProjectId={}, scheduleTaskId={}.",
                    taskParameters.getOmsProjectId(), scheduleTaskId);
            // jump to monitor state to monitor undetermined state
            return new OscActionResult(OscStates.MODIFY_DATA_TASK.getState(), null,
                    OscStates.MONITOR_DATA_TASK.getState());
        }
    }

    private void doUpdateOmsProjectConfig(Long scheduleTaskId, OnlineSchemaChangeScheduleTaskParameters taskParameters,
            OnlineSchemaChangeParameters oscParameters, OscActionContext context) {
        FullTransferConfig fullTransferConfig = new FullTransferConfig();
        IncrTransferConfig incrTransferConfig = new IncrTransferConfig();
        fullTransferConfig.setThrottleIOPS(oscParameters.getRateLimitConfig().getDataSizeLimit());
        incrTransferConfig.setThrottleIOPS(oscParameters.getRateLimitConfig().getDataSizeLimit());
        fullTransferConfig.setThrottleRps(oscParameters.getRateLimitConfig().getRowLimit());
        incrTransferConfig.setThrottleRps(oscParameters.getRateLimitConfig().getRowLimit());
        UpdateProjectConfigRequest request = new UpdateProjectConfigRequest();
        request.setFullTransferConfig(fullTransferConfig);
        request.setIncrTransferConfig(incrTransferConfig);
        request.setUid(taskParameters.getUid());
        request.setId(taskParameters.getOmsProjectId());

        log.info("Try to update oms project, omsProjectId={}, scheduleTaskId={},"
                + " request={}.", taskParameters.getOmsProjectId(), scheduleTaskId, JsonUtils.toJson(request));
        try {
            projectOpenApiService.updateProjectConfig(request);
        } catch (OmsException omsException) {
            throwOrIgnore(omsException);
        }

        log.info("Update oms project completed, Try to resume project, omsProjectId={},"
                + " scheduleTaskId={}", taskParameters.getOmsProjectId(), scheduleTaskId);
        OmsProjectControlRequest controlRequest = new OmsProjectControlRequest();
        controlRequest.setId(taskParameters.getOmsProjectId());
        controlRequest.setUid(taskParameters.getUid());
        projectOpenApiService.resumeProject(controlRequest);
        log.info("Resume oms project completed, omsProjectId={}, scheduleTaskId={}",
                taskParameters.getOmsProjectId(), scheduleTaskId);
        // update task parameters rate limit same as schedule
        taskParameters.setRateLimitConfig(oscParameters.getRateLimitConfig());
        int rows = context.getScheduleTaskRepository().updateTaskParameters(scheduleTaskId,
                JsonUtils.toJson(taskParameters));
        if (rows > 0) {
            log.info("Update throttle completed, scheduleTaskId={}", scheduleTaskId);
        }
    }

    /**
     * Lower version OMS may not support updateConfig api Ignore this exception and continue resume
     * migrate project
     *
     * @param omsException
     */
    private void throwOrIgnore(OmsException omsException) {
        if (!StringUtils.containsIgnoreCase(omsException.getMessage(),
                "Unsupported action: UpdateProjectConfig")) {
            throw omsException;
        }
    }

}
