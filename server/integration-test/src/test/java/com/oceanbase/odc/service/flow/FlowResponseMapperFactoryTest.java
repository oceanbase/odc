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

import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.flowable.engine.FormService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.connection.ConnectionEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.flow.GateWayInstanceRepository;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntityRepository;
import com.oceanbase.odc.metadb.flow.SequenceInstanceRepository;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceCandidateRepository;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceRepository;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.metadb.task.TaskRepository;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.factory.FlowResponseMapperFactory;
import com.oceanbase.odc.service.flow.instance.FlowApprovalInstance;
import com.oceanbase.odc.service.flow.instance.FlowGatewayInstance;
import com.oceanbase.odc.service.flow.instance.FlowInstance;
import com.oceanbase.odc.service.flow.instance.FlowSequenceInstance;
import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;
import com.oceanbase.odc.service.flow.model.ExecutionStrategyConfig;
import com.oceanbase.odc.service.flow.model.FlowInstanceDetailResp;
import com.oceanbase.odc.service.flow.model.FlowInstanceDetailResp.FlowInstanceMapper;
import com.oceanbase.odc.service.flow.model.FlowNodeInstanceDetailResp;
import com.oceanbase.odc.service.flow.model.FlowNodeInstanceDetailResp.FlowNodeInstanceMapper;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.model.FlowTaskExecutionStrategy;
import com.oceanbase.odc.service.flow.tool.TestFlowRuntimeTaskImpl;
import com.oceanbase.odc.service.flow.util.FlowInstanceUtil;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.test.tool.TestRandom;

/**
 * Test cases for {@link com.oceanbase.odc.service.flow.factory.FlowResponseMapperFactory}
 *
 * @author yh263208
 * @date 2022-03-04 15:18
 * @since ODC_release_3.3.0
 * @see ServiceTestEnv
 */
public class FlowResponseMapperFactoryTest extends ServiceTestEnv {

    @Autowired
    private FlowResponseMapperFactory responseFactory;
    @MockBean
    private TaskRepository taskRepository;
    @Autowired
    private FlowableAdaptor flowAdaptor;
    @MockBean
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private FlowInstanceRepository flowInstanceRepository;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private EventPublisher eventPublisher;
    @Autowired
    private NodeInstanceEntityRepository nodeRepository;
    @Autowired
    private SequenceInstanceRepository sequenceRepository;
    @Autowired
    private ServiceTaskInstanceRepository serviceTaskRepository;
    @Autowired
    private TaskService taskService;
    @Autowired
    private FormService formService;
    @Autowired
    private UserTaskInstanceRepository userTaskInstanceRepository;
    @Autowired
    private GateWayInstanceRepository gateWayInstanceRepository;
    @MockBean
    private DatabaseService databaseService;
    @Autowired
    private UserTaskInstanceCandidateRepository userTaskInstanceCandidateRepository;
    private final AtomicLong counter = new AtomicLong(0L);

    @Before
    public void setUp() {
        flowInstanceRepository.deleteAll();
        serviceTaskRepository.deleteAll();
        nodeRepository.deleteAll();
        sequenceRepository.deleteAll();
        eventPublisher.removeAllListeners();
        userTaskInstanceRepository.deleteAll();
        gateWayInstanceRepository.deleteAll();
        Mockito.when(taskRepository.findById(Mockito.anyLong())).thenReturn(Optional.of(createTaskEntity()));
        when(authenticationFacade.currentUserId()).thenReturn(1L);
        when(authenticationFacade.currentOrganizationId()).thenReturn(1L);
    }

    @Test
    public void generateFlowInstanceDetailResp_entityInput_returnDesp() {
        FlowInstanceEntity instanceEntity = createFlowInstanceEntity();
        FlowInstanceMapper mapper = FlowInstanceDetailResp.mapper()
                .withApprovable(id -> false)
                .withGetConnectionById(id -> TestRandom.nextObject(ConnectionEntity.class))
                .withGetTaskByFlowInstanceId(id -> Collections.singleton(TestRandom.nextObject(TaskEntity.class)))
                .withGetUserById(id -> TestRandom.nextObject(UserEntity.class))
                .withGetExecutionStrategyByFlowInstanceId(
                        id -> Collections.singletonList(TestRandom.nextObject(FlowTaskExecutionStrategy.class)))
                .withGetExecutionTimeByFlowInstanceId(
                        id -> Collections.singletonList(TestRandom.nextObject(Date.class)))
                .withGetCandidatesByFlowInstanceId(
                        id -> Collections.singleton(TestRandom.nextObject(UserEntity.class)));
        FlowInstanceDetailResp detailResp = mapper.map(instanceEntity);

        Assert.assertEquals(instanceEntity.getId(), detailResp.getId());
    }

