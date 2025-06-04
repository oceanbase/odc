/*
 * Copyright (c) 2025 OceanBase.
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.util.concurrent.AtomicDouble;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalDatabaseService;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalTableService;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.ExecutionResult;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.sql.SqlExecutionResultWrapper;
import com.oceanbase.odc.service.connection.logicaldatabase.model.DetailLogicalDatabaseResp;
import com.oceanbase.odc.service.schedule.model.LogicalDatabaseChangeParameters;
import com.oceanbase.odc.service.schedule.model.PublishLogicalDatabaseChangeReq;
import com.oceanbase.odc.service.task.TaskService;

import com.oceanbase.odc.service.task.base.logicdatabasechange.LogicalDatabaseChangeTask;
import com.oceanbase.odc.service.task.caller.DefaultJobContext;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.schedule.SingleJobProperties;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/8/30 16:08
 * @Description: []
 */
@Slf4j
public class LogicalDatabaseChangeFlowableTask extends BaseODCFlowTaskDelegate<Void> {
    @Autowired
    private TaskService taskService;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private LogicalDatabaseService logicalDatabaseService;

    @Autowired
    private LogicalTableService logicalTableService;

    private LogicalDatabaseChangeTask logicalDatabaseChangeTask;

    private AtomicBoolean isSuccessful = new AtomicBoolean(false);

    private AtomicBoolean isCancelled = new AtomicBoolean(false);

    private AtomicReference<Throwable> exception = new AtomicReference<>(null);

    private AtomicDouble lastProgress = new AtomicDouble(-1);

    private AtomicReference<String> lastResult = new AtomicReference<>(null);

    @Override
    protected Void start(Long taskId, TaskService taskService, DelegateExecution execution) throws Exception {
        try {
            PublishLogicalDatabaseChangeReq req = buildPublishLogicalDatabaseChangeReq(taskId);
            JobContext jobContext = buildJobContext(req, req.getTimeoutMillis());
            logicalDatabaseChangeTask = new LogicalDatabaseChangeTask();
            logicalDatabaseChangeTask.doInit(jobContext);
            boolean result = logicalDatabaseChangeTask.start();
            isSuccessful.set(result);
            return null;
        } catch (Throwable e) {
            isSuccessful.set(false);
            exception.set(e);
            log.info("LogicalDatabaseRuntimeFlowableTask execute failed, taskId={}", taskId, e);
            stopTask();
            throw e;
        }
    }

    public JobContext buildJobContext(PublishLogicalDatabaseChangeReq publishReq, Long timeoutMillis) {
        Map<String, String> jobData = new HashMap<>();
        jobData.put(JobParametersKeyConstants.TASK_PARAMETER_JSON_KEY,
            JobUtils.toJson(publishReq));
        if (timeoutMillis != null) {
            jobData.put(JobParametersKeyConstants.TASK_EXECUTION_END_TIME_MILLIS,
                String.valueOf(System.currentTimeMillis() + timeoutMillis));
        }
        Map<String, String> jobProperties = new HashMap<>();
        SingleJobProperties singleJobProperties = new SingleJobProperties();
        singleJobProperties.setEnableRetryAfterHeartTimeout(true);
        singleJobProperties.setMaxRetryTimesAfterHeartTimeout(1);
        jobProperties.putAll(singleJobProperties.toJobProperties());
        DefaultJobContext context = new DefaultJobContext();
        context.setJobIdentity(JobIdentity.of(taskId));
        context.setJobClass(LogicalDatabaseChangeTask.class.getName());
        context.setJobProperties(jobProperties);
        context.setJobParameters(jobData);
        return context;
    }

    private PublishLogicalDatabaseChangeReq buildPublishLogicalDatabaseChangeReq(Long taskID) {
        TaskEntity taskEntity = taskService.detail(taskID);
        LogicalDatabaseChangeParameters parameters = JsonUtils.fromJson(taskEntity.getParametersJson(),
            LogicalDatabaseChangeParameters.class);
        // check and query parameters
        PublishLogicalDatabaseChangeReq req = new PublishLogicalDatabaseChangeReq();
        req.setSqlContent(parameters.getSqlContent());
        req.setCreatorId(taskEntity.getCreatorId());
        DetailLogicalDatabaseResp logicalDatabaseResp = logicalDatabaseService.detail(parameters.getDatabaseId());
        logicalDatabaseResp.getPhysicalDatabases().stream().forEach(
            database -> database.setDataSource(
                connectionService.getForConnectionSkipPermissionCheck(database.getDataSource().getId())));
        req.setLogicalDatabaseResp(logicalDatabaseResp);
        req.setDelimiter(parameters.getDelimiter());
        req.setTimeoutMillis(parameters.getTimeoutMillis());
        req.setScheduleTaskId(taskEntity.getId());
        return req;
    }

    private void stopTask() {
        if (null != logicalDatabaseChangeTask) {
            logicalDatabaseChangeTask.stop();
        }
    }

    @Override
    protected boolean isSuccessful() {
        return isSuccessful.get();
    }

    @Override
    protected boolean isFailure() {
        // first check if is canceled
        if (isCancelled.get()) {
            return false;
        }
        // not success and exception found, then it's failure
        return !isSuccessful.get() && null != exception.get();
    }

    @Override
    protected void onProgressUpdate(Long taskId, TaskService taskService) {
        if (null == logicalDatabaseChangeTask) {
            return;
        }
        double previousProgress = lastProgress.get();
        double currentProgress = logicalDatabaseChangeTask.getProgress();
        Map<String, ExecutionResult<SqlExecutionResultWrapper>> result = logicalDatabaseChangeTask.getTaskResult();
        String currentResult = JsonUtils.toJson(result);

        // check progress and result change
        if (currentProgress == previousProgress && StringUtils.equals(currentResult, lastResult.get())) {
            return;
        }

        // update and save progress and result
        lastProgress.set(currentProgress);
        lastResult.set(currentResult);
        TaskEntity taskEntity = taskService.detail(taskId);
        taskEntity.setProgressPercentage(currentProgress);
        taskEntity.setResultJson(JsonUtils.toJson(result));
        taskService.update(taskEntity);
    }

    @Override
    protected boolean cancel(boolean mayInterruptIfRunning, Long taskId, TaskService taskService) {
        isCancelled.set(true);
        stopTask();
        return true;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled.get();
    }
}
