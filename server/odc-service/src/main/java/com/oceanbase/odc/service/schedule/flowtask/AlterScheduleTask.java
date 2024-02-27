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
package com.oceanbase.odc.service.schedule.flowtask;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.fastjson.JSON;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.service.flow.task.BaseODCFlowTaskDelegate;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;
import com.oceanbase.odc.service.task.TaskService;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tianke
 * @Date: 2022/9/18 19:31
 * @Descripition:
 */
@Slf4j
public class AlterScheduleTask extends BaseODCFlowTaskDelegate<AlterScheduleResult> {

    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    private volatile boolean isSuccessful = false;
    private volatile boolean isFailure = false;

    @Override
    protected AlterScheduleResult start(Long taskId, TaskService taskService, DelegateExecution execution)
            throws Exception {
        AlterScheduleTraceContextHolder.trace(authenticationFacade.currentUser().getId(), taskId);
        AlterScheduleResult taskResult = new AlterScheduleResult();
        log.info("Start to alter schedule task.");
        try {
            taskService.start(taskId);
            AlterScheduleParameters parameters = FlowTaskUtil.getAlterScheduleTaskParameters(execution);
            ScheduleEntity scheduleEntity =
                    scheduleService.nullSafeGetById(parameters.getTaskId());
            scheduleEntity.setModifierId(FlowTaskUtil.getTaskCreator(execution).getCreatorId());
            log.info("operation type = {}", parameters.getOperationType());
            switch (parameters.getOperationType()) {
                case CREATE: {
                    scheduleService.enable(scheduleEntity);
                    break;
                }
                case UPDATE: {
                    scheduleEntity.setMisfireStrategy(parameters.getMisfireStrategy());
                    scheduleEntity.setAllowConcurrent(parameters.getAllowConcurrent());
                    scheduleEntity.setDescription(parameters.getDescription());
                    scheduleEntity.setTriggerConfigJson(JSON.toJSONString(parameters.getTriggerConfig()));
                    scheduleEntity.setJobParametersJson(JSON.toJSONString(parameters.getScheduleTaskParameters()));
                    scheduleService.updateJobData(scheduleEntity);
                    break;
                }
                case PAUSE: {
                    if (scheduleEntity.getStatus() != ScheduleStatus.ENABLED) {
                        throw new RuntimeException(
                                String.format("Pause schedule not allowed,schedule status=%s",
                                        scheduleEntity.getStatus()));
                    }
                    scheduleService.pause(scheduleEntity);
                    break;
                }
                case TERMINATION:
                    scheduleService.terminate(scheduleEntity);
                    break;
                case RESUME:
                    if (scheduleEntity.getStatus() != ScheduleStatus.PAUSE) {
                        throw new RuntimeException(
                                String.format("Resume schedule not allowed,schedule status=%s",
                                        scheduleEntity.getStatus()));
                    }
                    scheduleService.resume(scheduleEntity);
                    break;
                default:
                    throw new UnsupportedException(
                            String.format("Unsupported operation type,type=%s", parameters.getOperationType()));
            }
            taskResult.setParameters(parameters);
            taskService.succeed(taskId, taskResult);
            isSuccessful = true;
            log.info("Alter schedule succeed,taskId={}", taskId);
            return taskResult;
        } catch (Exception e) {
            isFailure = true;
            AlterScheduleParameters parameters = FlowTaskUtil.getAlterScheduleTaskParameters(
                    execution);
            if (parameters.getOperationType() == OperationType.CREATE) {
                scheduleService.updateStatusById(parameters.getTaskId(),
                        ScheduleStatus.TERMINATION);
            }
            taskService.fail(taskId, 0, taskResult);
            log.warn("Alter schedule failed,error={}", e.getMessage());
            return taskResult;
        } finally {
            AlterScheduleTraceContextHolder.clear();
        }

    }

    @Override
    protected boolean isSuccessful() {
        return isSuccessful;
    }

    @Override
    protected boolean isFailure() {
        return isFailure;
    }

    @Override
    protected void onFailure(Long taskId, TaskService taskService) {
        log.warn("Alter schedule failed, taskId={}", taskId);
    }

    @Override
    protected void onSuccessful(Long taskId, TaskService taskService) {
        log.info("Alter schedule succeed, taskId={}", taskId);
        updateFlowInstanceStatus(FlowStatus.EXECUTION_SUCCEEDED);
    }

    @Override
    protected void onTimeout(Long taskId, TaskService taskService) {
        log.warn("Alter schedule timeout, taskId={}", taskId);
    }

    @Override
    protected void onProgressUpdate(Long taskId, TaskService taskService) {

    }

    @Override
    protected boolean cancel(boolean mayInterruptIfRunning, Long taskId, TaskService taskService) {
        taskService.cancel(taskId);
        return true;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }
}
