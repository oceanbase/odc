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
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.flow.exception.BaseFlowException;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.sql.split.OffsetString;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.core.sql.split.SqlStatementIterator;
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
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeSqlContent;
import com.oceanbase.odc.service.flow.task.model.FlowTaskProperties;
import com.oceanbase.odc.service.flow.task.model.RollbackPlanTaskResult;
import com.oceanbase.odc.service.flow.task.util.DatabaseChangeFileReader;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.session.DBSessionManageFacade;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.createindex.CreateIndex;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;

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
    private ConnectProperties connectProperties;
    @Autowired
    private DataMaskingService maskingService;
    @Autowired
    private ObjectStorageFacade objectStorageFacade;
    @Autowired
    private DBSessionManageFacade sessionManageFacade;
    @Autowired
    private TaskService taskService;
    @Autowired
    private FlowTaskProperties flowTaskProperties;
    private boolean autoModifyTimeout = false;
    @Autowired
    private ObjectStorageFacade storageFacade;

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
            DatabaseChangeParameters parameters = FlowTaskUtil.getAsyncParameter(execution);
            asyncTaskThread = generateOdcAsyncTaskThread(taskId, parameters, execution);
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
            result.setAutoModifyTimeout(this.autoModifyTimeout);
            if (asyncTaskThread.isAbort()
                    || (result.getFailCount() > 0 && parameters.isMarkAsFailedWhenAnyErrorsHappened())) {
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
        super.onSuccessful(taskId, taskService);
        updateFlowInstanceStatus(FlowStatus.EXECUTION_SUCCEEDED);
    }

    @Override
    protected void onTimeout(Long taskId, TaskService taskService) {
        log.warn("Async task timeout, taskId={}", taskId);
        super.onTimeout(taskId, taskService);
    }

    @Override
    protected void onProgressUpdate(Long taskId, TaskService taskService) {
        if (Objects.nonNull(asyncTaskThread)) {
            double progress = asyncTaskThread.getProgressPercentage();
            taskService.updateProgress(taskId, progress);
        }
    }

    private DatabaseChangeThread generateOdcAsyncTaskThread(Long taskId,
            DatabaseChangeParameters parameters, DelegateExecution execution) {
        Long creatorId = FlowTaskUtil.getTaskCreator(execution).getId();
        ConnectionConfig connectionConfig = FlowTaskUtil.getConnectionConfig(execution);
        modifyTimeoutIfTimeConsumingSqlExists(execution, parameters, connectionConfig.getDialectType(), creatorId);
        connectionConfig.setQueryTimeoutSeconds((int) TimeUnit.MILLISECONDS.toSeconds(parameters.getTimeoutMillis()));
        DefaultConnectSessionFactory sessionFactory = new DefaultConnectSessionFactory(connectionConfig);
        sessionFactory.setSessionTimeoutMillis(parameters.getTimeoutMillis());
        ConnectionSession connectionSession = sessionFactory.generateSession();
        if (connectionSession.getDialectType().isOracle()) {
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

    private void modifyTimeoutIfTimeConsumingSqlExists(DelegateExecution execution,
            DatabaseChangeParameters parameters, DialectType dialectType, Long creatorId) {
        long autoModifiedTimeout = flowTaskProperties.getIndexChangeMaxTimeoutMillisecond();
        if (!parameters.isModifyTimeoutIfTimeConsumingSqlExists() || !dialectType.isOceanbase()
                || autoModifiedTimeout <= parameters.getTimeoutMillis()) {
            return;
        }
        List<OffsetString> userInputSqls = null;
        SqlStatementIterator uploadFileSqlIterator = null;
        InputStream uploadFileInputStream = null;
        try {
            DatabaseChangeSqlContent sqlContent =
                    DatabaseChangeFileReader.getSqlContent(storageFacade, parameters, dialectType,
                            "async".concat(File.separator).concat(creatorId.toString()));
            userInputSqls = sqlContent.getUserInputSqls();
            uploadFileSqlIterator = sqlContent.getUploadFileSqlIterator();
            uploadFileInputStream = sqlContent.getUploadFileInputStream();
            while (CollectionUtils.isNotEmpty(userInputSqls)
                    || (uploadFileSqlIterator != null && uploadFileSqlIterator.hasNext())) {
                String sql = CollectionUtils.isNotEmpty(userInputSqls) ? userInputSqls.remove(0).getStr()
                        : uploadFileSqlIterator.next().getStr();
                if (checkTimeConsumingSql(SqlCheckUtil.parseSingleSql(dialectType, sql))) {
                    this.autoModifyTimeout = true;
                    parameters.setTimeoutMillis(autoModifiedTimeout);
                    Long taskId = FlowTaskUtil.getTaskId(execution);
                    TaskEntity databaseChangeTaskEntity = taskService.detail(taskId);
                    databaseChangeTaskEntity.setParametersJson(JsonUtils.toJson(parameters));
                    taskService.updateParametersJson(databaseChangeTaskEntity);
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Error occurs while modify database change task timeout if time-consuming SQL exists", e);
        } finally {
            if (uploadFileInputStream != null) {
                try {
                    uploadFileInputStream.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }

    }

    /**
     * Check whether there is any SQL that may be time-consuming such as creating indexes, modifying
     * primary key, etc.
     *
     * @param statement SQL parse statement
     * @return true if involves time-consuming SQL, otherwise false
     */
    private boolean checkTimeConsumingSql(Statement statement) {
        if (statement instanceof AlterTable) {
            return ((AlterTable) statement).getAlterTableActions().stream().anyMatch(action -> {
                if (action.getAddIndex() != null || action.getModifyPrimaryKey() != null) {
                    return true;
                } else if (action.getAddConstraint() != null) {
                    OutOfLineConstraint addConstraint = action.getAddConstraint();
                    return addConstraint.isPrimaryKey() || addConstraint.isUniqueKey();
                } else {
                    return false;
                }
            });
        } else if (statement instanceof CreateIndex) {
            return true;
        }
        return false;
    }

}
