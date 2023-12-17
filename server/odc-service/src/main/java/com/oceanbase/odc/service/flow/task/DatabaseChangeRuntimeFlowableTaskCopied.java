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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.connection.model.ConnectProperties;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.datasecurity.DataMaskingService;
import com.oceanbase.odc.service.flow.exception.ServiceTaskError;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeResult;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudEnvConfigurations;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.session.DBSessionManageFacade;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.odc.service.task.caller.JobException;
import com.oceanbase.odc.service.task.caller.JobUtils;
import com.oceanbase.odc.service.task.constants.JobDataMapConstants;
import com.oceanbase.odc.service.task.executor.task.DatabaseChangeTask;
import com.oceanbase.odc.service.task.schedule.DefaultJobDefinition;
import com.oceanbase.odc.service.task.schedule.JobDefinition;
import com.oceanbase.odc.service.task.schedule.JobScheduler;
import com.oceanbase.odc.service.task.schedule.SampleTaskJobDefinitionBuilder;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2022/2/18
 */

@Slf4j
public class DatabaseChangeRuntimeFlowableTaskCopied extends BaseODCFlowTaskDelegate<DatabaseChangeResult> {

    private volatile JobEntity jobEntity;
    private volatile boolean isSuccessful = false;
    private volatile boolean isFailure = false;
    private volatile DelegateExecution execution;
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
    @Autowired
    private CloudEnvConfigurations cloudEnvConfigurations;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning, Long taskId, TaskService taskService) {
        try {
            jobScheduler.cancelJob(jobEntity.getId());
            killCurrentQuery(sessionManageFacade);
        } catch (JobException e) {
            log.warn("cancel job failed.", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean isCancelled() {
        return jobEntity != null && jobEntity.getStatus() == TaskStatus.CANCELED;
    }

    @Override
    protected DatabaseChangeResult start(Long taskId, TaskService taskService, DelegateExecution execution)
            throws JobException {
        this.execution = execution;
        DatabaseChangeResult result;
        try {
            log.info("Async task starts, taskId={}, activityId={}", taskId, execution.getCurrentActivityId());

            JobDefinition jd = buildJobDefinition(execution, taskId);
            Long jobId = jobScheduler.scheduleJobNow(jd);
            jobEntity = taskFrameworkService.find(jobId);
            try {
                jobScheduler.await(jobId, FlowTaskUtil.getExecutionExpirationIntervalMillis(execution),
                        TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.warn("wait job finished, occur exception:", e);
            }
            jobEntity = taskFrameworkService.find(jobId);
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
        JobDefinition jd = new SampleTaskJobDefinitionBuilder().build(config, FlowTaskUtil.getSchemaName(execution),
                SqlUtils.split(config.getDialectType(), parameters.getSqlContent(), ";"));
        jd.getJobData().put(JobDataMapConstants.BUZ_ID, FlowTaskUtil.getTaskId(execution) + "");
        return jd;
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

    }

    private JobDefinition buildJobDefinition(DelegateExecution execution, Long taskId) {
        DatabaseChangeParameters parameters = FlowTaskUtil.getAsyncParameter(execution);
        ConnectionConfig config = FlowTaskUtil.getConnectionConfig(execution);
        Map<String, String> jobData = new HashMap<>();
        jobData.put(JobDataMapConstants.META_DB_TASK_PARAMETER, JsonUtils.toJson(parameters));
        jobData.put(JobDataMapConstants.CONNECTION_CONFIG, JobUtils.toJson(config));
        jobData.put(JobDataMapConstants.FLOW_INSTANCE_ID, FlowTaskUtil.getFlowInstanceId(execution) + "");
        jobData.put(JobDataMapConstants.CURRENT_SCHEMA_KEY, FlowTaskUtil.getSchemaName(execution));
        jobData.put(JobDataMapConstants.SESSION_TIME_ZONE, connectProperties.getDefaultTimeZone());
        jobData.put(JobDataMapConstants.OBJECT_STORAGE_CONFIGURATION,
                JsonUtils.toJson(cloudEnvConfigurations.getObjectStorageConfiguration()));
        List<ObjectMetadata> objectMetadatas = new ArrayList<>();
        for (String objectId : parameters.getSqlObjectIds()) {
            ObjectMetadata om = objectStorageFacade.loadMetaData(
                    "async".concat(File.separator).concat(FlowTaskUtil.getTaskCreator(execution).getId() + ""),
                    objectId);
            objectMetadatas.add(om);
        }
        jobData.put(JobDataMapConstants.OBJECT_METADATA, JsonUtils.toJson(objectMetadatas));
        return DefaultJobDefinition.builder().jobClass(DatabaseChangeTask.class)
                .jobType(TaskType.ASYNC.name())
                .jobData(jobData)
                .build();
    }

    public void killCurrentQuery(DBSessionManageFacade sessionManageFacade) {
        log.info("Try to kill current query");
        try {
            sessionManageFacade.killCurrentQuery(generateSession());
            log.info("Kill current query success");
        } catch (Exception e) {
            log.warn("Kill current query failed", e);
        }
    }

    private ConnectionSession generateSession() {
        ConnectionConfig connectionConfig = FlowTaskUtil.getConnectionConfig(getExecution());
        DefaultConnectSessionFactory sessionFactory = new DefaultConnectSessionFactory(connectionConfig);
        return sessionFactory.generateSession();
    }

    private DelegateExecution getExecution() {
        return this.execution;
    }
}
