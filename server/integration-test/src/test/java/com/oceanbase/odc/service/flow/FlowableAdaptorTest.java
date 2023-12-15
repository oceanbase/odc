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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.flowable.engine.FormService;
import org.flowable.engine.TaskService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.common.event.LocalEventPublisher;
import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.core.flow.model.FlowableElement;
import com.oceanbase.odc.core.flow.model.FlowableElementType;
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
import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;
import com.oceanbase.odc.service.flow.model.ExecutionStrategyConfig;
import com.oceanbase.odc.service.flow.model.FlowNodeType;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

/**
 * Test case for {@link FlowableAdaptor}
 *
 * @author yh263208
 * @date 2022-02-22 15:33
 * @since ODC_release_3.3.0
 * @see ServiceTestEnv
 */
public class FlowableAdaptorTest extends ServiceTestEnv {

    @MockBean
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private FlowableAdaptor flowableAdaptor;
    @Autowired
    private NodeInstanceEntityRepository nodeInstanceEntityRepository;
    @Autowired
    private FlowInstanceRepository flowInstanceRepository;
    @Autowired
    private SequenceInstanceRepository sequenceRepository;
    @Autowired
    private GateWayInstanceRepository gateWayInstanceRepository;
    @Autowired
    private TaskService taskService;
    @Autowired
    private FormService formService;
    @Autowired
    private UserTaskInstanceRepository userTaskInstanceRepository;
    @Autowired
    private ServiceTaskInstanceRepository serviceTaskRepository;
    @Autowired
    private UserTaskInstanceCandidateRepository userTaskInstanceCandidateRepository;

    @Before
    public void setUp() {
        nodeInstanceEntityRepository.deleteAll();
        flowInstanceRepository.deleteAll();
        nodeInstanceEntityRepository.deleteAll();
        sequenceRepository.deleteAll();
        gateWayInstanceRepository.deleteAll();
        userTaskInstanceRepository.deleteAll();
        sequenceRepository.deleteAll();
        when(authenticationFacade.currentUserId()).thenReturn(10000L);
        when(authenticationFacade.currentOrganizationId()).thenReturn(1L);
    }

    @Test
    public void set_setProcessInstanceId_successSet() {
        FlowInstanceEntity entity = createFlowInstanceEntity();
        String processInstanceId = UUID.randomUUID().toString();
        flowableAdaptor.setProcessInstanceId(entity.getId(), processInstanceId);
        Long id = flowableAdaptor.getFlowInstanceIdByProcessInstanceId(processInstanceId)
                .orElseThrow(IllegalStateException::new);
        Assert.assertEquals(entity.getId(), id);
    }

    @Test
    public void set_setProcessDefinitionId_successSet() {
        FlowInstanceEntity entity = createFlowInstanceEntity();
        String processDefinitionId = UUID.randomUUID().toString();
        flowableAdaptor.setProcessDefinitionId(entity.getId(), processDefinitionId);
        Long id = flowableAdaptor.getFlowInstanceIdByProcessDefinitionId(processDefinitionId)
                .orElseThrow(IllegalStateException::new);
        Assert.assertEquals(entity.getId(), id);
    }

    @Test
    public void get_getFlowGatewayInstanceByActivityId_successGot() {
        FlowInstanceEntity flowInstanceEntity = createFlowInstanceEntity();
        FlowGatewayInstance gatewayInstance = createGatewayInstance(flowInstanceEntity.getId());

        String activityId = UUID.randomUUID().toString();
        flowableAdaptor.setFlowableElements(Collections.singletonList(new Pair<>(gatewayInstance,
                new FlowableElement(activityId, UUID.randomUUID().toString(), FlowableElementType.EXCLUSIVE_GATEWAY))));

        FlowGatewayInstance actual =
                flowableAdaptor.getGatewayInstanceByActivityId(activityId, flowInstanceEntity.getId())
                        .orElseThrow(IllegalStateException::new);
        Assert.assertEquals(gatewayInstance.getId(), actual.getId());
    }

    @Test
    public void setFlowableElements_setSeveralSameElts_saveOnlyOneSucceed() {
        FlowInstanceEntity flowInstanceEntity = createFlowInstanceEntity();
        FlowGatewayInstance gatewayInstance = createGatewayInstance(flowInstanceEntity.getId());

        String activityId = UUID.randomUUID().toString();
        Pair<BaseFlowNodeInstance, FlowableElement> elt = new Pair<>(gatewayInstance,
                new FlowableElement(activityId, UUID.randomUUID().toString(), FlowableElementType.EXCLUSIVE_GATEWAY));
        flowableAdaptor.setFlowableElements(Collections.singletonList(elt));
        List<Pair<BaseFlowNodeInstance, FlowableElement>> elts = new ArrayList<>();
        elts.add(elt);
        elts.add(elt);
        flowableAdaptor.setFlowableElements(elts);

        Assert.assertEquals(1, nodeInstanceEntityRepository.findAll().size());
    }

    @Test
    public void get_getFlowApprovalTaskByAcitvityId_successGot() {
        FlowInstanceEntity flowInstanceEntity = createFlowInstanceEntity();
        FlowApprovalInstance approvalInstance = createApprovalInstance(flowInstanceEntity.getId());

        String activityId = UUID.randomUUID().toString();
        flowableAdaptor.setFlowableElements(Collections.singletonList(new Pair<>(approvalInstance,
                new FlowableElement(activityId, UUID.randomUUID().toString(), FlowableElementType.USER_TASK))));

        FlowApprovalInstance actual =
                flowableAdaptor.getApprovalInstanceByActivityId(activityId, flowInstanceEntity.getId())
                        .orElseThrow(IllegalStateException::new);
        Assert.assertEquals(approvalInstance.getId(), actual.getId());
    }

