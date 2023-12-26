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

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;

import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.flow.BaseExecutionListener;
import com.oceanbase.odc.core.flow.ExecutionConfigurer;
import com.oceanbase.odc.core.flow.builder.FlowableProcessBuilder;
import com.oceanbase.odc.core.flow.graph.Graph;
import com.oceanbase.odc.core.flow.graph.GraphEdge;
import com.oceanbase.odc.core.flow.graph.GraphVertex;
import com.oceanbase.odc.core.flow.model.FlowableElement;
import com.oceanbase.odc.core.flow.util.FlowUtil;
import com.oceanbase.odc.core.shared.OrganizationIsolated;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.flow.GateWayInstanceRepository;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntity;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntityRepository;
import com.oceanbase.odc.metadb.flow.SequenceInstanceEntity;
import com.oceanbase.odc.metadb.flow.SequenceInstanceRepository;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceCandidateRepository;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceRepository;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.model.FlowNodeType;
import com.oceanbase.odc.service.flow.model.NodeInstanceEntityKey;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link FlowInstance} object standards for a process instance, Wrapper object for flowable
 * {@link org.flowable.engine.runtime.ProcessInstance}
 *
 * @author yh263208
 * @date 2022-02-10 14:42
 * @since ODC_release_3.3.0
 */
@Getter
@Slf4j
public class FlowInstance extends Graph implements SecurityResource, OrganizationIsolated {

    private Long id;
    private Date createTime;
    private Date updateTime;
    private String processDefinitionId;
    private volatile String processInstanceId;

    private Long parentFlowInstanceId;
    private Long projectId;
    @Setter
    private FlowStatus status;
    private String flowConfigSnapShotXml;
    private final Long creatorId;
    private final String name;
    private final Long organizationId;
    private final String description;

    @Getter(AccessLevel.NONE)
    private final AuthenticationFacade authenticationFacade;
    @Getter(AccessLevel.NONE)
    private final RuntimeService runtimeService;
    @Getter(AccessLevel.NONE)
    private final RepositoryService repositoryService;
    @Getter(AccessLevel.NONE)
    private final FlowInstanceRepository flowInstanceRepository;
    @Getter(AccessLevel.NONE)
    private final NodeInstanceEntityRepository nodeInstanceRepository;
    @Getter(AccessLevel.NONE)
    private final SequenceInstanceRepository sequenceRepository;
    @Getter(AccessLevel.NONE)
    protected final FlowableAdaptor flowableAdaptor;
    @Getter(AccessLevel.NONE)
    protected final ProcessNodeBuilderAccessor accessor;
    @Getter(AccessLevel.NONE)
    private final GateWayInstanceRepository gateWayInstanceRepository;
    @Getter(AccessLevel.NONE)
    private final ServiceTaskInstanceRepository serviceTaskRepository;
    @Getter(AccessLevel.NONE)
    private final UserTaskInstanceRepository userTaskInstanceRepository;
    @Getter(AccessLevel.NONE)
    private final UserTaskInstanceCandidateRepository userTaskInstanceCandidateRepository;
    protected final FlowableProcessBuilder processBuilder;

    /**
     * Create a new {@link FlowInstance}
     */
    public FlowInstance(@NonNull String name, @NonNull FlowableAdaptor flowableAdaptor,
            @NonNull AuthenticationFacade authenticationFacade,
            @NonNull FlowInstanceRepository flowInstanceRepository,
            @NonNull NodeInstanceEntityRepository nodeInstanceRepository,
            @NonNull SequenceInstanceRepository sequenceRepository,
            @NonNull GateWayInstanceRepository gateWayInstanceRepository,
            @NonNull ServiceTaskInstanceRepository serviceTaskRepository,
            @NonNull UserTaskInstanceRepository userTaskInstanceRepository,
            @NonNull UserTaskInstanceCandidateRepository userTaskInstanceCandidateRepository,
            @NonNull RuntimeService runtimeService, @NonNull RepositoryService repositoryService) {
        this(name, null, flowableAdaptor, authenticationFacade, flowInstanceRepository, nodeInstanceRepository,
                sequenceRepository, gateWayInstanceRepository, serviceTaskRepository, userTaskInstanceRepository,
                userTaskInstanceCandidateRepository, runtimeService, repositoryService);
    }

