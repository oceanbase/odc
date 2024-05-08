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
package com.oceanbase.odc.service.flow.instance;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;

import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.core.flow.model.FlowableElement;
import com.oceanbase.odc.core.flow.model.FlowableElementType;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntityRepository;
import com.oceanbase.odc.metadb.flow.SequenceInstanceRepository;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceEntity;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.event.ServiceTaskStartedListener;
import com.oceanbase.odc.service.flow.event.TaskInstanceCreatedEvent;
import com.oceanbase.odc.service.flow.model.ExecutionStrategyConfig;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.model.FlowNodeType;
import com.oceanbase.odc.service.flow.model.FlowTaskExecutionStrategy;
import com.oceanbase.odc.service.flow.task.BaseRuntimeFlowableDelegate;
import com.oceanbase.odc.service.flow.task.mapper.RuntimeDelegateMapper;
import com.oceanbase.odc.service.flow.task.model.RuntimeTaskConstants;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Flow node instance, used to encapsulate asynchronous tasks
 *
 * @author yh263208
 * @date 2022-02-14 14:40
 * @since ODC_release_3.3.0
 */
@Slf4j
public class FlowTaskInstance extends BaseFlowNodeInstance {

    public static final String ABORT_VARIABLE_NAME = "aborted";
    @Setter
    @Getter
    private Long targetTaskId;
    @Getter
    private final ExecutionStrategyConfig strategyConfig;
    @Getter
    private final TaskType taskType;
    private final ServiceTaskInstanceRepository serviceTaskRepository;
    private final TaskService taskService;
    private final EventPublisher eventPublisher;
    private final RuntimeDelegateMapper delegateConvertor;
    private volatile BaseRuntimeFlowableDelegate<?> targetTaskHandle;
    private ServiceTaskStartedListener listener;

    public FlowTaskInstance(@NonNull ServiceTaskInstanceEntity entity, @NonNull RuntimeDelegateMapper convertor,
            @NonNull FlowableAdaptor flowableAdaptor,
            @NonNull EventPublisher eventPublisher,
            @NonNull TaskService taskService,
            @NonNull NodeInstanceEntityRepository nodeRepository,
            @NonNull SequenceInstanceRepository sequenceRepository,
            @NonNull ServiceTaskInstanceRepository serviceTaskRepository) {
        super(FlowNodeType.SERVICE_TASK, entity.getId(), entity.getOrganizationId(), entity.getStatus(),
                entity.getFlowInstanceId(), entity.getCreateTime(), entity.getUpdateTime(), entity.isStartEndpoint(),
                entity.isEndEndpoint(), flowableAdaptor, nodeRepository, sequenceRepository);
        setTargetTaskId(entity.getTargetTaskId());
        this.strategyConfig = ExecutionStrategyConfig.from(entity);
        this.taskService = taskService;
        this.serviceTaskRepository = serviceTaskRepository;
        this.eventPublisher = eventPublisher;
        this.taskType = entity.getTaskType();
        this.delegateConvertor = convertor;
        alloc();
        eventPublisher.publishEvent(new TaskInstanceCreatedEvent(this));
    }

    public FlowTaskInstance(@NonNull TaskType taskType, @NonNull Long organizationId, @NonNull Long flowInstanceId,
            @NonNull ExecutionStrategyConfig strategyConfig, boolean startEndpoint, boolean endEndPoint,
            @NonNull RuntimeDelegateMapper convertor,
            @NonNull FlowableAdaptor flowableAdaptor,
            @NonNull EventPublisher eventPublisher,
            @NonNull TaskService taskService,
            @NonNull NodeInstanceEntityRepository nodeRepository,
            @NonNull SequenceInstanceRepository sequenceRepository,
            @NonNull ServiceTaskInstanceRepository serviceTaskRepository) {
        super(FlowNodeType.SERVICE_TASK, organizationId, flowInstanceId, startEndpoint, endEndPoint,
                flowableAdaptor, nodeRepository, sequenceRepository);
        this.strategyConfig = strategyConfig;
        this.taskService = taskService;
        this.serviceTaskRepository = serviceTaskRepository;
        this.eventPublisher = eventPublisher;
        this.taskType = taskType;
        this.delegateConvertor = convertor;
        alloc();
        eventPublisher.publishEvent(new TaskInstanceCreatedEvent(this));
    }

    @Override
    public FlowableElementType getCoreFlowableElementType() {
        return FlowableElementType.SERVICE_TASK;
    }

    public static List<FlowTaskInstance> batchCreate(List<FlowTaskInstance> instances,
            @NonNull ServiceTaskInstanceRepository serviceTaskInstanceRepository) {
        if (CollectionUtils.isEmpty(instances)) {
            return Collections.emptyList();
        }
        List<ServiceTaskInstanceEntity> entities = instances.stream()
                .map(FlowTaskInstance::mapToTaskEntity).collect(Collectors.toList());
        entities = serviceTaskInstanceRepository.batchCreate(entities);
        for (int i = 0; i < instances.size(); i++) {
            instances.get(i).setId(entities.get(i).getId());
            instances.get(i).createTime = entities.get(i).getCreateTime();
            instances.get(i).updateTime = entities.get(i).getUpdateTime();
        }
        return instances;
    }

    @Override
    public void create() {
        validNotExists();
        ServiceTaskInstanceEntity entity = mapToTaskEntity(this);
        entity = serviceTaskRepository.save(entity);
        Verify.notNull(entity.getId(), "id");
        Verify.notNull(entity.getCreateTime(), "CreateTime");
        Verify.notNull(entity.getUpdateTime(), "UpdateTime");
        this.id = entity.getId();
        this.createTime = entity.getCreateTime();
        this.updateTime = entity.getUpdateTime();
        log.info("Create service task instance successfully, serviceTask={}", entity);
    }

