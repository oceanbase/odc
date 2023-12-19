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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.flowable.engine.FormService;
import org.flowable.engine.TaskService;
import org.flowable.task.service.impl.TaskQueryImpl;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.common.event.LocalEventPublisher;
import com.oceanbase.odc.core.flow.graph.GraphEdge;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntity;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntityRepository;
import com.oceanbase.odc.metadb.flow.SequenceInstanceEntity;
import com.oceanbase.odc.metadb.flow.SequenceInstanceRepository;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceCandidateRepository;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceEntity;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceRepository;
import com.oceanbase.odc.service.flow.instance.FlowApprovalInstance;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.model.FlowNodeType;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

import lombok.extern.slf4j.Slf4j;

/**
 * Test cases for {@link FlowApprovalInstance}
 *
 * @author yh263208
 * @date 2022-02-17 14:05
 * @since ODC_release_3.3.0
 * @see ServiceTestEnv
 */
@Slf4j
public class FlowApprovalInstanceTest extends ServiceTestEnv {

    private final AtomicLong counter = new AtomicLong();
    @MockBean
    private AuthenticationFacade authenticationFacade;
    @MockBean
    private TaskService taskService;
    @MockBean
    private FormService formService;
    @Autowired
    private NodeInstanceEntityRepository nodeRepository;
    @Autowired
    private SequenceInstanceRepository sequenceRepository;
    @Autowired
    private UserTaskInstanceRepository userTaskInstanceRepository;
    @Autowired
    private FlowableAdaptor flowableAdaptor;
    @Autowired
    private UserTaskInstanceCandidateRepository userTaskInstanceCandidateRepository;

    @Before
    public void setUp() {
        when(authenticationFacade.currentUserId()).thenReturn(1L);
        userTaskInstanceRepository.deleteAll();
        nodeRepository.deleteAll();
        sequenceRepository.deleteAll();
        TaskQueryImpl singleTaskQuery = Mockito.mock(TaskQueryImpl.class);
        Mockito.when(singleTaskQuery.singleResult()).thenReturn(new TaskEntityImpl());

        TaskQueryImpl taskQuery = Mockito.mock(TaskQueryImpl.class);
        Mockito.when(taskQuery.taskId(Mockito.anyString())).thenReturn(singleTaskQuery);

        Mockito.when(taskService.createTaskQuery()).thenReturn(taskQuery);
        Mockito.when(formService.getTaskFormData(Mockito.anyString())).thenReturn(null);
    }

    @Test
    public void create_createFlowApprovalInstance_createSucceed() {
        FlowApprovalInstance instance = createApprovalInstance(false, true);

        List<UserTaskInstanceEntity> entityList = userTaskInstanceRepository.findAll();
        Assert.assertEquals(1, entityList.size());
        Assert.assertEquals(instance.getId(), entityList.get(0).getId());
    }

    @Test
    public void delete_deleteFlowApprovalInstance_deleteSucceed() {
        FlowApprovalInstance taskInstance = createApprovalInstance(true, false);
        NodeInstanceEntity nodeEntity = new NodeInstanceEntity();
        nodeEntity.setInstanceId(taskInstance.getId());
        nodeEntity.setInstanceType(taskInstance.getNodeType());
        nodeEntity.setFlowInstanceId(taskInstance.getFlowInstanceId());
        nodeEntity.setActivityId(taskInstance.getActivityId());
        nodeEntity.setName(taskInstance.getName());
        nodeEntity.setFlowableElementType(taskInstance.getCoreFlowableElementType());
        nodeRepository.save(nodeEntity);
        taskInstance = copyFrom(taskInstance);

        Assert.assertTrue(taskInstance.delete());
        List<UserTaskInstanceEntity> entityList = userTaskInstanceRepository.findAll();
        Assert.assertEquals(0, entityList.size());
    }

    @Test
    public void delete_deleteEndApprovalInstanceWithTopoExists_beginNodeLeft() {
        FlowApprovalInstance begin = createApprovalInstance(true, false);
        FlowApprovalInstance end = createApprovalInstance(false, true);
        next(begin, end);
        end.delete();
        List<NodeInstanceEntity> ends = nodeRepository.findByInstanceIdAndInstanceTypeAndFlowableElementType(
                end.getId(), FlowNodeType.APPROVAL_TASK, end.getCoreFlowableElementType());

        Assert.assertTrue(ends.isEmpty());
        List<SequenceInstanceEntity> entities = sequenceRepository.findAll();
        Assert.assertEquals(0, entities.size());
    }