    /**
     * Create a new {@link FlowInstance}
     */
    public FlowInstance(@NonNull String name, String description,
            @NonNull FlowableAdaptor flowableAdaptor,
            @NonNull AuthenticationFacade authenticationFacade,
            @NonNull FlowInstanceRepository flowInstanceRepository,
            @NonNull NodeInstanceEntityRepository nodeInstanceRepository,
            @NonNull SequenceInstanceRepository sequenceRepository,
            @NonNull GateWayInstanceRepository gateWayInstanceRepository,
            @NonNull ServiceTaskInstanceRepository serviceTaskRepository,
            @NonNull UserTaskInstanceRepository userTaskInstanceRepository,
            @NonNull UserTaskInstanceCandidateRepository userTaskInstanceCandidateRepository,
            @NonNull RuntimeService runtimeService, @NonNull RepositoryService repositoryService) {
        this(null, name, authenticationFacade.currentUserId(), authenticationFacade.currentOrganizationId(),
                null, null, FlowStatus.CREATED, null, description, null, null, authenticationFacade,
                flowInstanceRepository, nodeInstanceRepository, sequenceRepository, gateWayInstanceRepository,
                serviceTaskRepository, userTaskInstanceRepository, userTaskInstanceCandidateRepository,
                flowableAdaptor, runtimeService, repositoryService);
        create();
        Verify.notNull(getId(), "id");
        Verify.notNull(getCreateTime(), "CreateTime");
        Verify.notNull(getUpdateTime(), "UpdateTime");
    }

    public FlowInstance(@NonNull String name, String description, Long projectId, Long parentFlowInstanceId,
            @NonNull FlowableAdaptor flowableAdaptor,
            @NonNull AuthenticationFacade authenticationFacade,
            @NonNull FlowInstanceRepository flowInstanceRepository,
            @NonNull NodeInstanceEntityRepository nodeInstanceRepository,
            @NonNull SequenceInstanceRepository sequenceRepository,
            @NonNull GateWayInstanceRepository gateWayInstanceRepository,
            @NonNull ServiceTaskInstanceRepository serviceTaskRepository,
            @NonNull UserTaskInstanceRepository userTaskInstanceRepository,
            @NonNull UserTaskInstanceCandidateRepository userTaskInstanceCandidateRepository,
            @NonNull RuntimeService runtimeService, @NonNull RepositoryService repositoryService) {
        this(null, name, authenticationFacade.currentUserId(), authenticationFacade.currentOrganizationId(),
                projectId, parentFlowInstanceId, null, null, FlowStatus.CREATED, null, description, null, null,
                authenticationFacade, flowInstanceRepository, nodeInstanceRepository, sequenceRepository,
                gateWayInstanceRepository, serviceTaskRepository, userTaskInstanceRepository,
                userTaskInstanceCandidateRepository, flowableAdaptor, runtimeService, repositoryService);
        create();
        Verify.notNull(getId(), "id");
        Verify.notNull(getCreateTime(), "CreateTime");
        Verify.notNull(getUpdateTime(), "UpdateTime");
    }

    /**
     * Load a {@link FlowInstance} from {@code metaDB}
     */
    public FlowInstance(@NonNull FlowInstanceEntity entity, @NonNull FlowableAdaptor flowableAdaptor,
            @NonNull AuthenticationFacade authenticationFacade,
            @NonNull FlowInstanceRepository flowInstanceRepository,
            @NonNull NodeInstanceEntityRepository nodeInstanceRepository,
            @NonNull SequenceInstanceRepository sequenceRepository,
            @NonNull GateWayInstanceRepository gateWayInstanceRepository,
            @NonNull ServiceTaskInstanceRepository serviceTaskRepository,
            @NonNull UserTaskInstanceRepository userTaskInstanceRepository,
            @NonNull UserTaskInstanceCandidateRepository userTaskInstanceCandidateRepository,
            @NonNull RuntimeService runtimeService, @NonNull RepositoryService repositoryService) {
        this(entity.getId(), entity.getName(), entity.getCreatorId(), entity.getOrganizationId(),
                entity.getProjectId(), entity.getParentInstanceId(), entity.getProcessDefinitionId(),
                entity.getProcessInstanceId(), entity.getStatus(), entity.getFlowConfigSnapshotXml(),
                entity.getDescription(), entity.getCreateTime(), entity.getUpdateTime(), authenticationFacade,
                flowInstanceRepository, nodeInstanceRepository, sequenceRepository, gateWayInstanceRepository,
                serviceTaskRepository, userTaskInstanceRepository, userTaskInstanceCandidateRepository,
                flowableAdaptor, runtimeService, repositoryService);
    }

