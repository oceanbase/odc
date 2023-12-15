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
package com.oceanbase.odc.service.flow.task;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.connection.model.ConnectProperties;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.datasecurity.DataMaskingService;
import com.oceanbase.odc.service.datasecurity.accessor.DatasourceColumnAccessor;
import com.oceanbase.odc.service.flow.exception.ServiceTaskError;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeResult;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.session.DBSessionManageFacade;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.odc.service.task.caller.JobException;
import com.oceanbase.odc.service.task.caller.JobUtils;
import com.oceanbase.odc.service.task.constants.JobDataMapConstants;
import com.oceanbase.odc.service.task.executor.sampletask.SampleTask;
import com.oceanbase.odc.service.task.listener.TaskResultProgressUploadListener;
import com.oceanbase.odc.service.task.schedule.DefaultJobDefinition;
import com.oceanbase.odc.service.task.schedule.JobDefinition;
import com.oceanbase.odc.service.task.schedule.JobScheduler;
import com.oceanbase.odc.service.task.service.JobEntity;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2022/2/18
 */

@Slf4j
public class DatabaseChangeRuntimeFlowableTaskCopied extends BaseODCFlowTaskDelegate<DatabaseChangeResult> {

    private volatile DatabaseChangeThread asyncTaskThread;
    private volatile boolean isSuccessful = false;
    private volatile boolean isFailure = false;
    @Autowired
    private CloudObjectStorageService cloudObjectStorageService;
    @Autowired
    private ObjectStorageFacade objectStorageFacade;
    @Autowired
    private ConnectProperties connectProperties;
    @Autowired
    private DataMaskingService maskingService;
    @Autowired
    private DBSessionManageFacade sessionManageFacade;
    @Autowired
    private JobScheduler jobScheduler;
    @Autowired
    private TaskFrameworkService taskFrameworkService;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning, Long taskId, TaskService taskService) {
        Verify.notNull(asyncTaskThread, "AsyncTaskThread can not be null");
        asyncTaskThread.stopTaskAndKillQuery(sessionManageFacade);
        taskService.cancel(taskId);
        return true;
    }

    @Override
    public boolean isCancelled() {
        Verify.notNull(asyncTaskThread, "AsyncTaskThread can not be null");
        return asyncTaskThread.getStop();
    }

    @Override
    protected DatabaseChangeResult start(Long taskId, TaskService taskService, DelegateExecution execution)
            throws JobException {
        DatabaseChangeResult result;
        try {
            log.info("Async task starts, taskId={}, activityId={}", taskId, execution.getCurrentActivityId());

            TaskEntity taskEntity = taskService.detail(taskId);
            JobDefinition jd = buildJobDefinition(execution);
            Long jobId = jobScheduler.scheduleJobNow(jd);
            jobScheduler.getEventPublisher().addEventListener(
                    new TaskResultProgressUploadListener(taskService, jobId, taskEntity.getId()));
            try {
                jobScheduler.await(jobId, FlowTaskUtil.getExecutionExpirationIntervalMillis(execution),
                        TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.warn("wait job finished, occur exception:", e);
            }
            JobEntity jobEntity = taskFrameworkService.find(jobId);
            result = JsonUtils.fromJson(jobEntity.getResultJson(), DatabaseChangeResult.class);
            TaskStatus status = jobEntity.getStatus();
            isSuccessful = status == TaskStatus.DONE;
            isFailure = !isSuccessful;
            log.info("Async task ends, taskId={}, activityId={}, returnVal={}, timeCost={}", taskId,
                    execution.getCurrentActivityId(),
                    result, System.currentTimeMillis() - getStartTimeMilliSeconds());
        } catch (Exception e) {
            log.error("Error occurs while async task executing", e);
            throw new ServiceTaskError(e);
        }
        return result;
    }

    private JobDefinition buildJobDefinition(DelegateExecution execution) {
        DatabaseChangeParameters parameters = FlowTaskUtil.getAsyncParameter(execution);
        ConnectionConfig config = FlowTaskUtil.getConnectionConfig(execution);
        Map<String, String> jobData = new HashMap<>();
        jobData.put(JobDataMapConstants.META_DB_TASK_PARAMETER, JsonUtils.toJson(parameters));
        jobData.put(JobDataMapConstants.CONNECTION_CONFIG, JobUtils.toJson(config));
        return DefaultJobDefinition.builder().jobClass(SampleTask.class)
                .jobType(TaskType.ASYNC.name())
                .jobData(jobData)
                .build();
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
        log.warn("Async task failed, taskId={}", taskId);
        super.onFailure(taskId, taskService);
    }

    @Override
    protected void onSuccessful(Long taskId, TaskService taskService) {
        log.info("Async task succeed, taskId={}", taskId);
        updateFlowInstanceStatus(FlowStatus.EXECUTION_SUCCEEDED);
        super.onSuccessful(taskId, taskService);
    }

    @Override
    protected void onTimeout(Long taskId, TaskService taskService) {
        log.warn("Async task timeout, taskId={}", taskId);
    }

    @Override
    protected void onProgressUpdate(Long taskId, TaskService taskService) {
        if (Objects.nonNull(asyncTaskThread)) {
            double progress = asyncTaskThread.getProgressPercentage();
            taskService.updateProgress(taskId, progress);
            if (System.currentTimeMillis() - asyncTaskThread.getStartTimestamp() > getTimeOutMilliSeconds()) {
                asyncTaskThread.stopTaskAndKillQuery(sessionManageFacade);
            }
        }
    }


}
