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

import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.tableformat.Table;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.exception.OscException;
import com.oceanbase.odc.service.onlineschemachange.fsm.Action;
import com.oceanbase.odc.service.onlineschemachange.logger.DefaultTableFactory;
import com.oceanbase.odc.service.onlineschemachange.model.FullVerificationResult;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskResult;
import com.oceanbase.odc.service.onlineschemachange.model.PrecheckResult;
import com.oceanbase.odc.service.onlineschemachange.model.SwapTableType;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsStepName;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.OmsProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionContext;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionResult;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.oms.ProjectStepResultChecker.ProjectStepResult;
import com.oceanbase.odc.service.onlineschemachange.oscfms.state.OscStates;
import com.oceanbase.odc.service.onlineschemachange.rename.SwapTableUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2024/7/8 20:04
 * @since 4.3.1
 */
@Slf4j
public class OmsMonitorDataTaskAction implements Action<OscActionContext, OscActionResult> {
    private final OmsProjectOpenApiService projectOpenApiService;

    private final OnlineSchemaChangeProperties onlineSchemaChangeProperties;

    public OmsMonitorDataTaskAction(@NotNull OmsProjectOpenApiService projectOpenApiService,
            @NotNull OnlineSchemaChangeProperties onlineSchemaChangeProperties) {
        this.projectOpenApiService = projectOpenApiService;
        this.onlineSchemaChangeProperties = onlineSchemaChangeProperties;
    }

    @Override
    public OscActionResult execute(OscActionContext context) throws Exception {
        ScheduleTaskEntity scheduleTask = context.getScheduleTask();
        log.debug("Start execute {}, schedule task id={}", getClass().getSimpleName(), scheduleTask.getId());

        OnlineSchemaChangeScheduleTaskParameters taskParameter = context.getTaskParameter();
        OnlineSchemaChangeParameters inputParameters = context.getParameter();
        // switch state
        if (shouldUpdateOMSConfig(taskParameter, inputParameters)) {
            return new OscActionResult(OscStates.MONITOR_DATA_TASK.getState(), null,
                    OscStates.MODIFY_DATA_TASK.getState());
        }
        // get result
        OnlineSchemaChangeScheduleTaskResult lastResult = JsonUtils.fromJson(scheduleTask.getResultJson(),
                OnlineSchemaChangeScheduleTaskResult.class);

        OnlineSchemaChangeScheduleTaskResult result =
                new OnlineSchemaChangeScheduleTaskResult(taskParameter);

        if (lastResult != null) {
            result.setManualSwapTableEnabled(lastResult.isManualSwapTableEnabled());
            result.setManualSwapTableStarted(lastResult.isManualSwapTableStarted());
        }
        // get oms step result
        ProjectStepResult projectStepResult =
                OmsRequestUtil.buildProjectStepResult(projectOpenApiService, onlineSchemaChangeProperties,
                        taskParameter.getUid(), taskParameter.getOmsProjectId(), taskParameter.getDatabaseName(),
                        lastResult.getCheckFailedTime());
        adaptResult(result, projectStepResult);
        scheduleTask.setResultJson(JsonUtils.toJson(result));
        scheduleTask.setProgressPercentage(projectStepResult.getTaskPercentage());
        // update progress, can merge with following update?
        recordCurrentProgress(taskParameter.getOmsProjectId(), result);
        try {
            return handleOmsProjectStepResult(context, projectStepResult, result,
                    context.getParameter().getSwapTableType(), scheduleTask);
        } finally {
            // update schedule task once
            context.getScheduleTaskRepository().update(scheduleTask);
        }
    }

    @VisibleForTesting
    protected OscActionResult handleOmsProjectStepResult(OscActionContext context, ProjectStepResult projectStepResult,
            OnlineSchemaChangeScheduleTaskResult result, SwapTableType swapTableType, ScheduleTaskEntity scheduleTask) {
        // oms task is ready, try swap step
        if (OmsRequestUtil.isOmsTaskReady(projectStepResult)) {
            // is manual swap table
            boolean isSwapTableReady =
                    SwapTableUtil.isSwapTableReady(scheduleTask.getStatus(), result.getFullTransferProgressPercentage(),
                            result.getFullVerificationResult());
            // can't swap table
            if (!isSwapTableReady) {
                // keep in same state, monitor task state
                return new OscActionResult(OscStates.MONITOR_DATA_TASK.getState(), null,
                        OscStates.MONITOR_DATA_TASK.getState());

            }
            // try do swap table
            switch (swapTableType) {
                case AUTO:
                    // auto swap, jump to swap stable state
                    scheduleTask.setResultJson(JsonUtils.toJson(result));
                    return new OscActionResult(OscStates.MONITOR_DATA_TASK.getState(), null,
                            OscStates.SWAP_TABLE.getState());
                case MANUAL:
                    if (!result.isManualSwapTableStarted()) {
                        // isManualSwapTableEnabled set true to let swap table button show on front-end panel
                        if (!result.isManualSwapTableEnabled()) {
                            // open manual swap table
                            result.setManualSwapTableEnabled(true);
                            scheduleTask.setResultJson(JsonUtils.toJson(result));
                        }
                        log.info("OSC: oms project ready, wait manual swap table triggered, task id={}",
                                scheduleTask.getId());
                        // manual swap table not set, keep waiting
                        return new OscActionResult(OscStates.MONITOR_DATA_TASK.getState(), null,
                                OscStates.MONITOR_DATA_TASK.getState());
                    } else {
                        // jump to swap table state
                        return new OscActionResult(OscStates.MONITOR_DATA_TASK.getState(), null,
                                OscStates.SWAP_TABLE.getState());
                    }
                default:
                    throw new IllegalStateException("invalid state for swap table type");
            }
        } else {
            // check oms status
            return continueHandleProjectStepResult(projectStepResult);
        }
    }

    private OscActionResult continueHandleProjectStepResult(ProjectStepResult projectStepResult) {
        if (projectStepResult.getPreCheckResult() == PrecheckResult.FAILED) {
            throw new OscException(ErrorCodes.OmsPreCheckFailed, projectStepResult.getErrorMsg());
        } else if (projectStepResult.getTaskStatus() == TaskStatus.FAILED) {
            throw new OscException(ErrorCodes.OmsProjectExecutingFailed, projectStepResult.getErrorMsg());
        } else if (projectStepResult.getFullVerificationResult() == FullVerificationResult.INCONSISTENT) {
            throw new OscException(ErrorCodes.OmsDataCheckInconsistent,
                    "Task failed for origin table has inconsistent data with new table, result: "
                            + projectStepResult.getFullVerificationResultDescription());
        } else {
            // stay in monitor state
            return new OscActionResult(OscStates.MONITOR_DATA_TASK.getState(), null,
                    OscStates.MONITOR_DATA_TASK.getState());
        }
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

    @VisibleForTesting
    protected boolean shouldUpdateOMSConfig(OnlineSchemaChangeScheduleTaskParameters taskParameters,
            OnlineSchemaChangeParameters inputParameters) {
        // if rate limiter parameters is changed, try to stop and restart project
        if (Objects.equals(inputParameters.getRateLimitConfig(), taskParameters.getRateLimitConfig())) {
            log.info("Rate limiter not changed,rateLimiterConfig = {}, update oms project not required",
                    inputParameters.getRateLimitConfig());
            return false;
        } else {
            return true;
        }
    }
}