    private FlowInstance(Long id, @NonNull String name, @NonNull Long creatorId, @NonNull Long organizationId,
            Long projectId, Long parentFlowInstanceId,
            String processDefinitionId, String processInstanceId, @NonNull FlowStatus status,
            String flowConfigSnapShotXml, String description, Date createTime, Date updateTime,
            @NonNull AuthenticationFacade authenticationFacade,
            @NonNull FlowInstanceRepository flowInstanceRepository,
            @NonNull NodeInstanceEntityRepository nodeInstanceRepository,
            @NonNull SequenceInstanceRepository sequenceRepository,
            @NonNull GateWayInstanceRepository gateWayInstanceRepository,
            @NonNull ServiceTaskInstanceRepository serviceTaskRepository,
            @NonNull UserTaskInstanceRepository userTaskInstanceRepository,
            @NonNull UserTaskInstanceCandidateRepository userTaskInstanceCandidateRepository,
            @NonNull FlowableAdaptor flowableAdaptor,
            @NonNull RuntimeService runtimeService, @NonNull RepositoryService repositoryService) {
        this.parentFlowInstanceId = parentFlowInstanceId;
        this.id = id;
        this.name = name;
        this.creatorId = creatorId;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.processDefinitionId = processDefinitionId;
        this.processInstanceId = processInstanceId;
        this.status = status;
        this.description = description;
        this.flowConfigSnapShotXml = flowConfigSnapShotXml;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.authenticationFacade = authenticationFacade;
        this.runtimeService = runtimeService;
        this.repositoryService = repositoryService;
        this.flowInstanceRepository = flowInstanceRepository;
        this.flowableAdaptor = flowableAdaptor;
        this.processBuilder = new FlowableProcessBuilder(name);
        this.accessor = new ProcessNodeBuilderAccessor();
        this.sequenceRepository = sequenceRepository;
        this.nodeInstanceRepository = nodeInstanceRepository;
        this.gateWayInstanceRepository = gateWayInstanceRepository;
        this.serviceTaskRepository = serviceTaskRepository;
        this.userTaskInstanceRepository = userTaskInstanceRepository;
        this.userTaskInstanceCandidateRepository = userTaskInstanceCandidateRepository;
    }

    private FlowInstance(Long id, @NonNull String name, @NonNull Long creatorId, @NonNull Long organizationId,
            String processDefinitionId, String processInstanceId, @NonNull FlowStatus status,
            String flowConfigSnapShotXml, String description, Date createTime, Date updateTime,
            @NonNull AuthenticationFacade authenticationFacade,
            @NonNull FlowInstanceRepository flowInstanceRepository,
            @NonNull NodeInstanceEntityRepository nodeInstanceRepository,
            @NonNull SequenceInstanceRepository sequenceRepository,
            @NonNull GateWayInstanceRepository gateWayInstanceRepository,
            @NonNull ServiceTaskInstanceRepository serviceTaskRepository,
            @NonNull UserTaskInstanceRepository userTaskInstanceRepository,
            @NonNull UserTaskInstanceCandidateRepository userTaskInstanceCandidateRepository,
            @NonNull FlowableAdaptor flowableAdaptor,
            @NonNull RuntimeService runtimeService, @NonNull RepositoryService repositoryService) {
        this.id = id;
        this.name = name;
        this.creatorId = creatorId;
        this.organizationId = organizationId;
        this.processDefinitionId = processDefinitionId;
        this.processInstanceId = processInstanceId;
        this.status = status;
        this.description = description;
        this.flowConfigSnapShotXml = flowConfigSnapShotXml;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.authenticationFacade = authenticationFacade;
        this.runtimeService = runtimeService;
        this.repositoryService = repositoryService;
        this.flowInstanceRepository = flowInstanceRepository;
        this.flowableAdaptor = flowableAdaptor;
        this.processBuilder = new FlowableProcessBuilder(name);
        this.accessor = new ProcessNodeBuilderAccessor();
        this.sequenceRepository = sequenceRepository;
        this.nodeInstanceRepository = nodeInstanceRepository;
        this.gateWayInstanceRepository = gateWayInstanceRepository;
        this.serviceTaskRepository = serviceTaskRepository;
        this.userTaskInstanceRepository = userTaskInstanceRepository;
        this.userTaskInstanceCandidateRepository = userTaskInstanceCandidateRepository;
    }