    @Test
    public void generateFlowInstanceDetailResp_instanceInput_returnDesp() {
        FlowInstance flowInstance = createFlowInstance();
        buildFlowInstance(flowInstance);

        List<Long> flowInstanceIds = Collections.singletonList(flowInstance.getId());
        when(databaseService.listDatabasesByConnectionIds(Mockito.anyCollection()))
                .thenReturn(Collections.singletonList(getDatabase()));
        FlowInstanceMapper mapper = responseFactory.generateMapperByInstanceIds(flowInstanceIds);
        FlowNodeInstanceMapper nodeMapper = responseFactory.generateNodeMapperByInstanceIds(flowInstanceIds);
        FlowInstanceDetailResp resp = mapper.map(flowInstance, nodeMapper);
        Assert.assertEquals(flowInstance.getId(), resp.getId());
    }

    @Test
    public void generateFlowNodeInstanceDetailResp_approvalInstance_returnDesp() {
        FlowApprovalInstance approvalInstance = createApprovalInstance(1L, 15, true, true);
        approvalInstance.setId(null);
        approvalInstance.create();
        approvalInstance.setStatus(FlowNodeStatus.EXECUTING);

        FlowNodeInstanceMapper nodeMapper = responseFactory
                .generateNodeMapperByInstanceIds(Collections.singletonList(approvalInstance.getFlowInstanceId()));
        FlowNodeInstanceDetailResp resp = nodeMapper.map(approvalInstance);
        Assert.assertEquals(FlowNodeStatus.EXECUTING, resp.getStatus());
    }

    @Test
    public void generateFlowNodeInstanceDetailResp_executingTaskInstance_returnDesp() {
        FlowTaskInstance taskInstance =
                createTaskInstance(1L, ExecutionStrategyConfig.autoStrategy(), true);
        taskInstance.setStatus(FlowNodeStatus.EXECUTING);

        FlowNodeInstanceMapper nodeMapper = responseFactory
                .generateNodeMapperByInstanceIds(Collections.singletonList(taskInstance.getFlowInstanceId()));
        FlowNodeInstanceDetailResp resp = nodeMapper.map(taskInstance);
        Assert.assertEquals(FlowNodeStatus.EXECUTING, resp.getStatus());
    }

    @Test
    public void generateFlowNodeInstanceDetailResp_pendingTaskInstance_returnDesp() {
        FlowTaskInstance taskInstance = createTaskInstance(1L,
                ExecutionStrategyConfig.manualStrategy(15, TimeUnit.SECONDS), true);
        taskInstance.setId(null);
        taskInstance.create();
        taskInstance.setStatus(FlowNodeStatus.PENDING);

        FlowNodeInstanceMapper nodeMapper = responseFactory
                .generateNodeMapperByInstanceIds(Collections.singletonList(taskInstance.getFlowInstanceId()));
        FlowNodeInstanceDetailResp resp = nodeMapper.map(taskInstance);

        Date deadlineTime = new Date(taskInstance.getUpdateTime().getTime()
                + TimeUnit.MILLISECONDS.convert(taskInstance.getStrategyConfig().getPendingExpireIntervalSeconds(),
                        TimeUnit.SECONDS));
        Assert.assertEquals(resp.getDeadlineTime(), deadlineTime);
    }

