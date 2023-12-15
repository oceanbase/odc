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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.flowable.engine.FormService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.common.event.LocalEventPublisher;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.flow.GateWayInstanceRepository;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntityRepository;
import com.oceanbase.odc.metadb.flow.SequenceInstanceRepository;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceCandidateRepository;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceRepository;
import com.oceanbase.odc.service.flow.instance.BaseFlowNodeInstance;
import com.oceanbase.odc.service.flow.instance.FlowApprovalInstance;
import com.oceanbase.odc.service.flow.instance.FlowGatewayInstance;
import com.oceanbase.odc.service.flow.instance.FlowInstance;
import com.oceanbase.odc.service.flow.instance.FlowInstanceConfigurer;
import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;
import com.oceanbase.odc.service.flow.model.ExecutionStrategyConfig;
import com.oceanbase.odc.service.flow.model.FlowNodeType;
import com.oceanbase.odc.service.flow.tool.TestFlowRuntimeTaskImpl;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

/**
 * Test cases for {@link FlowInstance}
 *
 * @author yh263208
 * @date 2022-02-22 10:52
 * @since ODC_release_3.3.0
 * @see ServiceTestEnv
 */
public class FlowInstanceTest extends ServiceTestEnv {

    @MockBean
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private FlowableAdaptor flowableAdaptor;
    @Autowired
    private FlowInstanceRepository flowInstanceRepository;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private RepositoryService repositoryService;
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
    @Autowired
    private UserTaskInstanceCandidateRepository userTaskInstanceCandidateRepository;

    @Before
    public void setUp() {
        flowInstanceRepository.deleteAll();
        serviceTaskRepository.deleteAll();
        nodeRepository.deleteAll();
        sequenceRepository.deleteAll();
        userTaskInstanceRepository.deleteAll();
        gateWayInstanceRepository.deleteAll();
        when(authenticationFacade.currentUserId()).thenReturn(1L);
        when(authenticationFacade.currentOrganizationId()).thenReturn(1L);
    }

    @Test
    public void create_createFlowInstance_createSucceed() {
        FlowInstance flowInstance = createFlowInstance("test instance");
        Optional<FlowInstanceEntity> optional = flowInstanceRepository.findById(flowInstance.getId());

        Assert.assertTrue(optional.isPresent());
    }

    @Test
    public void delete_deleteFlowInstance_deleteSucceed() {
        FlowInstance flowInstance = createFlowInstance("test instance_1");
        flowInstance.delete();
        Optional<FlowInstanceEntity> optional = flowInstanceRepository.findById(flowInstance.getId());

        Assert.assertFalse(optional.isPresent());
    }

    @Test
    public void update_updateStatus_updateSucceed() {
        FlowInstance flowInstance = createFlowInstance("test instance_1");
        flowInstance.setStatus(FlowStatus.EXECUTION_SUCCEEDED);
        flowInstance.update();

        FlowInstanceEntity actual = flowInstanceRepository.findById(flowInstance.getId())
                .orElseThrow(IllegalStateException::new);
        Assert.assertEquals(flowInstance.getStatus(), actual.getStatus());
    }

    @Test
    public void create_createFlowInstanceWithTopo_createSucceed() {
        createFlowInstanceWithTopo();

        Assert.assertEquals(8, nodeRepository.findAll().size());
        Assert.assertEquals(5, sequenceRepository.findAll().size());
        Assert.assertEquals(2, gateWayInstanceRepository.findAll().size());
        Assert.assertEquals(2, serviceTaskRepository.findAll().size());
        Assert.assertEquals(1, userTaskInstanceRepository.findAll().size());
    }

    @Test
    public void getStartNodeInstance_createFlowInstanceWithTopo_gateWayNodeGot() {
        FlowInstance instance = createFlowInstanceWithTopo();

        BaseFlowNodeInstance nodeInstance = instance.getStartNodeInstance();
        Assert.assertEquals(FlowNodeType.GATEWAY, nodeInstance.getNodeType());
    }

    @Test
    public void getEndNodeInstances_createFlowInstanceWithTopo_gateWayNodeGot() {
        FlowInstance instance = createFlowInstanceWithTopo();

        List<BaseFlowNodeInstance> nodeInstances = instance.getEndNodeInstances();
        Assert.assertEquals(1, nodeInstances.size());
        Assert.assertEquals(FlowNodeType.GATEWAY, nodeInstances.get(0).getNodeType());
    }

