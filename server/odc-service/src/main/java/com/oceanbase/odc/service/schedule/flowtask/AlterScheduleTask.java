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

import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.flow.task.BaseODCFlowTaskDelegate;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanConfig;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeParams;
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
        ScheduleService scheduleService = SpringContextUtil.getBean(ScheduleService.class);
        log.info("Start to alter schedule task.");
        try{
            AlterScheduleParameters parameters = FlowTaskUtil.getAlterScheduleTaskParameters(execution);
            taskService.start(taskId);
            scheduleService.executeChangeSchedule(parameters.getScheduleChangeParams());
            taskService.succeed(taskId, taskResult);
            isSuccessful = true;
            log.info("Alter schedule succeed,taskId={}", taskId);
        }catch (Exception e){
            log.warn("Alter schedule failed,taskId={}",taskId,e);
            isFailure = true;
            taskService.fail(taskId,0,taskResult);
        }
        return taskResult;
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
        setDownloadLogUrl();
        AlterScheduleTraceContextHolder.clear();
    }

    @Override
    protected void onSuccessful(Long taskId, TaskService taskService) {
        log.info("Alter schedule succeed, taskId={}", taskId);
        setDownloadLogUrl();
        updateFlowInstanceStatus(FlowStatus.EXECUTION_SUCCEEDED);
        AlterScheduleTraceContextHolder.clear();
    }

    @Override
    protected void onTimeout(Long taskId, TaskService taskService) {
        log.warn("Alter schedule timeout, taskId={}", taskId);
        setDownloadLogUrl();
        AlterScheduleTraceContextHolder.clear();
    }

    @Override
    protected void onProgressUpdate(Long taskId, TaskService taskService) {

    }

    @Override
    protected boolean cancel(boolean mayInterruptIfRunning, Long taskId, TaskService taskService) {
        taskService.cancel(taskId);
        AlterScheduleTraceContextHolder.clear();
        return true;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }
}
