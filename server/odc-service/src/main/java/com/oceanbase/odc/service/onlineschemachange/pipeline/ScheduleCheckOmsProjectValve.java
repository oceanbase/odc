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

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.tableformat.Table;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.exception.OscException;
import com.oceanbase.odc.service.onlineschemachange.logger.DefaultTableFactory;
import com.oceanbase.odc.service.onlineschemachange.model.FullVerificationResult;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskResult;
import com.oceanbase.odc.service.onlineschemachange.model.PrecheckResult;
import com.oceanbase.odc.service.onlineschemachange.model.SwapTableType;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsProjectStatusEnum;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsStepName;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.OmsProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.request.FullTransferConfig;
import com.oceanbase.odc.service.onlineschemachange.oms.request.IncrTransferConfig;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ListOmsProjectFullVerifyResultRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.OmsProjectControlRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.UpdateProjectConfigRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectFullVerifyResultResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectProgressResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectStepVO;
import com.oceanbase.odc.service.onlineschemachange.pipeline.ProjectStepResultChecker.ProjectStepResult;
import com.oceanbase.odc.service.onlineschemachange.rename.SwapTableUtil;
import com.oceanbase.odc.service.onlineschemachange.subtask.OscTaskCompleteHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-06-10
 * @since 4.2.0
 */
@Slf4j
@Component
public class ScheduleCheckOmsProjectValve extends BaseValve {
    @Autowired
    private OmsProjectOpenApiService projectOpenApiService;
    @Autowired
    private OscTaskCompleteHandler completeHandler;
    @Autowired
    private ScheduleTaskRepository scheduleTaskRepository;

    @Autowired
    private OnlineSchemaChangeProperties onlineSchemaChangeProperties;

    @Override
    public void invoke(ValveContext valveContext) {
        OscValveContext context = (OscValveContext) valveContext;
        ScheduleTaskEntity scheduleTask = context.getScheduleTask();
        log.debug("Start execute {}, schedule task id {}", getClass().getSimpleName(), scheduleTask.getId());

        OnlineSchemaChangeScheduleTaskParameters taskParameter = context.getTaskParameter();
        OnlineSchemaChangeParameters inputParameters = context.getParameter();

        OmsProjectControlRequest projectRequest = getProjectRequest(taskParameter);
        List<OmsProjectStepVO> projectSteps = projectOpenApiService.describeProjectSteps(projectRequest);
        if (log.isDebugEnabled()) {
            log.debug("Get project step list from projectOpenApiService is {} ", JsonUtils.toJson(projectSteps));
        }
        OnlineSchemaChangeScheduleTaskResult lastResult = JsonUtils.fromJson(scheduleTask.getResultJson(),
                OnlineSchemaChangeScheduleTaskResult.class);

        OmsProjectProgressResponse progress = projectOpenApiService.describeProjectProgress(projectRequest);

        ProjectStepResult projectStepResult = new ProjectStepResultChecker(progress, projectSteps,
                onlineSchemaChangeProperties.isEnableFullVerify(),
                onlineSchemaChangeProperties.getOms().getCheckProjectStepFailedTimeoutSeconds(),
                lastResult.getCheckFailedTime())
                        .withCheckerVerifyResult(() -> listProjectFullVerifyResult(taskParameter.getOmsProjectId(),
                                taskParameter.getDatabaseName(), taskParameter.getUid()))
                        .withResumeProject(() -> {
                            projectOpenApiService.resumeProject(projectRequest);
                            return null;
                        })
                        .getCheckerResult();
        updateOmsProjectConfig(scheduleTask.getId(), taskParameter, inputParameters, progress.getStatus());

        OnlineSchemaChangeScheduleTaskResult result = new OnlineSchemaChangeScheduleTaskResult(taskParameter);

        if (lastResult != null) {
            result.setManualSwapTableEnabled(lastResult.isManualSwapTableEnabled());
            result.setManualSwapTableStarted(lastResult.isManualSwapTableStarted());
        }
        adaptResult(result, projectStepResult);
        scheduleTask.setResultJson(JsonUtils.toJson(result));
        scheduleTask.setProgressPercentage(projectStepResult.getTaskPercentage());
        scheduleTaskRepository.update(scheduleTask);
        recordCurrentProgress(taskParameter.getOmsProjectId(), result);
        handleOmsProjectStepResult(valveContext, projectStepResult, result,
                context.getParameter().getSwapTableType(), scheduleTask);
    }