    @Test
    public void getNodeInstanceCount_createFlowInstanceWithTopo_5NodeGot() {
        FlowInstance instance = createFlowInstanceWithTopo();
        Assert.assertEquals(5, instance.getNodeInstanceCount());
    }

    @Test
    public void getNextNodeInstances_createFlowInstanceWithTopo_approvalAndServiceTaskGot() {
        FlowInstance instance = createFlowInstanceWithTopo();
        BaseFlowNodeInstance nodeInstance = instance.getStartNodeInstance();
        List<BaseFlowNodeInstance> nodes =
                instance.getNextNodeInstances(nodeInstance.getId(), nodeInstance.getNodeType());
        Assert.assertEquals(2, nodes.size());

        Set<FlowNodeType> actual =
                nodes.stream().map(BaseFlowNodeInstance::getNodeType).collect(Collectors.toSet());
        Assert.assertEquals(new HashSet<>(Arrays.asList(FlowNodeType.APPROVAL_TASK, FlowNodeType.SERVICE_TASK)),
                actual);
    }

    @Test
    public void delete_deleteFlowInstanceWithTopo_nothingExists() {
        FlowInstance flowInstance = createFlowInstanceWithTopo();
        Assert.assertTrue(flowInstance.delete());

        Assert.assertEquals(0, nodeRepository.findAll().size());
        Assert.assertEquals(0, sequenceRepository.findAll().size());
        Assert.assertEquals(0, gateWayInstanceRepository.findAll().size());
        Assert.assertEquals(0, serviceTaskRepository.findAll().size());
        Assert.assertEquals(0, userTaskInstanceRepository.findAll().size());
    }

    @Test
    public void start_startFlowInstance_processIsRunning() {
        FlowInstance flowInstance = createOdcInstance();
        Map<String, Object> variables = new HashMap<>();
        variables.putIfAbsent("level", 100);
        flowInstance.start(variables);

        Assert.assertTrue(flowInstance.isPresentOnThisMachine());
        Assert.assertFalse(flowInstance.isEnded());
    }

    @Test
    public void filterInstanceNode_getApprovalNode_getSucceed() {
        FlowInstance flowInstance = createOdcInstance();
        List<BaseFlowNodeInstance> instances = flowInstance.filterInstanceNode(
                i -> FlowNodeType.APPROVAL_TASK == i.getNodeType());
        Assert.assertEquals(3, instances.size());
    }

    private FlowInstance createOdcInstance() {
        FlowInstance flowInstance = createFlowInstance("Test Instance");
        // risk level gateway
        FlowGatewayInstance gatewayInstance = createGatewayInstance(flowInstance.getId(), true, false);

        FlowApprovalInstance first_route_approval_1 = createApprovalInstance(flowInstance.getId(), 10);
        FlowGatewayInstance first_route_approval_1_gateway = createGatewayInstance(flowInstance.getId(), false, true);
        FlowApprovalInstance first_route_approval_2 = createApprovalInstance(flowInstance.getId(), 10);
        FlowGatewayInstance first_route_approval_2_gateway = createGatewayInstance(flowInstance.getId(), false, true);
        FlowTaskInstance first_route_task_1 = createTaskInstance(flowInstance.getId(), TaskType.ASYNC,
                ExecutionStrategyConfig.autoStrategy(), true);

        FlowInstanceConfigurer firstRoute_3 =
                flowInstance.newFlowInstanceConfigurer(first_route_task_1).endFlowInstance();
        FlowInstanceConfigurer firstRoute_2 =
                flowInstance.newFlowInstanceConfigurer(first_route_approval_2).next(first_route_approval_2_gateway)
                        .route(String.format("${%s}", FlowApprovalInstance.APPROVAL_VARIABLE_NAME), firstRoute_3)
                        .route(flowInstance.endFlowInstance());
        FlowInstanceConfigurer firstRoute_1 =
                flowInstance.newFlowInstanceConfigurer(first_route_approval_1).next(first_route_approval_1_gateway)
                        .route(String.format("${%s}", FlowApprovalInstance.APPROVAL_VARIABLE_NAME), firstRoute_2)
                        .route(flowInstance.endFlowInstance());

        FlowApprovalInstance second_route_approval_1 = createApprovalInstance(flowInstance.getId(), 10);
        FlowGatewayInstance second_route_approval_1_gateway = createGatewayInstance(flowInstance.getId(), false, true);
        FlowTaskInstance second_route_task_1 = createTaskInstance(flowInstance.getId(), TaskType.ASYNC,
                ExecutionStrategyConfig.manualStrategy(20, TimeUnit.SECONDS), true);

        FlowInstanceConfigurer secondRoute_2 =
                flowInstance.newFlowInstanceConfigurer(second_route_task_1).endFlowInstance();
        FlowInstanceConfigurer secondRoute_1 =
                flowInstance.newFlowInstanceConfigurer(second_route_approval_1).next(second_route_approval_1_gateway)
                        .route(String.format("${%s}", FlowApprovalInstance.APPROVAL_VARIABLE_NAME), secondRoute_2)
                        .route(flowInstance.endFlowInstance());

        flowInstance.newFlowInstance().next(gatewayInstance)
                .route("${level == 1}", secondRoute_1)
                .route(firstRoute_1)
                .and()
                .buildTopology();
        return flowInstance;
    }

