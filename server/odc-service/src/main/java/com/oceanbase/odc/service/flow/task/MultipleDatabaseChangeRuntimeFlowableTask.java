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
import java.util.Map;
import java.util.stream.Collectors;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.databasechange.MultipleDatabaseChangeTraceContextHolder;
import com.oceanbase.odc.service.databasechange.model.DatabaseChangingRecord;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.model.FlowInstanceDetailResp;
import com.oceanbase.odc.service.flow.model.FlowTaskExecutionStrategy;
import com.oceanbase.odc.service.flow.model.QueryFlowInstanceParams;
import com.oceanbase.odc.service.flow.task.model.MultipleDatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.MultipleDatabaseChangeTaskResult;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
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

    private Integer batchId;

    private Integer batchSum;
    List<List<Long>> orderedDatabaseIds;

    @Autowired
    private FlowInstanceService flowInstanceService;

    @Autowired
    private ServiceTaskInstanceRepository serviceTaskInstanceRepository;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private DatabaseService databaseService;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning, Long taskId, TaskService taskService) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    protected Void start(Long taskId, TaskService taskService, DelegateExecution execution)
            throws InterruptedException {
        MultipleDatabaseChangeTraceContextHolder.trace(authenticationFacade.currentUser().getId(), taskId);
        try {
            TaskEntity detail = taskService.detail(taskId);
            MultipleDatabaseChangeParameters multipleDatabaseChangeParameters = JsonUtils.fromJson(
                    detail.getParametersJson(), MultipleDatabaseChangeParameters.class);
            this.orderedDatabaseIds = multipleDatabaseChangeParameters.getOrderedDatabaseIds();
            Integer value = multipleDatabaseChangeParameters.getBatchId();
            if (value == null) {
                this.batchId = 0;
            } else {
                this.batchId = value;
            }
            log.info("multiple database task start, taskId={}, batchId={}", taskId,
                    this.batchId + 1);
            multipleDatabaseChangeParameters.setBatchId(this.batchId + 1);
            detail.setParametersJson(JsonUtils.toJson(multipleDatabaseChangeParameters));
            taskService.updateParametersJson(detail);
            this.batchSum = multipleDatabaseChangeParameters.getOrderedDatabaseIds().size();
            List<Long> batchDatabaseIds =
                    multipleDatabaseChangeParameters.getOrderedDatabaseIds().get(this.batchId);
            List<Long> flowInstanceIds = new ArrayList<>();
            for (Long batchDatabaseId : batchDatabaseIds) {
                CreateFlowInstanceReq createFlowInstanceReq = new CreateFlowInstanceReq();
                createFlowInstanceReq.setDatabaseId(batchDatabaseId);
                createFlowInstanceReq.setTaskType(TaskType.ASYNC);
                createFlowInstanceReq.setExecutionStrategy(FlowTaskExecutionStrategy.AUTO);
                createFlowInstanceReq.setParentFlowInstanceId(FlowTaskUtil.getFlowInstanceId(execution));
                createFlowInstanceReq.setParameters(multipleDatabaseChangeParameters
                        .convertIntoDatabaseChangeParameters(multipleDatabaseChangeParameters));
                List<FlowInstanceDetailResp> individualFlowInstance = flowInstanceService.createIndividualFlowInstance(
                        createFlowInstanceReq);
                flowInstanceIds.add(individualFlowInstance.get(0).getId());
            }

            long originalTime = System.currentTimeMillis();
            boolean flagForTaskSucceed = true;
            while (System.currentTimeMillis() - originalTime <= multipleDatabaseChangeParameters.getTimeoutMillis()) {
                int numberForEndLoop = 0;
                for (Long flowInstanceId : flowInstanceIds) {
                    FlowInstanceDetailResp detailById = flowInstanceService.detail(flowInstanceId);
                    if (detailById != null) {
                        switch (detailById.getStatus()) {
                            case EXECUTION_SUCCEEDED:
                                numberForEndLoop++;
                                break;
                            case EXECUTION_FAILED:
                            case EXECUTION_EXPIRED:
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

            if (flagForTaskSucceed) {
                this.isFailure = false;
                this.isSuccessful = true;
            } else {
                this.isFailure = true;
                this.isSuccessful = false;
            }
            return null;
        } catch (Exception e) {
            log.warn("multiple database task failed, taskId={}, batchId={}", taskId,
                    this.batchId == null ? null : this.batchId + 1, e);
            this.isFailure = true;
            this.isSuccessful = false;
            throw e;
        } finally {
            MultipleDatabaseChangeTraceContextHolder.clear();
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
        try {
            log.warn("multiple database task failed, taskId={}, batchId={}", taskId,
                    this.batchId == null ? null : this.batchId + 1);
            updateFlowInstanceStatus(FlowStatus.EXECUTION_FAILED);
            super.onFailure(taskId, taskService);
        } finally {
            MultipleDatabaseChangeTraceContextHolder.clear();
        }
    }

    @Override
    protected void onSuccessful(Long taskId, TaskService taskService) {
        try {
            log.info("multiple database task succeed, taskId={}, batchId={}", taskId, this.batchId + 1);
            if (this.batchId == batchSum - 1) {
                List<FlowInstanceEntity> flowInstanceByParentId = flowInstanceService.getFlowInstanceByParentId(
                        getFlowInstanceId());
                boolean allSucceeded = flowInstanceByParentId.stream()
                        .map(FlowInstanceEntity::getId)
                        .map(flowInstanceService::detail)
                        .allMatch(detail -> FlowStatus.EXECUTION_SUCCEEDED.equals(detail.getStatus()));
                if (allSucceeded) {
                    updateFlowInstanceStatus(FlowStatus.EXECUTION_SUCCEEDED);
                } else {
                    updateFlowInstanceStatus(FlowStatus.EXECUTION_FAILED);
                }
            }
            taskService.succeed(taskId, generateResult(true));
            super.onSuccessful(taskId, taskService);
        } catch (Exception e) {
            log.warn("multiple database task failed, taskId={}, batchId={}", taskId,
                    this.batchId == null ? null : this.batchId + 1, e);
        } finally {
            MultipleDatabaseChangeTraceContextHolder.clear();
        }
    }

    @Override
    protected void onTimeout(Long taskId, TaskService taskService) {
        try {
            taskService.fail(taskId, 100, generateResult(false));
            log.warn("multiple database task timeout, taskId={}, batchId={}", taskId,
                    this.batchId == null ? null : this.batchId + 1);
        } finally {
            MultipleDatabaseChangeTraceContextHolder.clear();
        }
        super.onTimeout(taskId, taskService);
    }

    @Override
    protected void onProgressUpdate(Long taskId, TaskService taskService) {}

    private MultipleDatabaseChangeTaskResult generateResult(boolean success) {
        MultipleDatabaseChangeTaskResult result = new MultipleDatabaseChangeTaskResult();
        Long flowInstanceId = getFlowInstanceId();
        QueryFlowInstanceParams param = QueryFlowInstanceParams.builder().parentInstanceId(flowInstanceId)
                .build();
        Page<FlowInstanceDetailResp> page = flowInstanceService.list(Pageable.unpaged(), param);
        List<FlowInstanceDetailResp> flowInstanceDetailRespList = page.getContent();
        Map<Long, FlowInstanceDetailResp> map = flowInstanceDetailRespList.stream().collect(
                Collectors.toMap(flowInstanceDetailResp -> flowInstanceDetailResp.getDatabase().getId(),
                        flowInstanceDetailResp -> flowInstanceDetailResp));
        List<Long> idList = this.orderedDatabaseIds.stream().flatMap(x -> x.stream()).collect(Collectors.toList());
        List<Database> databaseList = databaseService.detailForMultipleDatabase(idList);
        ArrayList<DatabaseChangingRecord> databaseChangingRecords = new ArrayList<>();
        for (Database database : databaseList) {
            DatabaseChangingRecord databaseChangingRecord = new DatabaseChangingRecord();
            databaseChangingRecord.setDatabase(database);
            if (map.containsKey(database.getId())) {
                databaseChangingRecord.setFlowInstanceDetailResp(map.get(database.getId()));
                databaseChangingRecord.setStatus(map.get(database.getId()).getStatus());
            } else {
                databaseChangingRecord.setStatus(FlowStatus.WAIT_FOR_EXECUTION);
            }
            databaseChangingRecords.add(databaseChangingRecord);
        }
        result.setDatabaseChangingRecordList(databaseChangingRecords);
        result.setSuccess(success);
        return result;
    }
}
