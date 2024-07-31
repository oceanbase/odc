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
import java.util.concurrent.Executors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.onlineschemachange.exception.OmsException;
import com.oceanbase.odc.service.onlineschemachange.fsm.Action;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsProjectStatusEnum;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.OmsProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.request.OmsProjectControlRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectProgressResponse;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionContext;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionResult;
import com.oceanbase.odc.service.onlineschemachange.oscfms.state.OscStates;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2024/7/9 11:58
 * @since 4.3.1
 */
@Slf4j
public class OmsCleanResourcesAction implements Action<OscActionContext, OscActionResult> {

    private final OmsProjectOpenApiService projectOpenApiService;

    private final List<TaskStatus> expectedTaskStatus = Lists.newArrayList(TaskStatus.DONE, TaskStatus.FAILED,
            TaskStatus.CANCELED, TaskStatus.RUNNING);

    public OmsCleanResourcesAction(@NonNull OmsProjectOpenApiService omsProjectOpenApiService) {
        this.projectOpenApiService = omsProjectOpenApiService;
    }

    @Override
    public OscActionResult execute(OscActionContext context) throws Exception {
        OnlineSchemaChangeScheduleTaskParameters taskParameters = context.getTaskParameter();
        ScheduleTaskEntity scheduleTask = context.getScheduleTask();
        PreConditions.validArgumentState(expectedTaskStatus.contains(scheduleTask.getStatus()), ErrorCodes.Unexpected,
                new Object[] {scheduleTask.getStatus()}, "schedule task is not excepted status");
        // Oms project must be released if current schedule task is not running
        boolean released = checkAndReleaseProject(taskParameters.getOmsProjectId(), taskParameters.getUid());
        if (!released) {
            // try release again
            log.info("OCS: release OMS project failed, try it again. OMS project ID {}, uid {}",
                    taskParameters.getOmsProjectId(), taskParameters.getUid());
            return new OscActionResult(OscStates.CLEAN_RESOURCE.getState(), null, OscStates.CLEAN_RESOURCE.getState());
        }
        return determinateNextState(scheduleTask, context.getSchedule());
    }

    @VisibleForTesting
    protected OscActionResult determinateNextState(ScheduleTaskEntity scheduleTask, ScheduleEntity schedule) {
        Long scheduleId = schedule.getId();
        // try to dispatch to next state for done status
        if (scheduleTask.getStatus() == TaskStatus.DONE) {
            return new OscActionResult(OscStates.CLEAN_RESOURCE.getState(), null, OscStates.YIELD_CONTEXT.getState());
        }
        // if task state is in cancel state, stop and transfer to complete state
        if (scheduleTask.getStatus() == TaskStatus.CANCELED) {
            log.info("Because task is canceled, so delete quartz job {}", scheduleId);
            // cancel as complete
            return new OscActionResult(OscStates.CLEAN_RESOURCE.getState(), null, OscStates.COMPLETE.getState());
        }
        // remain failed and prepare state
        OnlineSchemaChangeParameters onlineSchemaChangeParameters = JsonUtils.fromJson(
               schedule.getJobParametersJson(), OnlineSchemaChangeParameters.class);
        if (onlineSchemaChangeParameters.getErrorStrategy() == TaskErrorStrategy.CONTINUE) {
            log.info("Because error strategy is continue, so schedule next task");
            // try schedule next task
            return new OscActionResult(OscStates.CLEAN_RESOURCE.getState(), null, OscStates.YIELD_CONTEXT.getState());
        } else {
            log.info("Because error strategy is abort, so delete quartz job {}", scheduleId);
            // not continue for remain state, transfer to complete state
            return new OscActionResult(OscStates.CLEAN_RESOURCE.getState(), null, OscStates.COMPLETE.getState());
        }
    }

    @VisibleForTesting
    protected boolean checkAndReleaseProject(String omsProjectId, String uid) {
        if (omsProjectId == null) {
            return true;
        }
        OmsProjectControlRequest controlRequest = new OmsProjectControlRequest();
        controlRequest.setId(omsProjectId);
        controlRequest.setUid(uid);
        log.info("Oms project {} has not released, try to release it.", omsProjectId);
        return checkAndReleaseProject(controlRequest);
    }

    protected boolean checkAndReleaseProject(OmsProjectControlRequest projectControl) {
        if (projectControl.getId() == null) {
            return true;
        }

        boolean released = false;
        OmsProjectControlRequest controlRequest = new OmsProjectControlRequest();
        controlRequest.setId(projectControl.getId());
        controlRequest.setUid(projectControl.getUid());
        try {
            OmsProjectProgressResponse progressResponse = projectOpenApiService.describeProjectProgress(controlRequest);
            released = progressResponse.getStatus().isProjectDestroyed();
        } catch (OmsException e) {
            // representative project has been deleted when message contain "GHANA-PROJECT000001"
            // or "project is not exists",
            if (e.getMessage() != null && e.getMessage().contains("GHANA-PROJECT000001")) {
                released = true;
            }
        }

        if (!released) {
            log.info("Oms project {} has not released, try to release it.", projectControl.getId());
            doReleaseOmsResource(controlRequest);
        }
        return released;
    }

    private void doReleaseOmsResource(OmsProjectControlRequest projectControlRequest) {
        if (projectControlRequest.getId() == null) {
            return;
        }
        // try release sync
        try {
            OmsProjectProgressResponse response =
                projectOpenApiService.describeProjectProgress(projectControlRequest);
            if (response.getStatus() == OmsProjectStatusEnum.RUNNING) {
                projectOpenApiService.stopProject(projectControlRequest);
            }
            projectOpenApiService.releaseProject(projectControlRequest);
            log.info("Release oms project, id {}", projectControlRequest.getId());
        } catch (Throwable ex) {
            log.warn("Failed to release oms project, id {}, occur error {}", projectControlRequest.getId(),
                ex.getMessage());
        }
    }
}
