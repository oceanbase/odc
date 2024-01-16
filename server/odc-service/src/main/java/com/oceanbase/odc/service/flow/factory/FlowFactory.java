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
package com.oceanbase.odc.service.flow.factory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flowable.engine.FormService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.VerifyException;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.flow.GateWayInstanceEntity;
import com.oceanbase.odc.metadb.flow.GateWayInstanceRepository;
import com.oceanbase.odc.metadb.flow.GateWayInstanceSpecs;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntity;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntityRepository;
import com.oceanbase.odc.metadb.flow.SequenceInstanceEntity;
import com.oceanbase.odc.metadb.flow.SequenceInstanceRepository;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceEntity;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceSpecs;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceCandidateRepository;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceEntity;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceRepository;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceSpecs;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.event.TaskInstanceCreatedEvent;
import com.oceanbase.odc.service.flow.instance.BaseFlowNodeInstance;
import com.oceanbase.odc.service.flow.instance.FlowApprovalInstance;
import com.oceanbase.odc.service.flow.instance.FlowGatewayInstance;
import com.oceanbase.odc.service.flow.instance.FlowInstance;
import com.oceanbase.odc.service.flow.instance.FlowNodeInstanceKey;
import com.oceanbase.odc.service.flow.instance.FlowSequenceInstance;
import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;
import com.oceanbase.odc.service.flow.instance.OdcFlowInstance;
import com.oceanbase.odc.service.flow.model.ExecutionStrategyConfig;
import com.oceanbase.odc.service.flow.task.mapper.OdcRuntimeDelegateMapper;
import com.oceanbase.odc.service.flow.util.FlowInstanceUtil;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

import lombok.NonNull;

/**
 * Factory to build {@link FlowInstance} {@link FlowApprovalInstance} {@link FlowTaskInstance}
 *
 * @author yh263208
 * @date 2022-03-03 22:05
 * @since ODC_release_3.3.0
 */
@Component
public class FlowFactory {

    @Autowired
    private FlowInstanceRepository flowInstanceRepository;
    @Autowired
    private FlowableAdaptor flowableAdaptor;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private NodeInstanceEntityRepository nodeRepository;
    @Autowired
    private SequenceInstanceRepository sequenceRepository;
    @Autowired
    private GateWayInstanceRepository gatewayInstanceRepository;
    @Autowired
    private TaskService flowableTaskService;
    @Autowired
    private FormService formService;
    @Autowired
    private EventPublisher eventPublisher;
    @Autowired
    private UserTaskInstanceRepository userTaskInstanceRepository;
    @Autowired
    private ServiceTaskInstanceRepository serviceTaskRepository;
    @Autowired
    private UserTaskInstanceCandidateRepository userTaskInstanceCandidateRepository;

    public FlowInstance generateFlowInstance(@NonNull String name, String description) {
        return new OdcFlowInstance(name, description, flowableAdaptor, authenticationFacade,
                flowInstanceRepository, nodeRepository, sequenceRepository, gatewayInstanceRepository,
                serviceTaskRepository, userTaskInstanceRepository, userTaskInstanceCandidateRepository,
                runtimeService, repositoryService);
    }

    public FlowInstance generateFlowInstance(@NonNull String name,
            Long parentFlowInstanceId, Long projectId, String description) {
        return new OdcFlowInstance(name, description, parentFlowInstanceId, projectId,
                flowableAdaptor, authenticationFacade, flowInstanceRepository, nodeRepository,
                sequenceRepository, gatewayInstanceRepository, serviceTaskRepository, userTaskInstanceRepository,
                userTaskInstanceCandidateRepository, runtimeService, repositoryService);
    }

    public FlowGatewayInstance generateFlowGatewayInstance(@NonNull Long flowInstanceId, boolean isStartEndPoint,
            boolean isEndEndPoint) {
        return new FlowGatewayInstance(authenticationFacade.currentOrganizationId(), flowInstanceId, isStartEndPoint,
                isEndEndPoint, flowableAdaptor, nodeRepository, sequenceRepository, gatewayInstanceRepository);
    }

