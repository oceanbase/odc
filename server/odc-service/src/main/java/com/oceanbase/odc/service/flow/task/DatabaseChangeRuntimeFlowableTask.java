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

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.flow.exception.BaseFlowException;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.connection.model.ConnectProperties;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.datasecurity.DataMaskingService;
import com.oceanbase.odc.service.datasecurity.accessor.DatasourceColumnAccessor;
import com.oceanbase.odc.service.flow.exception.ServiceTaskCancelledException;
import com.oceanbase.odc.service.flow.exception.ServiceTaskError;
import com.oceanbase.odc.service.flow.exception.ServiceTaskExpiredException;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeResult;
import com.oceanbase.odc.service.flow.task.model.RollbackPlanTaskResult;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.session.DBSessionManageFacade;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.task.TaskService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2022/2/18
 */

@Slf4j
public class DatabaseChangeRuntimeFlowableTask extends BaseODCFlowTaskDelegate<DatabaseChangeResult> {

    private volatile DatabaseChangeThread asyncTaskThread;
    private volatile boolean isSuccessful = false;
    private volatile boolean isFailure = false;
    @Autowired
    private CloudObjectStorageService cloudObjectStorageService;
    @Autowired
    private ConnectProperties connectProperties;
    @Autowired
    private DataMaskingService maskingService;
    @Autowired
    private ObjectStorageFacade objectStorageFacade;
    @Autowired
    private DBSessionManageFacade sessionManageFacade;

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
    protected DatabaseChangeResult start(Long taskId, TaskService taskService, DelegateExecution execution) {
        DatabaseChangeResult result;
        try {
            log.info("Async task starts, taskId={}, activityId={}", taskId, execution.getCurrentActivityId());
            asyncTaskThread = generateOdcAsyncTaskThread(taskId, execution);
            taskService.start(taskId);
            TaskEntity taskEntity = taskService.detail(taskId);
            result = JsonUtils.fromJson(taskEntity.getResultJson(), DatabaseChangeResult.class);
            asyncTaskThread.run();
            RollbackPlanTaskResult rollbackPlanTaskResult = null;
            if (result != null) {
                rollbackPlanTaskResult = result.getRollbackPlanResult();
            }
            result = asyncTaskThread.getResult();
            result.setRollbackPlanResult(rollbackPlanTaskResult);
            if (asyncTaskThread.isAbort()) {
                isFailure = true;
                taskService.fail(taskId, asyncTaskThread.getProgressPercentage(), result);
            } else if (asyncTaskThread.getStop()) {
                isFailure = true;
                taskService.fail(taskId, asyncTaskThread.getProgressPercentage(), result);
                if (isTimeout()) {
                    throw new ServiceTaskExpiredException();
                } else {
                    throw new ServiceTaskCancelledException();
                }
            } else {
                isSuccessful = true;
                taskService.succeed(taskId, result);
            }
            log.info("Async task ends, taskId={}, activityId={}, returnVal={}, timeCost={}", taskId,
                    execution.getCurrentActivityId(),
                    result, System.currentTimeMillis() - getStartTimeMilliSeconds());
        } catch (Exception e) {
            log.error("Error occurs while async task executing", e);
            if (e instanceof BaseFlowException) {
                throw e;
            } else {
                throw new ServiceTaskError(e);
            }
        }
        return result;
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
            if (System.currentTimeMillis() - asyncTaskThread.getStartTimestamp() > getTimeoutMillis()) {
                asyncTaskThread.stopTaskAndKillQuery(sessionManageFacade);
            }
        }
    }

    private DatabaseChangeThread generateOdcAsyncTaskThread(Long taskId, DelegateExecution execution) {
        Long creatorId = FlowTaskUtil.getTaskCreator(execution).getId();
        DatabaseChangeParameters parameters = FlowTaskUtil.getAsyncParameter(execution);
        ConnectionConfig connectionConfig = FlowTaskUtil.getConnectionConfig(execution);
        connectionConfig.setQueryTimeoutSeconds((int) TimeUnit.MILLISECONDS.toSeconds(parameters.getTimeoutMillis()));
        DefaultConnectSessionFactory sessionFactory = new DefaultConnectSessionFactory(connectionConfig);
        sessionFactory.setSessionTimeoutMillis(parameters.getTimeoutMillis());
        ConnectionSession connectionSession = sessionFactory.generateSession();
        if (connectionSession.getDialectType() == DialectType.OB_ORACLE) {
            ConnectionSessionUtil.initConsoleSessionTimeZone(connectionSession, connectProperties.getDefaultTimeZone());
        }
        SqlCommentProcessor processor = new SqlCommentProcessor(connectionConfig.getDialectType(), true, true);
        ConnectionSessionUtil.setSqlCommentProcessor(connectionSession, processor);
        ConnectionSessionUtil.setCurrentSchema(connectionSession, FlowTaskUtil.getSchemaName(execution));
        ConnectionSessionUtil.setColumnAccessor(connectionSession, new DatasourceColumnAccessor(connectionSession));
        DatabaseChangeThread returnVal = new DatabaseChangeThread(connectionSession, parameters,
                cloudObjectStorageService, objectStorageFacade, maskingService);
        returnVal.setTaskId(taskId);
        returnVal.setFlowInstanceId(this.getFlowInstanceId());
        returnVal.setUserId(creatorId);
        return returnVal;
    }

}