    public void create() {
        validNotExists();
        FlowInstanceEntity entity = new FlowInstanceEntity();
        entity.setName(getName());
        entity.setCreatorId(getCreatorId());
        entity.setOrganizationId(getOrganizationId());
        // TODO Project ID cannot be null. This is hard-coded template.
        entity.setProjectId(getProjectId() == null ? -1 : getProjectId());
        entity.setProcessInstanceId(getProcessInstanceId());
        entity.setProcessDefinitionId(getProcessDefinitionId());
        entity.setStatus(getStatus());
        entity.setFlowConfigSnapshotXml(getFlowConfigSnapShotXml());
        entity.setDescription(getDescription());
        entity.setParentInstanceId(getParentFlowInstanceId());
        entity = flowInstanceRepository.save(entity);
        Verify.notNull(entity.getId(), "id");
        Verify.notNull(entity.getCreateTime(), "CreateTime");
        Verify.notNull(entity.getUpdateTime(), "UpdateTime");
        this.id = entity.getId();
        this.createTime = entity.getCreateTime();
        this.updateTime = entity.getUpdateTime();
        log.info("Create flow instance successfully, flowInstance={}", entity);
    }

    public boolean delete() {
        if (this.processInstanceId != null) {
            if (!isPresentOnThisMachine()) {
                throw new UnsupportedOperationException("Process instance is not on this machine");
            }
            try {
                runtimeService.deleteProcessInstance(this.processInstanceId, "User delete");
            } catch (Exception e) {
                log.warn("Failed to delete process instance, instanceId={}, processInstanceId={}", this.id,
                        this.processInstanceId, e);
                throw new IllegalStateException("Process instance is running");
            }
        }
        dealloc();
        validExists();
        flowInstanceRepository.deleteById(getId());
        log.info("Delete flow instance successfully, flowInstanceId={}", getId());
        boolean returnVal = true;
        for (GraphVertex graphVertex : this.vertexList) {
            Verify.verify(graphVertex instanceof BaseFlowNodeInstance,
                    "GraphVertex has to be a instance of AbstractFlowNodeInstance");
            BaseFlowNodeInstance nodeInstance = (BaseFlowNodeInstance) graphVertex;
            returnVal &= nodeInstance.delete();
        }
        return returnVal;
    }

    public void update() {
        validExists();
        FlowInstanceEntity entity = new FlowInstanceEntity();
        entity.setId(getId());
        entity.setName(getName());
        entity.setProcessDefinitionId(getProcessDefinitionId());
        entity.setProcessInstanceId(getProcessInstanceId());
        entity.setStatus(getStatus());
        entity.setFlowConfigSnapshotXml(getFlowConfigSnapShotXml());
        int affectRows = flowInstanceRepository.update(entity);
        log.info("Update flow instance successfully, affectRows={}, flowInstance={}", affectRows, entity);
    }

    public void start() {
        start(null);
    }

