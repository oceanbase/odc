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

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.flowable.engine.TaskService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.common.event.LocalEventPublisher;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntity;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntityRepository;
import com.oceanbase.odc.metadb.flow.SequenceInstanceRepository;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceEntity;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;
import com.oceanbase.odc.service.flow.model.ExecutionStrategyConfig;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * Test cases for {@link FlowTaskInstance}
 *
 * @author yh263208
 * @date 2022-02-15 18:53
 * @since ODC_release_3.3.0
 */
@Slf4j
public class FlowTaskInstanceTest extends ServiceTestEnv {

    private final AtomicLong counter = new AtomicLong();
    @Autowired
    private FlowableAdaptor flowableAdaptor;
    @Autowired
    private NodeInstanceEntityRepository nodeRepository;
    @Autowired
    private SequenceInstanceRepository sequenceRepository;
    @Autowired
    private ServiceTaskInstanceRepository serviceTaskRepository;
    @Autowired
    private TaskService taskService;

    @Before
    public void setUp() {
        serviceTaskRepository.deleteAll();
        nodeRepository.deleteAll();
        sequenceRepository.deleteAll();
    }

    @Test
    public void create_createFlowTaskInstance_createSucceed() {
        FlowTaskInstance instance = createTaskInstance(false, true);

        List<ServiceTaskInstanceEntity> entityList = serviceTaskRepository.findAll();
        Assert.assertEquals(1, entityList.size());
        Assert.assertEquals(instance.getId(), entityList.get(0).getId());
    }

    @Test
    public void delete_deleteFlowTaskInstance_deleteSucceed() {
        FlowTaskInstance taskInstance = createTaskInstance(true, false);
        NodeInstanceEntity nodeEntity = new NodeInstanceEntity();
        nodeEntity.setInstanceId(taskInstance.getId());
        nodeEntity.setInstanceType(taskInstance.getNodeType());
        nodeEntity.setFlowInstanceId(taskInstance.getFlowInstanceId());
        nodeEntity.setActivityId(taskInstance.getActivityId());
        nodeEntity.setName(taskInstance.getName());
        nodeEntity.setFlowableElementType(taskInstance.getCoreFlowableElementType());
        nodeRepository.save(nodeEntity);
        taskInstance = copyFrom(taskInstance);
        taskInstance.delete();

        List<ServiceTaskInstanceEntity> entityList = serviceTaskRepository.findAll();
        Assert.assertEquals(0, entityList.size());
    }

    @Test
    public void update_updateStatusAndTargetTaskId_updateSucceed() {
        FlowTaskInstance taskInstance = createTaskInstance(true, false);
        taskInstance.setStatus(FlowNodeStatus.COMPLETED);
        Random random = new Random();
        Long taskId = random.nextLong();
        taskInstance.setTargetTaskId(taskId);
        taskInstance.update();

        ServiceTaskInstanceEntity entity = serviceTaskRepository.findById(taskInstance.getId())
                .orElseThrow(IllegalStateException::new);
        Assert.assertEquals(taskInstance.getStatus(), entity.getStatus());
        Assert.assertEquals(taskId, entity.getTargetTaskId());
    }

    private FlowTaskInstance createTaskInstance(boolean startEndPoint, boolean endEndPoint,
            String name, String activityId) {
        FlowTaskInstance instance = new FlowTaskInstance(TaskType.ASYNC, 1L, 1L,
                ExecutionStrategyConfig.autoStrategy(), startEndPoint, endEndPoint, taskType -> null,
                flowableAdaptor, new LocalEventPublisher(), taskService, nodeRepository,
                sequenceRepository, serviceTaskRepository);
        instance.create();
        instance.setActivityId(activityId);
        instance.setName(name);
        return instance;
    }

    private FlowTaskInstance createTaskInstance(boolean startEndPoint, boolean endEndPoint) {
        return createTaskInstance(startEndPoint, endEndPoint, "service_task_" + counter.incrementAndGet(),
                UUID.randomUUID().toString());
    }

    private FlowTaskInstance copyFrom(FlowTaskInstance instance) {
        ServiceTaskInstanceEntity entity = new ServiceTaskInstanceEntity();
        entity.setId(instance.getId());
        entity.setOrganizationId(instance.getOrganizationId());
        entity.setStatus(instance.getStatus());
        entity.setFlowInstanceId(instance.getFlowInstanceId());
        entity.setCreateTime(instance.getCreateTime());
        entity.setUpdateTime(instance.getUpdateTime());
        entity.setStartEndpoint(instance.isStartEndpoint());
        entity.setEndEndpoint(instance.isEndEndPoint());
        entity.setStrategy(instance.getStrategyConfig().getStrategy());
        entity.setTaskType(instance.getTaskType());
        entity.setWaitExecExpireIntervalSeconds(instance.getStrategyConfig().getPendingExpireIntervalSeconds());
        return new FlowTaskInstance(entity, taskType -> null, flowableAdaptor, new LocalEventPublisher(),
                taskService, nodeRepository, sequenceRepository, serviceTaskRepository);
    }

}