    @Test
    public void get_getFlowApprovalTaskByName_successGot() {
        FlowInstanceEntity flowInstanceEntity = createFlowInstanceEntity();
        FlowApprovalInstance approvalInstance = createApprovalInstance(flowInstanceEntity.getId());

        String name = UUID.randomUUID().toString();
        flowableAdaptor.setFlowableElements(Collections.singletonList(new Pair<>(approvalInstance,
                new FlowableElement(UUID.randomUUID().toString(), name, FlowableElementType.USER_TASK))));

        FlowApprovalInstance actual = flowableAdaptor.getApprovalInstanceByName(name, flowInstanceEntity.getId())
                .orElseThrow(IllegalStateException::new);
        Assert.assertEquals(approvalInstance.getId(), actual.getId());
    }

    @Test
    public void get_getFlowTaskByAcitvityId_successGot() {
        FlowInstanceEntity flowInstanceEntity = createFlowInstanceEntity();
        FlowTaskInstance taskInstance = createTaskInstance(flowInstanceEntity.getId());

        String activityId = UUID.randomUUID().toString();
        flowableAdaptor.setFlowableElements(Collections.singletonList(new Pair<>(taskInstance,
                new FlowableElement(activityId, UUID.randomUUID().toString(), FlowableElementType.SERVICE_TASK))));

        FlowTaskInstance actual = flowableAdaptor.getTaskInstanceByActivityId(activityId, flowInstanceEntity.getId())
                .orElseThrow(IllegalStateException::new);
        Assert.assertEquals(taskInstance.getId(), actual.getId());
    }

    @Test
    public void get_getFlowableElements_successGot() {
        FlowInstanceEntity flowInstanceEntity = createFlowInstanceEntity();

        FlowTaskInstance taskInstance = createTaskInstance(flowInstanceEntity.getId());
        String serviceActivityId = UUID.randomUUID().toString();
        String serviceName = UUID.randomUUID().toString();
        flowableAdaptor.setFlowableElements(Collections.singletonList(new Pair<>(taskInstance,
                new FlowableElement(serviceActivityId, serviceName, FlowableElementType.SERVICE_TASK))));
        String signalActivityId = UUID.randomUUID().toString();
        String signalName = UUID.randomUUID().toString();
        flowableAdaptor.setFlowableElements(Collections.singletonList(new Pair<>(taskInstance,
                new FlowableElement(signalActivityId, signalName, FlowableElementType.SIGNAL_CATCH_EVENT))));
        String signalActivityId1 = UUID.randomUUID().toString();
        String signalName1 = UUID.randomUUID().toString();
        flowableAdaptor.setFlowableElements(Collections.singletonList(new Pair<>(taskInstance,
                new FlowableElement(signalActivityId1, signalName1, FlowableElementType.SIGNAL_CATCH_EVENT))));

        FlowApprovalInstance approvalInstance = createApprovalInstance(flowInstanceEntity.getId());
        String userTaskActivityId = UUID.randomUUID().toString();
        String userTaskName = UUID.randomUUID().toString();
        flowableAdaptor.setFlowableElements(Collections.singletonList(new Pair<>(approvalInstance,
                new FlowableElement(userTaskActivityId, userTaskName, FlowableElementType.USER_TASK))));

        List<FlowableElement> elementList = flowableAdaptor.getFlowableElementByType(taskInstance.getId(),
                FlowNodeType.SERVICE_TASK, FlowableElementType.SIGNAL_CATCH_EVENT);
        Assert.assertEquals(2, elementList.size());
    }

    private FlowGatewayInstance createGatewayInstance(Long flowInstanceId) {
        FlowGatewayInstance inst = new FlowGatewayInstance(1L, flowInstanceId, true, true, flowableAdaptor,
                nodeInstanceEntityRepository, sequenceRepository, gateWayInstanceRepository);
        inst.create();
        return inst;
    }

    private FlowApprovalInstance createApprovalInstance(Long flowInstanceId) {
        FlowApprovalInstance inst = new FlowApprovalInstance(1L, flowInstanceId, null, 12, true, true, false,
                flowableAdaptor, taskService, formService, new LocalEventPublisher(), authenticationFacade,
                nodeInstanceEntityRepository, sequenceRepository, userTaskInstanceRepository,
                userTaskInstanceCandidateRepository);
        inst.create();
        return inst;
    }

    private FlowTaskInstance createTaskInstance(Long flowInstanceId) {
        FlowTaskInstance inst = new FlowTaskInstance(TaskType.ASYNC, 1L, flowInstanceId,
                ExecutionStrategyConfig.autoStrategy(),
                true, true, taskType -> null, flowableAdaptor, new LocalEventPublisher(), taskService,
                nodeInstanceEntityRepository, sequenceRepository, serviceTaskRepository);
        inst.create();
        return inst;
    }

    private FlowInstanceEntity createFlowInstanceEntity() {
        FlowInstanceEntity entity = new FlowInstanceEntity();
        entity.setCreatorId(1L);
        entity.setProjectId(1L);
        entity.setFlowConfigId(2L);
        entity.setFlowConfigSnapshotXml("Test snapshot");
        entity.setName("Test Flow Instance");
        entity.setOrganizationId(1L);
        entity.setStatus(FlowStatus.CREATED);
        entity = flowInstanceRepository.save(entity);
        return entity;
    }

}
