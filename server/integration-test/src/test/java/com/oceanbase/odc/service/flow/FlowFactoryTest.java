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
import java.util.HashSet;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.common.event.LocalEventPublisher;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.flow.GateWayInstanceRepository;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntityRepository;
import com.oceanbase.odc.metadb.flow.SequenceInstanceRepository;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceCandidateRepository;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceRepository;
import com.oceanbase.odc.service.flow.factory.FlowFactory;
import com.oceanbase.odc.service.flow.instance.FlowApprovalInstance;
import com.oceanbase.odc.service.flow.instance.FlowGatewayInstance;
import com.oceanbase.odc.service.flow.instance.FlowInstance;
import com.oceanbase.odc.service.flow.instance.FlowSequenceInstance;
import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;
import com.oceanbase.odc.service.flow.model.ExecutionStrategyConfig;
import com.oceanbase.odc.service.flow.tool.TestFlowRuntimeTaskImpl;
import com.oceanbase.odc.service.flow.util.FlowInstanceUtil;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

/**
 * Test cases for {@link FlowFactory}
 *
 * @author yh263208
 * @date 2022-03-04 11:38
 * @since ODC_release_3.3.0
 * @see ServiceTestEnv
 */
public class FlowFactoryTest extends ServiceTestEnv {

    @MockBean
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private FlowFactory flowFactory;
    @Autowired
    private FlowInstanceRepository flowInstanceRepository;
    @Autowired
    private NodeInstanceEntityRepository nodeRepository;
    @Autowired
    private SequenceInstanceRepository sequenceRepository;
    @Autowired
    private ServiceTaskInstanceRepository serviceTaskRepository;
    @Autowired
    private UserTaskInstanceRepository userTaskInstanceRepository;
    @Autowired
    private FlowableAdaptor flowAdaptor;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private FormService formService;
    @Autowired
    private GateWayInstanceRepository gateWayInstanceRepository;
    @Autowired
    private UserTaskInstanceCandidateRepository userTaskInstanceCandidateRepository;
    private final AtomicLong counter = new AtomicLong(0L);

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
    public void generateFlowInstance_createNewOne_returnFlowInstance() {
        String name = "Test FlowInstance";
        FlowInstance flowInstance = flowFactory.generateFlowInstance(name, "Test");

        Assert.assertNotNull(flowInstance.getId());
    }

    @Test
    public void generateFlowGatewayInstance_createNewOne_returnFlowGatewayInstance() {
        FlowInstance flowInstance = flowFactory.generateFlowInstance("Test FlowInstance", "Test");
        FlowGatewayInstance gatewayInstance =
                flowFactory.generateFlowGatewayInstance(flowInstance.getId(), true, false);

        Assert.assertEquals((long) flowInstance.getId(), gatewayInstance.getFlowInstanceId());
    }

    @Test
    public void generateFlowApprovalInstance_createNewOne_returnFlowApprovalInstance() {
        FlowInstance flowInstance = flowFactory.generateFlowInstance("Test FlowInstance", "Test");
        FlowApprovalInstance approvalInstance =
                flowFactory.generateFlowApprovalInstance(flowInstance.getId(), true, false, false, 15, null);

        Assert.assertEquals((long) flowInstance.getId(), approvalInstance.getFlowInstanceId());
    }

    @Test
    public void generateFlowTaskInstance_createNewOne_returnFlowTaskInstance() {
        FlowInstance flowInstance = flowFactory.generateFlowInstance("Test FlowInstance", "Test");
        FlowTaskInstance taskInstance = flowFactory.generateFlowTaskInstance(flowInstance.getId(), true, false,
                TaskType.ASYNC, ExecutionStrategyConfig.autoStrategy());

        Assert.assertEquals((long) flowInstance.getId(), taskInstance.getFlowInstanceId());
    }

    @Test
    public void getFlowInstance_loadFromDB_returnFlowInstance() {
        FlowInstance flowInstance = createFlowInstance();
        buildFlowInstance(flowInstance);

        Optional<FlowInstance> optional = flowFactory.getFlowInstance(flowInstance.getId());
        Assert.assertTrue(optional.isPresent());
        Assert.assertEquals(9, optional.get().getNodeInstanceCount());
        Assert.assertEquals(8, optional.get().getSequenceCount());
    }

    private void buildFlowInstance(FlowInstance flowInstance) {
        FlowGatewayInstance gatewayInstance = createGatewayInstance(flowInstance.getId(), true, false);
        FlowApprovalInstance first_route_approval_1 = createApprovalInstance(flowInstance.getId());
        FlowGatewayInstance first_route_approval_1_gateway = createGatewayInstance(flowInstance.getId(), false, true);
        FlowApprovalInstance first_route_approval_2 = createApprovalInstance(flowInstance.getId());
        FlowGatewayInstance first_route_approval_2_gateway = createGatewayInstance(flowInstance.getId(), false, true);
        FlowTaskInstance first_route_task_1 = createTaskInstance(flowInstance.getId(),
                ExecutionStrategyConfig.autoStrategy());
        FlowApprovalInstance second_route_approval_1 = createApprovalInstance(flowInstance.getId());
        FlowGatewayInstance second_route_approval_1_gateway = createGatewayInstance(flowInstance.getId(), false, true);
        FlowTaskInstance second_route_task_1 = createTaskInstance(flowInstance.getId(),
                ExecutionStrategyConfig.manualStrategy(20, TimeUnit.SECONDS));
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
        return new FlowInstance("Test Instance", flowAdaptor, authenticationFacade,
                flowInstanceRepository, nodeRepository, sequenceRepository, gateWayInstanceRepository,
                serviceTaskRepository, userTaskInstanceRepository, userTaskInstanceCandidateRepository,
                runtimeService, repositoryService);
    }

    private FlowTaskInstance createTaskInstance(Long flowInstanceId, ExecutionStrategyConfig config) {
        FlowTaskInstance inst = new FlowTaskInstance(TaskType.ASYNC, authenticationFacade.currentOrganizationId(),
                flowInstanceId, config, false, true, type -> TestFlowRuntimeTaskImpl.class, flowAdaptor,
                new LocalEventPublisher(), taskService, nodeRepository, sequenceRepository, serviceTaskRepository);
        inst.setId(counter.incrementAndGet());
        return inst;
    }

    private FlowApprovalInstance createApprovalInstance(Long flowInstanceId) {
        FlowApprovalInstance inst = new FlowApprovalInstance(authenticationFacade.currentOrganizationId(),
                flowInstanceId, null, 10, false, false, false, flowAdaptor, taskService, formService,
                new LocalEventPublisher(), authenticationFacade, nodeRepository, sequenceRepository,
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

}
