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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.oceanbase.odc.common.i18n.I18n;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.databasechange.MultipleDatabaseChangeTraceContextHolder;
import com.oceanbase.odc.service.databasechange.model.DatabaseChangeDatabase;
import com.oceanbase.odc.service.databasechange.model.DatabaseChangeFlowInstanceDetailResp;
import com.oceanbase.odc.service.databasechange.model.DatabaseChangingRecord;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.model.FlowInstanceDetailResp;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.model.FlowTaskExecutionStrategy;
import com.oceanbase.odc.service.flow.model.QueryFlowInstanceParams;
import com.oceanbase.odc.service.flow.task.model.MultipleDatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.MultipleDatabaseChangeTaskResult;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;
import com.oceanbase.odc.service.task.TaskService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: zijia.cj
 * @date: 2024/3/29
 */
@Slf4j
public class MultipleDatabaseChangeRuntimeFlowableTask extends BaseODCFlowTaskDelegate<Void> {

    private volatile boolean isSuccessful = false;
    private volatile boolean isFailure = false;
    private volatile boolean isFinished = false;
    private volatile User taskCreator;
    private volatile MultipleDatabaseChangeParameters multipleDatabaseChangeParameters;
    private volatile Integer batchId;
    private volatile FlowTaskExecutionStrategy flowTaskExecutionStrategy;
    private Integer batchSum;