    public void start(Map<String, Object> variables) {
        if (this.processInstanceId != null) {
            throw new UnsupportedOperationException(
                    "Process Instance has been exist, process instance id " + this.processInstanceId);
        }
        Process process = this.processBuilder.build();
        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.addProcess(process);

        /**
         * Name for parameter one of the method {@link DeploymentBuilder#addBpmnModel(String, BpmnModel)}
         * has to be end with {@code bpmn}
         */
        Deployment deployment = repositoryService.createDeployment().addBpmnModel("odc.bpmn", bpmnModel).deploy();
        ProcessDefinition processDefinition =
                repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();

        this.flowConfigSnapShotXml = FlowUtil.convertToXml(this.getProcessBuilder());
        this.processDefinitionId = processDefinition.getId();
        update();

        ProcessInstance processInstance;
        try {
            processInstance = runtimeService.startProcessInstanceById(processDefinition.getId(), variables);
        } catch (Exception e) {
            log.warn("Failed to start process instance, instanceId={}, processDefinitionId={}", this.id,
                    this.processDefinitionId);
            runtimeService.createProcessInstanceQuery().processDefinitionId(this.processDefinitionId).list().forEach(
                    processInstance1 -> runtimeService.deleteProcessInstance(processInstance1.getId(), e.getMessage()));
            throw e;
        }
        this.processInstanceId = processInstance.getId();
        flowableAdaptor.setProcessInstanceId(getId(), processInstance.getId());
        log.info(
                "Flow instance starts executing, flowInstanceId={}, status={}, processInstanceId={}, processDefinitionId={}",
                this.id, this.status, processInstanceId, processDefinitionId);
    }

    public BaseFlowNodeInstance getStartNodeInstance() {
        List<BaseFlowNodeInstance> nodeInstances = filterInstanceNode(BaseFlowNodeInstance::isStartEndpoint);
        Verify.singleton(nodeInstances, "NodeInstances");
        return nodeInstances.get(0);
    }

    public List<BaseFlowNodeInstance> getEndNodeInstances() {
        return filterInstanceNode(BaseFlowNodeInstance::isEndEndPoint);
    }

    public int getNodeInstanceCount() {
        return this.vertexList.size();
    }

    public int getSequenceCount() {
        return this.edgeList.size();
    }

    public List<BaseFlowNodeInstance> filterInstanceNode(@NonNull Predicate<BaseFlowNodeInstance> predicate) {
        List<BaseFlowNodeInstance> returnVal = new LinkedList<>();
        for (GraphVertex graphVertex : this.vertexList) {
            Verify.verify(graphVertex instanceof BaseFlowNodeInstance,
                    "GraphVertex has to be a instance of AbstractFlowNodeInstance");
            BaseFlowNodeInstance nodeInstance = (BaseFlowNodeInstance) graphVertex;
            if (predicate.test(nodeInstance)) {
                returnVal.add(nodeInstance);
            }
        }
        return returnVal;
    }

    public List<BaseFlowNodeInstance> getNextNodeInstances(@NonNull Long instanceId,
            @NonNull FlowNodeType instanceType) {
        Optional<BaseFlowNodeInstance> optional = findByIdAndInstanceType(instanceId, instanceType);
        BaseFlowNodeInstance nodeInstance = optional.orElseThrow(
                () -> new NullPointerException("Instance not fount by id " + instanceId + " and type " + instanceType));
        List<BaseFlowNodeInstance> returnVal = new LinkedList<>();
        for (GraphEdge outEdge : nodeInstance.getOutEdges()) {
            GraphVertex graphVertex = outEdge.getTo();
            Verify.verify(graphVertex instanceof BaseFlowNodeInstance,
                    "GraphVertex has to be a instance of AbstractFlowNodeInstance");
            returnVal.add((BaseFlowNodeInstance) graphVertex);
        }
        return returnVal;
    }

    /**
     * dealloc resource
     */
    public void dealloc() {
        forEachInstanceNode(nodeInstance -> {
            try {
                nodeInstance.dealloc();
            } catch (Exception exception) {
                log.warn("Node instance failed to release resources, nodeId={}, nodeType={}", nodeInstance.getId(),
                        nodeInstance.getNodeType(), exception);
                throw exception;
            }
        });
        log.info("Flow instance resources are released successfully, flowInstanceId={}", getId());
    }