    private void updateOmsProjectConfig(Long scheduleTaskId, OnlineSchemaChangeScheduleTaskParameters taskParameters,
            OnlineSchemaChangeParameters inputParameters, OmsProjectStatusEnum omsProjectStatus) {
        // if rate limiter parameters is changed, try to stop and restart project
        if (Objects.equals(inputParameters.getRateLimitConfig(), taskParameters.getRateLimitConfig())) {
            return;
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

        } else if (omsProjectStatus == OmsProjectStatusEnum.SUSPEND) {
            try {
                doUpdateOmsProjectConfig(scheduleTaskId, taskParameters, inputParameters);
            } catch (Exception e) {
                log.warn("Update oms project config failed, omsProjectId={}, scheduleTaskId={}.",
                        taskParameters.getOmsProjectId(), scheduleTaskId, e);
            }
        }
    }

    private void doUpdateOmsProjectConfig(Long scheduleTaskId, OnlineSchemaChangeScheduleTaskParameters taskParameters,
            OnlineSchemaChangeParameters oscParameters) {
        OmsProjectControlRequest controlRequest = new OmsProjectControlRequest();
        controlRequest.setId(taskParameters.getOmsProjectId());
        controlRequest.setUid(taskParameters.getUid());
        UpdateProjectConfigRequest request = new UpdateProjectConfigRequest();
        request.setId(taskParameters.getOmsProjectId());
        FullTransferConfig fullTransferConfig = new FullTransferConfig();
        IncrTransferConfig incrTransferConfig = new IncrTransferConfig();
        fullTransferConfig.setThrottleIOPS(oscParameters.getRateLimitConfig().getDataSizeLimit());
        incrTransferConfig.setThrottleIOPS(oscParameters.getRateLimitConfig().getDataSizeLimit());
        fullTransferConfig.setThrottleRps(oscParameters.getRateLimitConfig().getRowLimit());
        incrTransferConfig.setThrottleRps(oscParameters.getRateLimitConfig().getRowLimit());
        request.setFullTransferConfig(fullTransferConfig);
        request.setIncrTransferConfig(incrTransferConfig);

        log.info("Try to update oms project, omsProjectId={}, scheduleTaskId={},"
                + " request={}.", taskParameters.getOmsProjectId(), scheduleTaskId, JsonUtils.toJson(request));
        projectOpenApiService.updateProjectConfig(request);
        log.info("Update oms project completed, Try to resume project, omsProjectId={},"
                + " scheduleTaskId={}", taskParameters.getOmsProjectId(), scheduleTaskId);

        projectOpenApiService.resumeProject(controlRequest);
        log.info("Resume oms project completed, omsProjectId={}, scheduleTaskId={}",
                taskParameters.getOmsProjectId(), scheduleTaskId);
        // update task parameters rate limit same as schedule
        taskParameters.setRateLimitConfig(oscParameters.getRateLimitConfig());
        int rows = scheduleTaskRepository.updateTaskParameters(scheduleTaskId, JsonUtils.toJson(taskParameters));
        if (rows > 0) {
            log.info("Update throttle completed, scheduleTaskId={}", scheduleTaskId);
        }
    }

    private void handleOmsProjectStepResult(ValveContext valveContext, ProjectStepResult projectStepResult,
            OnlineSchemaChangeScheduleTaskResult result, SwapTableType swapTableType, ScheduleTaskEntity scheduleTask) {

        if (projectStepResult.getTaskStatus() == TaskStatus.DONE
                && (projectStepResult.getFullVerificationResult() == FullVerificationResult.CONSISTENT ||
                        projectStepResult.getFullVerificationResult() == FullVerificationResult.UNCHECK)) {

            boolean isEnableSwapTable = SwapTableUtil.isSwapTableEnable(swapTableType,
                    scheduleTask.getStatus(), result.getFullTransferProgressPercentage(),
                    result.getFullVerificationResult());
            if (isEnableSwapTable && !result.isManualSwapTableStarted()) {
                if (!result.isManualSwapTableEnabled()) {
                    // open manual swap table
                    result.setManualSwapTableEnabled(true);
                    scheduleTask.setResultJson(JsonUtils.toJson(result));
                    scheduleTaskRepository.update(scheduleTask);
                }
            } else {
                scheduleTask.setResultJson(JsonUtils.toJson(result));
                scheduleTaskRepository.update(scheduleTask);
                getNext().invoke(valveContext);
            }

        } else {
            continueHandleProjectStepResult(projectStepResult);
        }
    }