    @Test
    public void delete_deleteBeginApprovalInstanceWithTopoExists_endNodeLeft() {
        FlowApprovalInstance begin = createApprovalInstance(true, false);
        FlowApprovalInstance end = createApprovalInstance(false, true);
        next(begin, end);
        begin.delete();
        List<NodeInstanceEntity> begins = nodeRepository.findByInstanceIdAndInstanceTypeAndFlowableElementType(
                begin.getId(), FlowNodeType.APPROVAL_TASK, begin.getCoreFlowableElementType());

        Assert.assertTrue(begins.isEmpty());
        List<SequenceInstanceEntity> entities = sequenceRepository.findAll();
        Assert.assertEquals(0, entities.size());
    }

    @Test
    public void update_updateStatusAndUserTaskId_updateSucceed() {
        FlowApprovalInstance expect = createApprovalInstance(true, false);
        expect.setStatus(FlowNodeStatus.COMPLETED);
        expect.setUserTaskId(UUID.randomUUID().toString());
        expect.update();

        UserTaskInstanceEntity actual = userTaskInstanceRepository.findById(expect.getId())
                .orElseThrow(IllegalStateException::new);
        Assert.assertEquals(expect.getStatus(), actual.getStatus());
        Assert.assertEquals(expect.getUserTaskId(), actual.getUserTaskId());
    }

    @Test
    public void disApprove_disApproveWithComment_disApproveSucceed() {
        FlowApprovalInstance instance = createApprovalInstance(true, false);
        instance.setUserTaskId("123");
        String comment = "dis-approve comment";
        instance.disApprove(comment, true);

        UserTaskInstanceEntity actual = userTaskInstanceRepository.findById(instance.getId())
                .orElseThrow(IllegalStateException::new);
        Assert.assertEquals(FlowNodeStatus.COMPLETED, actual.getStatus());
        Assert.assertFalse(actual.isApproved());
        Assert.assertEquals(comment, actual.getComment());
    }

    @Test
    public void approve_approveWithComment_approveSucceed() {
        FlowApprovalInstance instance = createApprovalInstance(true, false);
        instance.setUserTaskId("123");
        String comment = "approve comment";
        instance.approve(comment, true);

        UserTaskInstanceEntity actual = userTaskInstanceRepository.findById(instance.getId())
                .orElseThrow(IllegalStateException::new);
        Assert.assertEquals(FlowNodeStatus.COMPLETED, actual.getStatus());
        Assert.assertTrue(actual.isApproved());
        Assert.assertEquals(comment, actual.getComment());
    }

    private FlowApprovalInstance createApprovalInstance(boolean startEndPoint, boolean endEndPoint, String name,
            String activityId, Long flowInstanceId) {
        FlowApprovalInstance instance = new FlowApprovalInstance(1L, flowInstanceId, null, 12, startEndPoint,
                endEndPoint, false, flowableAdaptor, taskService, formService, new LocalEventPublisher(),
                authenticationFacade, nodeRepository, sequenceRepository, userTaskInstanceRepository,
                userTaskInstanceCandidateRepository);
        instance.create();
        instance.setName(name);
        instance.setActivityId(activityId);
        return instance;
    }

    private FlowApprovalInstance createApprovalInstance(boolean startEndPoint, boolean endEndPoint) {
        return createApprovalInstance(startEndPoint, endEndPoint, "approval_task_" + counter.incrementAndGet(),
                UUID.randomUUID().toString(), counter.incrementAndGet());
    }

    private FlowApprovalInstance copyFrom(FlowApprovalInstance instance) {
        UserTaskInstanceEntity entity = new UserTaskInstanceEntity();
        entity.setId(instance.getId());
        entity.setUserTaskId(instance.getUserTaskId());
        entity.setOrganizationId(instance.getOrganizationId());
        entity.setStatus(instance.getStatus());
        entity.setOperatorId(instance.getOperatorId());
        entity.setComment(instance.getComment());
        entity.setApproved(instance.isApproved());
        entity.setFlowInstanceId(instance.getFlowInstanceId());
        entity.setCreateTime(instance.getCreateTime());
        entity.setUpdateTime(instance.getUpdateTime());
        entity.setStartEndpoint(instance.isStartEndpoint());
        entity.setEndEndpoint(instance.isEndEndPoint());
        entity.setExpireIntervalSeconds(instance.getExpireIntervalSeconds());
        return new FlowApprovalInstance(entity, flowableAdaptor, taskService, formService, new LocalEventPublisher(),
                authenticationFacade, nodeRepository, sequenceRepository, userTaskInstanceRepository,
                userTaskInstanceCandidateRepository);
    }

    private void next(FlowApprovalInstance from, FlowApprovalInstance to) {
        GraphEdge edge = new GraphEdge(UUID.randomUUID().toString(), "begin -> end");
        edge.setFrom(from);
        edge.setTo(to);
        from.addOutEdge(edge);
        to.addInEdge(edge);
    }

}