    /**
     * Build the topology of the node, related tables are as follow:
     *
     * <pre>
     *     1. {@code
     * flow_instance_node
     * }
     *     2. {@code
     * flow_instance_sequence
     * }
     * </pre>
     *
     * this method will insert the topo structure between the related nodes
     */
    public void buildTopology() {
        List<FlowApprovalInstance> approvalInstances = new ArrayList<>();
        List<FlowTaskInstance> taskInstances = new ArrayList<>();
        List<FlowGatewayInstance> gatewayInstances = new ArrayList<>();
        forEachInstanceNode(inst -> {
            if (inst instanceof FlowApprovalInstance) {
                approvalInstances.add((FlowApprovalInstance) inst);
            } else if (inst instanceof FlowTaskInstance) {
                taskInstances.add((FlowTaskInstance) inst);
            } else if (inst instanceof FlowGatewayInstance) {
                gatewayInstances.add((FlowGatewayInstance) inst);
            } else {
                throw new IllegalStateException("Unknown node instance, " + inst.getClass());
            }
        });
        FlowApprovalInstance.batchCreate(approvalInstances, this.userTaskInstanceRepository,
                this.userTaskInstanceCandidateRepository);
        FlowTaskInstance.batchCreate(taskInstances, this.serviceTaskRepository);
        FlowGatewayInstance.batchCreate(gatewayInstances, this.gateWayInstanceRepository);
        List<Pair<BaseFlowNodeInstance, FlowableElement>> elts = new ArrayList<>();
        forEachInstanceNode(inst -> {
            for (FlowableElement elt : inst.getBindFlowableElements()) {
                elts.add(new Pair<>(inst, elt));
            }
        });
        this.flowableAdaptor.setFlowableElements(elts);
        List<NodeInstanceEntity> entities = new ArrayList<>();
        forEachInstanceNode(inst -> {
            NodeInstanceEntity nodeEntity = new NodeInstanceEntity();
            nodeEntity.setInstanceId(inst.getId());
            nodeEntity.setInstanceType(inst.getNodeType());
            nodeEntity.setFlowInstanceId(inst.getFlowInstanceId());
            nodeEntity.setActivityId(inst.getActivityId());
            nodeEntity.setName(inst.getName());
            nodeEntity.setFlowableElementType(inst.getCoreFlowableElementType());
            entities.add(nodeEntity);
        });
        Map<NodeInstanceEntityKey, NodeInstanceEntity> map = this.nodeInstanceRepository
                .batchCreate(entities)
                .stream().collect(Collectors.toMap(NodeInstanceEntityKey::new, e -> e));
        List<SequenceInstanceEntity> seqs = new ArrayList<>();
        forEachInstanceNode(inst -> {
            NodeInstanceEntity sourceEntity = map.get(new NodeInstanceEntityKey(inst));
            for (GraphEdge outEdge : inst.getOutEdges()) {
                GraphVertex graphVertex = outEdge.getTo();
                if (!(graphVertex instanceof BaseFlowNodeInstance)) {
                    throw new IllegalStateException("GraphVertex has to be an instance of BaseFlowNodeInstance");
                }
                NodeInstanceEntity targetEntity =
                        map.get(new NodeInstanceEntityKey((BaseFlowNodeInstance) graphVertex));
                SequenceInstanceEntity sequenceEntity = new SequenceInstanceEntity();
                sequenceEntity.setFlowInstanceId(inst.getFlowInstanceId());
                sequenceEntity.setSourceNodeInstanceId(sourceEntity.getId());
                sequenceEntity.setTargetNodeInstanceId(targetEntity.getId());
                seqs.add(sequenceEntity);
            }
        });
        this.sequenceRepository.batchCreate(seqs);
        log.info("Flow instance node establishes the topology relationship successfully, flowInstanceId={}", getId());
    }

    @Override
    public String resourceId() {
        return getId() + "";
    }

    @Override
    public String resourceType() {
        return ResourceType.ODC_FLOW_INSTANCE.name();
    }

    @Override
    public Long organizationId() {
        return getOrganizationId();
    }

    @Override
    public Long id() {
        return getId();
    }

    public FlowInstanceConfigurer newFlowInstance() {
        return newFlowInstance(null);
    }

    public FlowInstanceConfigurer newFlowInstance(Class<? extends BaseExecutionListener> startListenerClazz) {
        ExecutionConfigurer configurer = this.processBuilder.newProcess(startListenerClazz);
        return newFlowInstanceConfigurer(configurer);
    }

    public FlowInstanceConfigurer endFlowInstance() {
        return endFlowInstance(null);
    }