    private void buildFlowInstance(FlowInstance flowInstance) {
        FlowGatewayInstance gatewayInstance = createGatewayInstance(flowInstance.getId(), true, false);
        FlowApprovalInstance first_route_approval_1 = createApprovalInstance(flowInstance.getId(), 10, false, false);
        first_route_approval_1.setStatus(FlowNodeStatus.COMPLETED);
        FlowGatewayInstance first_route_approval_1_gateway = createGatewayInstance(flowInstance.getId(), false, true);
        FlowApprovalInstance first_route_approval_2 = createApprovalInstance(flowInstance.getId(), 10, false, false);
        FlowGatewayInstance first_route_approval_2_gateway = createGatewayInstance(flowInstance.getId(), false, true);
        FlowTaskInstance first_route_task_1 = createTaskInstance(flowInstance.getId(),
                ExecutionStrategyConfig.autoStrategy(), false);
        FlowApprovalInstance second_route_approval_1 = createApprovalInstance(flowInstance.getId(), 10, false, false);
        FlowGatewayInstance second_route_approval_1_gateway = createGatewayInstance(flowInstance.getId(), false, true);
        FlowTaskInstance second_route_task_1 = createTaskInstance(flowInstance.getId(),
                ExecutionStrategyConfig.manualStrategy(20, TimeUnit.SECONDS), false);
        Set<FlowSequenceInstance> sequences = new HashSet<>();
        sequences.add(new FlowSequenceInstance(gatewayInstance, first_route_approval_1));
        sequences.add(new FlowSequenceInstance(first_route_approval_1, first_route_approval_1_gateway));
        sequences.add(new FlowSequenceInstance(first_route_approval_1_gateway, first_route_approval_2));
        sequences.add(new FlowSequenceInstance(first_route_approval_2, first_route_approval_2_gateway));
        sequences.add(new FlowSequenceInstance(first_route_approval_2_gateway, first_route_task_1));
        sequences.add(new FlowSequenceInstance(gatewayInstance, second_route_approval_1));
        sequences.add(new FlowSequenceInstance(second_route_approval_1, second_route_approval_1_gateway));
        sequences.add(new FlowSequenceInstance(second_route_approval_1_gateway, second_route_task_1));
        FlowInstanceUtil.loadTopology(flowInstance,
                Arrays.asList(gatewayInstance, first_route_approval_1, first_route_approval_1_gateway,
                        first_route_approval_2, first_route_approval_2_gateway, first_route_task_1,
                        second_route_approval_1, second_route_approval_1_gateway, second_route_task_1),
                sequences);
        flowInstance.forEachInstanceNode(inst -> inst.setId(null));
        flowInstance.buildTopology();
    }

    private FlowInstance createFlowInstance() {
        return new FlowInstance("test", flowAdaptor, authenticationFacade, flowInstanceRepository,
                nodeRepository, sequenceRepository, gateWayInstanceRepository, serviceTaskRepository,
                userTaskInstanceRepository, userTaskInstanceCandidateRepository, runtimeService, repositoryService);
    }

    private FlowTaskInstance createTaskInstance(Long flowInstanceId, ExecutionStrategyConfig config,
            boolean startEndPoint) {
        FlowTaskInstance taskInstance = new FlowTaskInstance(TaskType.ASYNC,
                authenticationFacade.currentOrganizationId(), flowInstanceId, config, startEndPoint, true,
                type -> TestFlowRuntimeTaskImpl.class, flowAdaptor, eventPublisher, taskService, nodeRepository,
                sequenceRepository, serviceTaskRepository);
        taskInstance.setTargetTaskId(1L);
        taskInstance.setId(counter.incrementAndGet());
        return taskInstance;
    }

    private Database getDatabase() {
        Database database = new Database();
        database.setId(1L);
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setId(1L);
        database.setDataSource(connectionConfig);
        return database;
    }

    private FlowApprovalInstance createApprovalInstance(Long flowInstanceId, Integer expireIntervalSeconds,
            boolean startEndPoint, boolean endEndPoint) {
        FlowApprovalInstance inst = new FlowApprovalInstance(authenticationFacade.currentOrganizationId(),
                flowInstanceId, null, expireIntervalSeconds, startEndPoint, endEndPoint, false, flowAdaptor,
                taskService, formService, eventPublisher, authenticationFacade, nodeRepository, sequenceRepository,
                userTaskInstanceRepository, userTaskInstanceCandidateRepository);
        inst.setId(counter.incrementAndGet());
        return inst;
    }

    private FlowGatewayInstance createGatewayInstance(Long flowInstanceId, boolean startEndPoint, boolean endEndPoint) {
        FlowGatewayInstance inst = new FlowGatewayInstance(authenticationFacade.currentOrganizationId(), flowInstanceId,
                startEndPoint, endEndPoint, flowAdaptor, nodeRepository, sequenceRepository, gateWayInstanceRepository);
        inst.setId(counter.incrementAndGet());
        return inst;
    }

    private TaskEntity createTaskEntity() {
        return TestRandom.nextObject(TaskEntity.class);
    }

    private FlowInstanceEntity createFlowInstanceEntity() {
        return TestRandom.nextObject(FlowInstanceEntity.class);
    }
}
