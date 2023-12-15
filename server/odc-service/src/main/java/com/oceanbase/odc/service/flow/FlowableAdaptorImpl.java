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
package com.oceanbase.odc.service.flow;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.flowable.engine.FormService;
import org.flowable.engine.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.flow.model.FlowableElement;
import com.oceanbase.odc.core.flow.model.FlowableElementType;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.flow.FlowInstanceSpecs;
import com.oceanbase.odc.metadb.flow.GateWayInstanceEntity;
import com.oceanbase.odc.metadb.flow.GateWayInstanceRepository;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntity;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntityRepository;
import com.oceanbase.odc.metadb.flow.SequenceInstanceRepository;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceEntity;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceCandidateRepository;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceEntity;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceRepository;
import com.oceanbase.odc.service.flow.instance.BaseFlowNodeInstance;
import com.oceanbase.odc.service.flow.instance.FlowApprovalInstance;
import com.oceanbase.odc.service.flow.instance.FlowGatewayInstance;
import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;
import com.oceanbase.odc.service.flow.model.FlowNodeType;
import com.oceanbase.odc.service.flow.task.mapper.OdcRuntimeDelegateMapper;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation for {@link FlowableAdaptor}, Implementation based on database query
 *
 * @author yh263208
 * @date 2022-02-17 20:16
 * @since ODC_release_3.3.0
 * @see FlowableAdaptor
 */
@Slf4j
@Service
@SkipAuthorize("odc internal usage")
public class FlowableAdaptorImpl implements FlowableAdaptor {

    @Autowired
    private NodeInstanceEntityRepository nodeInstanceRepository;
    @Autowired
    private ServiceTaskInstanceRepository serviceTaskInstanceRepository;
    @Autowired
    private EventPublisher eventPublisher;
    @Autowired
    private SequenceInstanceRepository sequenceRepository;
    @Autowired
    private FlowInstanceRepository flowInstanceRepository;
    @Autowired
    private UserTaskInstanceRepository userTaskInstanceRepository;
    @Autowired
    private TaskService taskService;
    @Autowired
    private FormService formService;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private GateWayInstanceRepository gateWayInstanceRepository;
    @Autowired
    private UserTaskInstanceCandidateRepository userTaskInstanceCandidateRepository;

    @Override
    public void setProcessInstanceId(@NonNull Long flowInstanceId, @NonNull String processInstanceId) {
        Optional<FlowInstanceEntity> optional = flowInstanceRepository.findById(flowInstanceId);
        PreConditions.validExists(ResourceType.ODC_FLOW_INSTANCE, "Id", flowInstanceId, optional::isPresent);
        int affectRows = flowInstanceRepository.updateProcessInstanceIdById(flowInstanceId, processInstanceId);
        log.info("Binding Flow/Process Instance Id exception, flowInstanceId={}, processInstanceId={}, affectRows={}",
                flowInstanceId, processInstanceId, affectRows);
        Verify.verify(affectRows == 1, "AffectRows has to be equal to one");
    }

    @Override
    public Optional<Long> getFlowInstanceIdByProcessInstanceId(@NonNull String processInstanceId) {
        Specification<FlowInstanceEntity> specification =
                Specification.where(FlowInstanceSpecs.processInstanceIdEquals(processInstanceId));
        List<FlowInstanceEntity> entityList = flowInstanceRepository.findAll(specification);
        if (entityList.isEmpty()) {
            return Optional.empty();
        }
        Verify.verify(entityList.size() == 1,
                "A process instance Id " + processInstanceId + " is associated with multiple flow instance Ids "
                        + entityList.stream().map(FlowInstanceEntity::getId).collect(Collectors.toList()));
        FlowInstanceEntity entity = entityList.get(0);
        if (entity.getId() == null) {
            return Optional.empty();
        }
        return Optional.of(entity.getId());
    }

    @Override
    public void setProcessDefinitionId(@NonNull Long flowInstanceId,
            @NonNull String processDefinitionId) {
        Optional<FlowInstanceEntity> optional = flowInstanceRepository.findById(flowInstanceId);
        PreConditions.validExists(ResourceType.ODC_FLOW_INSTANCE, "Id", flowInstanceId, optional::isPresent);
        int affectRows = flowInstanceRepository.updateProcessDefinitionIdById(flowInstanceId, processDefinitionId);
        log.info("Binding Flow/Process Instance Id exception, flowInstanceId={}, processDefinitionId={}, affectRows={}",
                flowInstanceId, processDefinitionId, affectRows);
        Verify.verify(affectRows == 1, "AffectRows has to be equal to one");
    }

    @Override
    public Optional<Long> getFlowInstanceIdByProcessDefinitionId(@NonNull String processDefinitionId) {
        Specification<FlowInstanceEntity> specification =
                Specification.where(FlowInstanceSpecs.processDefinitionIdEquals(processDefinitionId));
        List<FlowInstanceEntity> entityList = flowInstanceRepository.findAll(specification);
        if (entityList.isEmpty()) {
            return Optional.empty();
        }
        Verify.verify(entityList.size() == 1,
                "A process definition Id " + processDefinitionId + " is associated with multiple flow instance Ids "
                        + entityList.stream().map(FlowInstanceEntity::getId).collect(Collectors.toList()));
        FlowInstanceEntity entity = entityList.get(0);
        if (entity.getId() == null) {
            return Optional.empty();
        }
        return Optional.of(entity.getId());
    }