    private FlowInstance createFlowInstanceWithTopo() {
        FlowInstance flowInstance = createFlowInstance("Test Instance");

        FlowGatewayInstance gatewayInstance = createGatewayInstance(flowInstance.getId(), true, false);
        FlowApprovalInstance firstRouteApproval = createApprovalInstance(flowInstance.getId(), 12);
        FlowTaskInstance firstRouteTask = createTaskInstance(flowInstance.getId(), TaskType.ASYNC,
                ExecutionStrategyConfig.autoStrategy(), false);

        FlowTaskInstance secondRouteTask = createTaskInstance(flowInstance.getId(), TaskType.MOCKDATA,
                ExecutionStrategyConfig.manualStrategy(12, TimeUnit.SECONDS), false);

        FlowGatewayInstance convergedGatewayInstance = createGatewayInstance(flowInstance.getId(), false, true);

        FlowInstanceConfigurer firstRoute =
                flowInstance.newFlowInstanceConfigurer(firstRouteApproval).next(firstRouteTask);
        FlowInstanceConfigurer secondRoute = flowInstance.newFlowInstanceConfigurer(secondRouteTask);

        flowInstance.newFlowInstance()
                .next(gatewayInstance)
                .route("${level == 1}", firstRoute)
                .route(secondRoute).and()
                .converge(Arrays.asList(firstRoute, secondRoute),
                        flowInstance.newFlowInstanceConfigurer(convergedGatewayInstance).endFlowInstance())
                .buildTopology();
        return flowInstance;
    }

    private FlowInstance createFlowInstance(String name) {
        return new FlowInstance(name, flowableAdaptor, authenticationFacade, flowInstanceRepository,
                nodeRepository, sequenceRepository, gateWayInstanceRepository, serviceTaskRepository,
                userTaskInstanceRepository, userTaskInstanceCandidateRepository, runtimeService, repositoryService);
    }

    private FlowTaskInstance createTaskInstance(Long flowInstanceId, TaskType taskType,
            ExecutionStrategyConfig config, boolean endEndPoint) {
        return new FlowTaskInstance(taskType, authenticationFacade.currentOrganizationId(),
                flowInstanceId, config, false, endEndPoint, type -> TestFlowRuntimeTaskImpl.class, flowableAdaptor,
                new LocalEventPublisher(), taskService, nodeRepository, sequenceRepository, serviceTaskRepository);
    }

    private FlowApprovalInstance createApprovalInstance(Long flowInstanceId, Integer expireIntervalSeconds) {
        return new FlowApprovalInstance(authenticationFacade.currentOrganizationId(),
                flowInstanceId, null, expireIntervalSeconds, false, false, false, flowableAdaptor, taskService,
                formService, new LocalEventPublisher(), authenticationFacade, nodeRepository, sequenceRepository,
                userTaskInstanceRepository, userTaskInstanceCandidateRepository);
    }

    private FlowGatewayInstance createGatewayInstance(Long flowInstanceId, boolean startEndPoint, boolean endEndPoint) {
        return new FlowGatewayInstance(authenticationFacade.currentOrganizationId(), flowInstanceId,
                startEndPoint, endEndPoint, flowableAdaptor, nodeRepository, sequenceRepository,
                gateWayInstanceRepository);
    }

}