    public FlowApprovalInstance generateFlowApprovalInstance(@NonNull Long flowInstanceId, boolean isStartEndPoint,
            boolean isEndEndPoint, boolean autoApprove, int expireIntervalSeconds, Long externalApprovalId) {
        Verify.verify(expireIntervalSeconds > 0, "ApprovalExpirationInterval can not be negative");
        return new FlowApprovalInstance(authenticationFacade.currentOrganizationId(), flowInstanceId,
                externalApprovalId, expireIntervalSeconds, isStartEndPoint, isEndEndPoint,
                autoApprove, flowableAdaptor, flowableTaskService, formService, eventPublisher, authenticationFacade,
                nodeRepository, sequenceRepository, userTaskInstanceRepository, userTaskInstanceCandidateRepository);
    }

    public FlowApprovalInstance generateFlowApprovalInstance(@NonNull Long flowInstanceId, boolean isStartEndPoint,
            boolean isEndEndPoint, boolean autoApprove, int expireIntervalSeconds, boolean waitForConfirm) {
        Verify.verify(expireIntervalSeconds > 0, "ApprovalExpirationInterval can not be negative");
        return new FlowApprovalInstance(authenticationFacade.currentOrganizationId(), flowInstanceId,
                expireIntervalSeconds, isStartEndPoint, isEndEndPoint, autoApprove, flowableAdaptor,
                flowableTaskService, formService, eventPublisher, authenticationFacade, nodeRepository,
                sequenceRepository, userTaskInstanceRepository, waitForConfirm, userTaskInstanceCandidateRepository);
    }

    public FlowTaskInstance generateFlowTaskInstance(@NonNull Long flowInstanceId, boolean isStartEndPoint,
            boolean isEndEndPoint, @NonNull TaskType taskType, @NonNull ExecutionStrategyConfig config) {
        return new FlowTaskInstance(taskType, authenticationFacade.currentOrganizationId(), flowInstanceId, config,
                isStartEndPoint, isEndEndPoint, new OdcRuntimeDelegateMapper(), flowableAdaptor, eventPublisher,
                flowableTaskService, nodeRepository, sequenceRepository, serviceTaskRepository);
    }

    public Optional<FlowInstance> getFlowInstance(@NonNull Long flowInstanceId) {
        Optional<FlowInstanceEntity> flowInstanceOptional = flowInstanceRepository.findById(flowInstanceId);
        if (!flowInstanceOptional.isPresent()) {
            return Optional.empty();
        }
        FlowInstance target = generateFlowInstance(flowInstanceOptional.get());
        List<NodeInstanceEntity> nodes = this.nodeRepository.findByFlowInstanceId(target.getId());
        List<BaseFlowNodeInstance> instances = new LinkedList<>(getGatewayInstances(target.getId(), nodes));
        instances.addAll(getApprovalInstances(target.getId(), nodes));
        instances.addAll(getTaskInstances(target.getId(), nodes));

        Set<FlowSequenceInstance> sequences = getSequences(target.getId());
        FlowInstanceUtil.loadTopology(target, instances, sequences);
        return Optional.of(target);
    }