    @Override
    public void update() {
        validExists();
        ServiceTaskInstanceEntity entity = new ServiceTaskInstanceEntity();
        entity.setId(getId());
        entity.setStatus(getStatus());
        entity.setTargetTaskId(getTargetTaskId());
        entity.setTaskType(getTaskType());
        int affectRows = serviceTaskRepository.update(entity);
        log.info("Update service task instance successfully, affectRows={}, serviceTask={}", affectRows, entity);
    }

    @Override
    protected void doDelete(@NonNull Long instanceId) {
        serviceTaskRepository.deleteById(instanceId);
        log.info("Delete service task instance successfully, serviceTaskInstanceId={}", instanceId);
    }

    public void bindServiceTask(@NonNull BaseRuntimeFlowableDelegate<?> target) {
        if (target.getTargetTaskInstanceId() == null || getId() == null
                || !Objects.equals(target.getTargetTaskInstanceId(), getId())) {
            return;
        }
        this.targetTaskHandle = target;
        setStatus(FlowNodeStatus.EXECUTING);
    }

    @Override
    public boolean isPresentOnThisMachine() {
        return this.targetTaskHandle != null;
    }

    @Override
    public void dealloc() {
        if (this.listener == null) {
            return;
        }
        this.eventPublisher.removeEventListener(this.listener);
        this.listener = null;
    }

    @Override
    public void alloc() {
        if (this.listener != null) {
            return;
        }
        this.listener = new ServiceTaskStartedListener(this);
        this.eventPublisher.addEventListener(this.listener);
    }

    public void confirmExecute() {
        complete(false);
    }

    public void abort() {
        complete(true);
    }

    public void cancel(boolean mayInterruptIfRunning) {
        if (!isPresentOnThisMachine()) {
            throw new UnsupportedOperationException("Task isn't on this machine");
        }
        try {
            this.targetTaskHandle.cancel(mayInterruptIfRunning);
        } catch (Exception e) {
            log.warn("Cancel task failed, instanceId={}, instanceType={}, taskType={}, taskId={}", getId(),
                    getNodeType(), getTaskType(), getTargetTaskId(), e);
            throw e;
        }
    }

    public boolean isDone() {
        if (!isPresentOnThisMachine()) {
            throw new UnsupportedOperationException("Task isn't on this machine");
        }
        return this.targetTaskHandle.isDone();
    }

    public Object getResult() throws InterruptedException, ExecutionException {
        if (!isPresentOnThisMachine()) {
            throw new UnsupportedOperationException("Task isn't on this machine");
        }
        return this.targetTaskHandle.get();
    }

    public Object getResult(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (!isPresentOnThisMachine()) {
            throw new UnsupportedOperationException("Task isn't on this machine");
        }
        return this.targetTaskHandle.get(timeout, unit);
    }

    @Override
    public String resourceType() {
        return ResourceType.ODC_FLOW_TASK_INSTANCE.name();
    }

    public Class<? extends BaseRuntimeFlowableDelegate<?>> getTargetDelegateClass() {
        return this.delegateConvertor.map(this.taskType);
    }

    private void complete(boolean aborted) {
        if (this.strategyConfig.getStrategy() == FlowTaskExecutionStrategy.AUTO) {
            throw new UnsupportedOperationException("Automatic tasks do not support this execution");
        }
        Verify.verify(getStatus() == FlowNodeStatus.PENDING, "Task status is illegal: " + getStatus());

        List<FlowableElement> elements =
                flowableAdaptor.getFlowableElementByType(getId(), getNodeType(), FlowableElementType.USER_TASK)
                        .stream().filter(t -> !t.getName().contains(RuntimeTaskConstants.CALLBACK_TASK))
                        .collect(Collectors.toList());
        Verify.verify(!elements.isEmpty(), "Can not find any user task related to task instance, id " + getId());
        log.info("Get the execution node of the task instance, instanceId={}, elements={}", getId(), elements);

        Optional<String> optional =
                flowableAdaptor.getProcessInstanceIdByFlowInstanceId(getFlowInstanceId());
        Verify.verify(optional.isPresent(),
                "Can not find process instance by flow instance id " + getFlowInstanceId());

        String pid = optional.get();
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(pid).list().stream()
                .filter(t -> elements.stream().anyMatch(e -> Objects.equals(e.getName(), t.getName())))
                .collect(Collectors.toList());
        Verify.verify(tasks.size() == elements.size(), "Some tasks is not found");
        Map<String, Object> variables = new HashMap<>();
        variables.putIfAbsent(ABORT_VARIABLE_NAME, aborted);
        tasks.forEach(task -> taskService.complete(task.getId(), variables));
        log.info("Execution node of the task instance has been executed, instanceId={}, names={}, aborted={}",
                getId(), tasks.stream().map(TaskInfo::getName).collect(Collectors.toList()), aborted);
    }

    private static ServiceTaskInstanceEntity mapToTaskEntity(FlowTaskInstance instance) {
        ServiceTaskInstanceEntity entity = new ServiceTaskInstanceEntity();
        entity.setOrganizationId(instance.getOrganizationId());
        entity.setStatus(instance.getStatus());
        entity.setFlowInstanceId(instance.getFlowInstanceId());
        entity.setStartEndpoint(instance.isStartEndpoint());
        entity.setEndEndpoint(instance.isEndEndPoint());
        entity.setStrategy(instance.strategyConfig.getStrategy());
        entity.setTaskType(instance.getTaskType());
        entity.setTargetTaskId(instance.getTargetTaskId());
        entity.setWaitExecExpireIntervalSeconds(instance.strategyConfig.getPendingExpireIntervalSeconds());
        entity.setExecutionTime(instance.strategyConfig.getExecutionTime());
        return entity;
    }

}