    public FlowInstanceConfigurer endFlowInstance(Class<? extends BaseExecutionListener> endListenerClazz) {
        ExecutionConfigurer configurer = this.processBuilder.endProcess(endListenerClazz);
        return newFlowInstanceConfigurer(configurer);
    }

    public FlowInstanceConfigurer newFlowInstanceConfigurer(FlowApprovalInstance newNode) {
        FlowInstanceConfigurer configurer = newFlowInstanceConfigurer();
        configurer.next(newNode);
        return configurer;
    }

    public FlowInstanceConfigurer newFlowInstanceConfigurer(FlowTaskInstance newNode) {
        FlowInstanceConfigurer configurer = newFlowInstanceConfigurer();
        configurer.next(newNode);
        return configurer;
    }

    public FlowInstanceConfigurer newFlowInstanceConfigurer(FlowGatewayInstance newNode) {
        FlowInstanceConfigurer configurer = newFlowInstanceConfigurer();
        configurer.next(newNode);
        return configurer;
    }

    public FlowInstanceConfigurer newFlowInstanceConfigurer() {
        return new FlowInstanceConfigurer(this, processBuilder, flowableAdaptor, accessor);
    }

    public FlowInstance converge(@NonNull List<FlowInstanceConfigurer> configurerList,
            @NonNull FlowInstanceConfigurer convergedConfigurer) {
        BaseFlowNodeInstance to = convergedConfigurer.first();
        if (to == null) {
            processBuilder.converge(configurerList.stream().map(FlowInstanceConfigurer::getTargetExecution)
                    .collect(Collectors.toList()), convergedConfigurer.getTargetExecution());
            return this;
        }
        for (FlowInstanceConfigurer configurer : configurerList) {
            if (to instanceof FlowGatewayInstance) {
                configurer.next((FlowGatewayInstance) to);
            } else if (to instanceof FlowApprovalInstance) {
                configurer.next((FlowApprovalInstance) to);
            } else if (to instanceof FlowTaskInstance) {
                configurer.next((FlowTaskInstance) to);
            } else {
                throw new IllegalStateException("Illegal instance type " + to.getClass());
            }
        }
        return this;
    }

    public void forEachInstanceNode(@NonNull Consumer<BaseFlowNodeInstance> consumer) {
        for (GraphVertex graphVertex : this.vertexList) {
            Verify.verify(graphVertex instanceof BaseFlowNodeInstance,
                    "GraphVertex has to be a instance of AbstractFlowNodeInstance");
            consumer.accept((BaseFlowNodeInstance) graphVertex);
        }
    }

    public boolean isPresentOnThisMachine() {
        if (this.processInstanceId == null) {
            return false;
        }
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(this.getProcessInstanceId()).singleResult();
        return processInstance != null;
    }

    public boolean isEnded() {
        if (!isPresentOnThisMachine()) {
            throw new UnsupportedOperationException("Process instance is not on this machine");
        }
        ProcessInstance processInstance =
                runtimeService.createProcessInstanceQuery().processInstanceId(getProcessInstanceId()).singleResult();
        return processInstance.isEnded();
    }

    protected FlowInstanceConfigurer newFlowInstanceConfigurer(@NonNull ExecutionConfigurer configurer) {
        return new FlowInstanceConfigurer(this, processBuilder, configurer, flowableAdaptor, accessor);
    }

    private void validExists() {
        if (getId() == null) {
            throw new NullPointerException("Id of FlowInstance can not be null");
        }
    }

    private void validNotExists() {
        if (getId() != null) {
            throw new IllegalStateException("Cannot create an already existing FlowInstance, Id exists");
        }
    }

    private Optional<BaseFlowNodeInstance> findByIdAndInstanceType(@NonNull Long instanceId,
            @NonNull FlowNodeType instanceType) {
        List<BaseFlowNodeInstance> nodeInstances = filterInstanceNode(
                instance -> Objects.equals(instance.getNodeType(), instanceType)
                        && Objects.equals(instance.getId(), instanceId));
        Verify.verify(nodeInstances.size() <= 1, "Duplicate node, Id " + instanceId + ", type " + instanceType);
        if (nodeInstances.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(nodeInstances.get(0));
    }

}