    @Autowired
    private FlowInstanceService flowInstanceService;
    @Autowired
    private FlowInstanceRepository flowInstanceRepository;
    @Autowired
    private ServiceTaskInstanceRepository serviceTaskInstanceRepository;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning, Long taskId, TaskService taskService) {
        // todo implement later
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    protected boolean isSuccessful() {
        if (this.flowTaskExecutionStrategy == null) {
            return this.isSuccessful;
        }
        return isFinished
                ? isContinue(this.flowTaskExecutionStrategy, multipleDatabaseChangeParameters.getAutoErrorStrategy())
                : false;
    }

    @Override
    protected boolean isFailure() {
        if (this.flowTaskExecutionStrategy == null) {
            return this.isFailure;
        }
        return isFinished
                ? !isContinue(this.flowTaskExecutionStrategy, multipleDatabaseChangeParameters.getAutoErrorStrategy())
                : false;
    }

    @Override
    protected Void start(Long taskId, TaskService taskService, DelegateExecution execution)
            throws InterruptedException {
        MultipleDatabaseChangeTraceContextHolder.trace(taskId);
        try {
            this.flowInstanceRepository.updateStatusById(getFlowInstanceId(), FlowStatus.EXECUTING);
            this.serviceTaskInstanceRepository.updateStatusById(getTargetTaskInstanceId(), FlowNodeStatus.EXECUTING);
            FlowInstanceDetailResp flowInstanceDetailResp = flowInstanceService.detail(getFlowInstanceId());
            this.flowTaskExecutionStrategy = flowInstanceDetailResp.getExecutionStrategy();
            TaskEntity detail = taskService.detail(taskId);
            this.multipleDatabaseChangeParameters = JsonUtils.fromJson(
                    detail.getParametersJson(), MultipleDatabaseChangeParameters.class);
            Integer value = multipleDatabaseChangeParameters.getBatchId();
            if (value == null) {
                this.batchId = 0;
            } else {
                this.batchId = value;
            }
            log.info("Multiple database task start, taskId={}, batchId={}", taskId,
                    this.batchId + 1);
            multipleDatabaseChangeParameters.setBatchId(this.batchId + 1);
            detail.setParametersJson(JsonUtils.toJson(multipleDatabaseChangeParameters));
            taskService.updateParametersJson(detail);
            this.batchSum = multipleDatabaseChangeParameters.getOrderedDatabaseIds().size();
            List<Long> batchDatabaseIds =
                    multipleDatabaseChangeParameters.getOrderedDatabaseIds().get(this.batchId);
            List<Long> flowInstanceIds = new ArrayList<>();
            this.taskCreator = FlowTaskUtil.getTaskCreator(execution);
            Map<Long, DatabaseChangeDatabase> map = multipleDatabaseChangeParameters.getDatabases().stream()
                    .collect(Collectors.toMap(DatabaseChangeDatabase::getId, Function.identity()));
            Locale locale = multipleDatabaseChangeParameters.getLocale();
            for (Long batchDatabaseId : batchDatabaseIds) {
                CreateFlowInstanceReq createFlowInstanceReq = new CreateFlowInstanceReq();
                createFlowInstanceReq.setDatabaseId(batchDatabaseId);
                createFlowInstanceReq.setTaskType(TaskType.ASYNC);
                createFlowInstanceReq.setExecutionStrategy(FlowTaskExecutionStrategy.AUTO);
                createFlowInstanceReq.setParentFlowInstanceId(FlowTaskUtil.getFlowInstanceId(execution));
                createFlowInstanceReq.setParameters(multipleDatabaseChangeParameters
                        .convertIntoDatabaseChangeParameters(multipleDatabaseChangeParameters));
                createFlowInstanceReq.setDescription(
                        generateDescription(locale,map.get(batchDatabaseId), getFlowInstanceId(), this.batchId));
                List<FlowInstanceDetailResp> individualFlowInstance = flowInstanceService.createWithoutApprovalNode(
                        createFlowInstanceReq);
                flowInstanceIds.add(individualFlowInstance.get(0).getId());
            }

            long originalTime = System.currentTimeMillis();
            boolean flagForTaskSucceed = true;
            // todo 待优化，做成异步回调，减少阻塞和查数据库的次数。
            while (System.currentTimeMillis() - originalTime <= multipleDatabaseChangeParameters.getTimeoutMillis()) {
                int numberForEndLoop = 0;
                List<FlowInstanceEntity> flowInstanceEntityList = flowInstanceService.listByIds(flowInstanceIds);
                for (FlowInstanceEntity flowInstanceEntity : flowInstanceEntityList) {
                    if (flowInstanceEntity != null) {
                        switch (flowInstanceEntity.getStatus()) {
                            case EXECUTION_SUCCEEDED:
                                numberForEndLoop++;
                                break;
                            case EXECUTION_FAILED:
                            case EXECUTION_EXPIRED:
                            case CANCELLED:
                                flagForTaskSucceed = false;
                                numberForEndLoop++;
                                break;
                            default:
                                break;
                        }
                    }
                }
                if (numberForEndLoop == multipleDatabaseChangeParameters.getOrderedDatabaseIds().get(
                        this.batchId).size()) {
                    break;
                }
                Thread.sleep(1000);
            }
            // Check whether the current batch database change is successfully initiated
            if (flagForTaskSucceed) {
                this.isFailure = false;
                this.isSuccessful = true;
            } else {
                this.isFailure = true;
                this.isSuccessful = false;
            }
            return null;
        } catch (Exception e) {
            log.warn("Multiple database task failed, taskId={}, batchId={}", taskId,
                    this.batchId == null ? null : this.batchId + 1, e);
            this.isFailure = true;
            this.isSuccessful = false;
            throw e;
        } finally {
            this.isFinished = true;
            MultipleDatabaseChangeTraceContextHolder.clear();
        }
    }

    @Override
    protected void onFailure(Long taskId, TaskService taskService) {
        try {
            MultipleDatabaseChangeTraceContextHolder.trace(taskId);
            log.warn("Multiple database task failed, taskId={}, batchId={}", taskId,
                    this.batchId == null ? null : this.batchId + 1);
            updateFlowInstanceStatus(FlowStatus.EXECUTION_FAILED);
            taskService.fail(taskId, 100, generateResult());
        } finally {
            MultipleDatabaseChangeTraceContextHolder.clear();
            super.onFailure(taskId, taskService);
        }
    }

    @Override
    protected void onSuccessful(Long taskId, TaskService taskService) {
        try {
            MultipleDatabaseChangeTraceContextHolder.trace(taskId);
            log.info("Multiple database task succeed, taskId={}, batchId={}", taskId, this.batchId + 1);
            if (this.batchId == batchSum - 1) {
                List<FlowInstanceEntity> list = flowInstanceService.getFlowInstanceByParentId(getFlowInstanceId());
                if (list.stream().allMatch(e -> FlowStatus.EXECUTION_SUCCEEDED == e.getStatus())) {
                    updateFlowInstanceStatus(FlowStatus.EXECUTION_SUCCEEDED);
                } else {
                    updateFlowInstanceStatus(FlowStatus.EXECUTION_FAILED);
                }
                taskService.succeed(taskId, generateResult());
            } else {
                TaskEntity taskEntity = taskService.detail(taskId);
                taskEntity.setResultJson(JsonUtils.toJson(generateResult()));
                taskEntity.setProgressPercentage((this.batchId + 1) * 100D / this.batchSum);
                taskService.update(taskEntity);
            }
            super.onSuccessful(taskId, taskService);
            if (!this.isSuccessful) {
                this.serviceTaskInstanceRepository.updateStatusById(getTargetTaskInstanceId(), FlowNodeStatus.FAILED);
            } else {
                this.serviceTaskInstanceRepository.updateStatusById(getTargetTaskInstanceId(),
                        FlowNodeStatus.COMPLETED);
            }
        } catch (Exception e) {
            log.warn("Multiple database task failed, taskId={}, batchId={}", taskId,
                    this.batchId == null ? null : this.batchId + 1, e);
        } finally {
            MultipleDatabaseChangeTraceContextHolder.clear();
        }
    }

    @Override
    protected void onTimeout(Long taskId, TaskService taskService) {
        try {
            MultipleDatabaseChangeTraceContextHolder.trace(taskId);
            taskService.fail(taskId, 100, generateResult());
            log.warn("Multiple database task timeout, taskId={}, batchId={}", taskId,
                    this.batchId == null ? null : this.batchId + 1);
        } finally {
            MultipleDatabaseChangeTraceContextHolder.clear();
        }
        super.onTimeout(taskId, taskService);
    }

    @Override
    protected void onProgressUpdate(Long taskId, TaskService taskService) {
        if (this.taskCreator != null) {
            try {
                SecurityContextUtils.setCurrentUser(this.taskCreator);
                MultipleDatabaseChangeTaskResult multipleDatabaseChangeTaskResult = generateResult();
                if (multipleDatabaseChangeTaskResult != null) {
                    taskService.updateResult(taskId, multipleDatabaseChangeTaskResult);
                }
            } catch (Exception e) {
                log.warn("Failed to update multiple database task progress, taskId={}, batchId={}", taskId,
                        this.batchId == null ? null : this.batchId + 1, e);
            } finally {
                SecurityContextUtils.clear();
            }
        }
    }

    private MultipleDatabaseChangeTaskResult generateResult() {
        if (this.multipleDatabaseChangeParameters == null) {
            return null;
        }
        MultipleDatabaseChangeTaskResult result = new MultipleDatabaseChangeTaskResult();
        Long flowInstanceId = getFlowInstanceId();
        QueryFlowInstanceParams param = QueryFlowInstanceParams.builder().parentInstanceId(flowInstanceId)
                .build();
        Page<FlowInstanceDetailResp> page = flowInstanceService.list(Pageable.unpaged(), param);
        List<FlowInstanceDetailResp> flowInstanceDetailRespList = page.getContent();
        if (flowInstanceDetailRespList.isEmpty()) {
            return null;
        }
        // todo At present multi-databases changes do not include databases with the same name
        Map<Long, FlowInstanceDetailResp> databaseId2FlowInstanceDetailResp =
                flowInstanceDetailRespList.stream().collect(
                        Collectors.toMap(flowInstanceDetailResp -> flowInstanceDetailResp.getDatabase().getId(),
                                flowInstanceDetailResp -> flowInstanceDetailResp));
        List<DatabaseChangeDatabase> databases = this.multipleDatabaseChangeParameters.getDatabases();
        List<DatabaseChangingRecord> records = new ArrayList<>();
        for (DatabaseChangeDatabase database : databases) {
            FlowInstanceDetailResp resp = databaseId2FlowInstanceDetailResp.get(database.getId());
            DatabaseChangingRecord record = new DatabaseChangingRecord();
            record.setDatabase(database);
            record.setFlowInstanceDetailResp(resp != null ? new DatabaseChangeFlowInstanceDetailResp(resp) : null);
            record.setStatus(resp != null ? resp.getStatus() : null);
            records.add(record);
        }
        result.setDatabaseChangingRecordList(records);
        return result;
    }

    private Boolean isContinue(FlowTaskExecutionStrategy flowTaskExecutionStrategy,
            TaskErrorStrategy autoTaskErrorStrategy) {
        if (flowTaskExecutionStrategy == FlowTaskExecutionStrategy.MANUAL
                || (flowTaskExecutionStrategy == FlowTaskExecutionStrategy.AUTO
                        && autoTaskErrorStrategy == TaskErrorStrategy.CONTINUE)) {
            return true;
        } else {
            return this.isSuccessful;
        }
    }

    @Override
    public List<Class<? extends ExecutionListener>> getExecutionListenerClasses() {
        return null;
    }

    public String generateDescription(Locale locale, DatabaseChangeDatabase database, Long flowInstanceId, Integer batchId) {
        String i18nKey = "com.oceanbase.odc.builtin-resource.multiple-async.sub-ticket.description";
        String envName = database.getEnvironment().getName();
        String envKey = envName.substring(2, envName.length() - 1);
        String description = I18n.translate(
            i18nKey,
            new Object[] {I18n.translate(envKey,null,envKey,locale) , flowInstanceId, batchId + 1,
                          database.getDataSource().getName(), database.getName()}, locale);
        return description;
    }
}