    private Set<FlowSequenceInstance> getSequences(@NonNull Long flowInstanceId) {
        List<SequenceInstanceEntity> entities = sequenceRepository.findByFlowInstanceId(flowInstanceId);
        if (entities.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> nodeInstanceIds = entities.stream().flatMap(entity -> Stream.of(entity.getTargetNodeInstanceId(),
                entity.getSourceNodeInstanceId())).collect(Collectors.toSet());
        Map<Long, FlowNodeInstanceKey> nodeInstanceId2nodeInstanceKey = nodeRepository.findByIds(nodeInstanceIds)
                .stream().collect(Collectors.toMap(NodeInstanceEntity::getId,
                        entity -> new FlowNodeInstanceKey(entity.getInstanceId(), entity.getInstanceType())));
        return entities.stream().map(entity -> {
            FlowNodeInstanceKey source = nodeInstanceId2nodeInstanceKey.get(entity.getSourceNodeInstanceId());
            FlowNodeInstanceKey target = nodeInstanceId2nodeInstanceKey.get(entity.getTargetNodeInstanceId());
            Verify.notNull(source, "Source can not be null");
            Verify.notNull(target, "Target can not be null");
            return new FlowSequenceInstance(source, target);
        }).collect(Collectors.toSet());
    }

    private List<FlowGatewayInstance> getGatewayInstances(@NonNull Long flowInstanceId,
            List<NodeInstanceEntity> nodes) {
        Specification<GateWayInstanceEntity> specification =
                Specification.where(GateWayInstanceSpecs.flowInstanceIdEquals(flowInstanceId));
        return gatewayInstanceRepository.findAll(specification).stream()
                .map(e -> generateFlowGatewayInstance(e, nodes)).collect(Collectors.toList());
    }

    private List<FlowApprovalInstance> getApprovalInstances(@NonNull Long flowInstanceId,
            List<NodeInstanceEntity> nodes) {
        Specification<UserTaskInstanceEntity> specification =
                Specification.where(UserTaskInstanceSpecs.flowInstanceIdEquals(flowInstanceId));
        return userTaskInstanceRepository.findAll(specification).stream()
                .map(e -> generateFlowApprovalInstance(e, nodes)).collect(Collectors.toList());
    }

    private List<FlowTaskInstance> getTaskInstances(@NonNull Long flowInstanceId,
            List<NodeInstanceEntity> nodes) {
        Specification<ServiceTaskInstanceEntity> specification =
                Specification.where(ServiceTaskInstanceSpecs.flowInstanceIdEquals(flowInstanceId));
        return serviceTaskRepository.findAll(specification).stream()
                .map(e -> generateFlowTaskInstance(e, nodes)).collect(Collectors.toList());
    }

    private FlowInstance generateFlowInstance(@NonNull FlowInstanceEntity entity) {
        return new OdcFlowInstance(entity, flowableAdaptor, authenticationFacade,
                flowInstanceRepository, nodeRepository, sequenceRepository, gatewayInstanceRepository,
                serviceTaskRepository, userTaskInstanceRepository, userTaskInstanceCandidateRepository,
                runtimeService, repositoryService);
    }

    private FlowGatewayInstance generateFlowGatewayInstance(
            @NonNull GateWayInstanceEntity entity, List<NodeInstanceEntity> nodes) {
        Long id = entity.getId();
        try {
            entity.setId(null);
            FlowGatewayInstance target = new FlowGatewayInstance(entity, flowableAdaptor,
                    nodeRepository, sequenceRepository, gatewayInstanceRepository);
            setNameAndActivityId(id, target, nodes);
            return target;
        } finally {
            entity.setId(id);
        }
    }

    private FlowApprovalInstance generateFlowApprovalInstance(
            @NonNull UserTaskInstanceEntity entity, List<NodeInstanceEntity> nodes) {
        Long id = entity.getId();
        try {
            entity.setId(null);
            FlowApprovalInstance target = new FlowApprovalInstance(entity, flowableAdaptor, flowableTaskService,
                    formService, eventPublisher, authenticationFacade, nodeRepository, sequenceRepository,
                    userTaskInstanceRepository, userTaskInstanceCandidateRepository);
            setNameAndActivityId(id, target, nodes);
            return target;
        } finally {
            entity.setId(id);
        }
    }

    private FlowTaskInstance generateFlowTaskInstance(
            @NonNull ServiceTaskInstanceEntity entity, List<NodeInstanceEntity> nodes) {
        Long id = entity.getId();
        try {
            entity.setId(null);
            FlowTaskInstance target = new FlowTaskInstance(entity, new OdcRuntimeDelegateMapper(),
                    flowableAdaptor, eventPublisher, flowableTaskService, nodeRepository, sequenceRepository,
                    serviceTaskRepository);
            setNameAndActivityId(id, target, nodes);
            eventPublisher.publishEvent(new TaskInstanceCreatedEvent(target));
            return target;
        } finally {
            entity.setId(id);
        }
    }

    private void setNameAndActivityId(Long instId, BaseFlowNodeInstance inst, List<NodeInstanceEntity> nodes) {
        if (inst.getId() != null) {
            throw new VerifyException("Id for node instance should be null");
        }
        List<NodeInstanceEntity> targets = nodes.stream().filter(n -> Objects.equals(instId, n.getInstanceId())
                && Objects.equals(n.getFlowableElementType(), inst.getCoreFlowableElementType())
                && Objects.equals(n.getInstanceType(), inst.getNodeType())).collect(Collectors.toList());
        Verify.singleton(targets, "Node has to be singleton");
        inst.setId(instId);
        NodeInstanceEntity node = targets.get(0);
        inst.setName(node.getName());
        inst.setActivityId(node.getActivityId());
    }

}