    private void continueHandleProjectStepResult(ProjectStepResult projectStepResult) {
        if (projectStepResult.getPreCheckResult() == PrecheckResult.FAILED) {
            throw new OscException(ErrorCodes.OmsPreCheckFailed, projectStepResult.getErrorMsg());
        } else if (projectStepResult.getTaskStatus() == TaskStatus.FAILED) {
            throw new OscException(ErrorCodes.OmsProjectExecutingFailed, projectStepResult.getErrorMsg());
        } else if (projectStepResult.getFullVerificationResult() == FullVerificationResult.INCONSISTENT) {
            throw new OscException(ErrorCodes.OmsDataCheckInconsistent,
                    "Task failed for origin table has inconsistent data with new table, result: "
                            + projectStepResult.getFullVerificationResultDescription());
        }
    }

    private OmsProjectFullVerifyResultResponse listProjectFullVerifyResult(String projectId, String databaseName,
            String uid) {
        ListOmsProjectFullVerifyResultRequest request = new ListOmsProjectFullVerifyResultRequest();
        request.setProjectId(projectId);
        request.setSourceSchemas(new String[] {databaseName});
        request.setDestSchemas(new String[] {databaseName});
        request.setStatus(new String[] {"FINISHED", "SUSPEND", "RUNNING"});
        request.setPageSize(10);
        request.setPageNumber(1);
        request.setUid(uid);

        OmsProjectFullVerifyResultResponse response = projectOpenApiService.listProjectFullVerifyResult(request);
        if (log.isDebugEnabled()) {
            log.debug("Get project full verify result from projectOpenApiService is {} ", JsonUtils.toJson(response));
        }
        return response;
    }

    private void adaptResult(OnlineSchemaChangeScheduleTaskResult result, ProjectStepResult projectStepResult) {
        result.setFullTransferEstimatedCount(projectStepResult.getFullTransferEstimatedCount());
        result.setFullTransferFinishedCount(projectStepResult.getFullTransferFinishedCount());
        result.setFullTransferProgressPercentage(projectStepResult.getFullTransferProgressPercentage());
        result.setFullVerificationResult(projectStepResult.getFullVerificationResult());
        result.setFullVerificationResultDescription(projectStepResult.getFullVerificationResultDescription());
        result.setFullVerificationProgressPercentage(projectStepResult.getFullVerificationProgressPercentage());
        result.setCurrentStep(projectStepResult.getCurrentStep());
        result.setCurrentStepStatus(projectStepResult.getCurrentStepStatus());
        result.setPrecheckResult(projectStepResult.getPreCheckResult());
        result.setCheckFailedTime(projectStepResult.getCheckFailedTime());
    }

    private OmsProjectControlRequest getProjectRequest(OnlineSchemaChangeScheduleTaskParameters taskParam) {
        OmsProjectControlRequest projectRequest = new OmsProjectControlRequest();
        projectRequest.setUid(taskParam.getUid());
        projectRequest.setId(taskParam.getOmsProjectId());
        return projectRequest;
    }

    private void recordCurrentProgress(String projectId, OnlineSchemaChangeScheduleTaskResult result) {
        String progress = "";
        String description = "";
        OmsStepName step = OmsStepName.UNKNOWN;
        try {
            if (result.getCurrentStep() != null) {
                step = OmsStepName.valueOf(result.getCurrentStep());
                switch (step) {
                    case PRE_CHECK:
                        description = result.getPrecheckResultDescription();
                        break;
                    case FULL_TRANSFER:
                        progress = result.getFullTransferProgressPercentage().toString();
                        description = result.getFullTransferFinishedCount() + "/"
                                + result.getFullTransferEstimatedCount();
                        break;
                    case FULL_VERIFIER:
                        progress = result.getFullVerificationProgressPercentage().toString();
                        description = result.getFullVerificationResultDescription();
                        break;
                    default:
                }
            }
        } catch (Exception ex) {
            log.warn("Get oms step progress and description occur error");
        }

        List<String> body = Lists.newArrayList(projectId, step == OmsStepName.UNKNOWN ? "" : step.name(),
                result.getCurrentStepStatus(), progress, description);
        Table table = new DefaultTableFactory().generateTable(5, getHeader(), body);
        log.info("\n" + table.render().toString() + "\n");
    }

    private List<String> getHeader() {
        return Lists.newArrayList("ProjectId", "Step", "Status", "Progress", "Description");
    }
}