    @Override
    public Optional<String> getProcessInstanceIdByFlowInstanceId(@NonNull Long flowInstanceId) {
        Optional<FlowInstanceEntity> optional = flowInstanceRepository.findById(flowInstanceId);
        if (!optional.isPresent()) {
            return Optional.empty();
        }
        String processInstanceId = optional.get().getProcessInstanceId();
        if (processInstanceId == null) {
            return Optional.empty();
        }
        return Optional.of(processInstanceId);
    }

    @Override
    public List<FlowableElement> getFlowableElementByType(@NonNull Long instanceId, @NonNull FlowNodeType instanceType,
            @NonNull FlowableElementType flowableElementType) {
        List<NodeInstanceEntity> optional = nodeInstanceRepository
                .findByInstanceIdAndInstanceTypeAndFlowableElementType(instanceId, instanceType,
                        flowableElementType);
        if (optional.isEmpty()) {
            return Collections.emptyList();
        }
        return optional.stream().map(
                entity -> new FlowableElement(entity.getActivityId(), entity.getName(),
                        entity.getFlowableElementType()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FlowTaskInstance> getTaskInstanceByActivityId(@NonNull String activityId,
            @NonNull Long flowInstanceId) {
        Optional<ServiceTaskInstanceEntity> optional = serviceTaskInstanceRepository
                .findByInstanceTypeAndActivityId(FlowNodeType.SERVICE_TASK, activityId, flowInstanceId);
        return innerConvert(optional, this,
                (entity, flowService) -> new FlowTaskInstance(entity, new OdcRuntimeDelegateMapper(), flowService,
                        eventPublisher, taskService, nodeInstanceRepository, sequenceRepository,
                        serviceTaskInstanceRepository));
    }

    @Override
    public Optional<FlowApprovalInstance> getApprovalInstanceByActivityId(@NonNull String activityId,
            @NonNull Long flowInstanceId) {
        Optional<UserTaskInstanceEntity> optional =
                userTaskInstanceRepository.findByInstanceTypeAndActivityId(FlowNodeType.APPROVAL_TASK, activityId,
                        flowInstanceId);
        return innerConvert(optional, this,
                (entity, flowService) -> new FlowApprovalInstance(entity, flowService, taskService, formService,
                        eventPublisher, authenticationFacade, nodeInstanceRepository, sequenceRepository,
                        userTaskInstanceRepository, userTaskInstanceCandidateRepository));
    }

    @Override
    public Optional<FlowApprovalInstance> getApprovalInstanceByName(@NonNull String name,
            @NonNull Long flowInstanceId) {
        Optional<UserTaskInstanceEntity> optional =
                userTaskInstanceRepository.findByInstanceTypeAndName(FlowNodeType.APPROVAL_TASK, name, flowInstanceId);
        return innerConvert(optional, this,
                (entity, flowService) -> new FlowApprovalInstance(entity, flowService, taskService, formService,
                        eventPublisher, authenticationFacade, nodeInstanceRepository, sequenceRepository,
                        userTaskInstanceRepository, userTaskInstanceCandidateRepository));
    }

    @Override
    public Optional<FlowGatewayInstance> getGatewayInstanceByActivityId(@NonNull String activityId,
            @NonNull Long flowInstanceId) {
        Optional<GateWayInstanceEntity> optional =
                gateWayInstanceRepository.findByInstanceTypeAndActivityId(FlowNodeType.GATEWAY, activityId,
                        flowInstanceId);
        return innerConvert(optional, this,
                (gateway, flowService) -> new FlowGatewayInstance(gateway, flowService, nodeInstanceRepository,
                        sequenceRepository, gateWayInstanceRepository));
    }

    @Override
    public void setFlowableElements(@NonNull List<Pair<BaseFlowNodeInstance, FlowableElement>> elements) {
        Set<Long> ids = elements.stream().map(p -> p.left.getId()).collect(Collectors.toSet());
        List<NodeInstanceEntity> entities = this.nodeInstanceRepository.findByInstanceIdIn(ids);
        List<NodeInstanceEntity> nodes = elements.stream()
                .filter(p -> entities.stream()
                        .noneMatch(e -> Objects.equals(e.getFlowableElementType(), p.right.getType())
                                && (Objects.equals(e.getName(), p.right.getName())
                                        || Objects.equals(e.getActivityId(), p.right.getActivityId()))))
                .map(p -> {
                    NodeInstanceEntity entity = new NodeInstanceEntity();
                    entity.setActivityId(p.right.getActivityId());
                    entity.setName(p.right.getName());
                    entity.setFlowableElementType(p.right.getType());
                    entity.setInstanceType(p.left.getNodeType());
                    entity.setInstanceId(p.left.getId());
                    entity.setFlowInstanceId(p.left.getFlowInstanceId());
                    return entity;
                }).collect(Collectors.toList());
        this.nodeInstanceRepository.batchCreate(nodes);
    }

    private <T, V> Optional<T> innerConvert(@NonNull Optional<V> optional, @NonNull FlowableAdaptor flowableAdaptor,
            @NonNull BiFunction<V, FlowableAdaptor, T> function) {
        if (!optional.isPresent()) {
            return Optional.empty();
        }
        T value = function.apply(optional.get(), flowableAdaptor);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

}
