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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.metadb.flow.GateWayInstanceEntity;
import com.oceanbase.odc.metadb.flow.GateWayInstanceRepository;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntity;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntityRepository;
import com.oceanbase.odc.metadb.flow.SequenceInstanceRepository;
import com.oceanbase.odc.service.flow.instance.FlowGatewayInstance;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * Test cases for {@link FlowGatewayInstance}
 *
 * @author yh263208
 * @date 2022-02-17 22:11
 * @since ODC_release_3.3.0
 * @see ServiceTestEnv
 */
@Slf4j
public class FlowGatewayInstanceTest extends ServiceTestEnv {

    private final AtomicLong counter = new AtomicLong();
    @Autowired
    private FlowableAdaptor flowableAdaptor;
    @Autowired
    private NodeInstanceEntityRepository nodeRepository;
    @Autowired
    private SequenceInstanceRepository sequenceRepository;
    @Autowired
    private GateWayInstanceRepository gateWayInstanceRepository;

    @Before
    public void setUp() {
        gateWayInstanceRepository.deleteAll();
        nodeRepository.deleteAll();
        sequenceRepository.deleteAll();
    }

    @Test
    public void create_createFlowGatewayInstance_createSucceed() {
        FlowGatewayInstance instance = createGatewayInstance(false, true);

        List<GateWayInstanceEntity> entityList = gateWayInstanceRepository.findAll();
        Assert.assertEquals(1, entityList.size());
        Assert.assertEquals(instance.getId(), entityList.get(0).getId());
    }

    @Test
    public void delete_deleteFlowGatewayInstance_deleteSucceed() {
        FlowGatewayInstance instance = createGatewayInstance(true, false);
        NodeInstanceEntity nodeEntity = new NodeInstanceEntity();
        nodeEntity.setInstanceId(instance.getId());
        nodeEntity.setInstanceType(instance.getNodeType());
        nodeEntity.setFlowInstanceId(instance.getFlowInstanceId());
        nodeEntity.setActivityId(instance.getActivityId());
        nodeEntity.setName(instance.getName());
        nodeEntity.setFlowableElementType(instance.getCoreFlowableElementType());
        nodeRepository.save(nodeEntity);
        instance = copyFrom(instance);
        instance.delete();

        List<GateWayInstanceEntity> entityList = gateWayInstanceRepository.findAll();
        Assert.assertEquals(0, entityList.size());
    }

    @Test
    public void update_updateStatus_updateSucceed() {
        FlowGatewayInstance instance = createGatewayInstance(true, false);
        instance.setStatus(FlowNodeStatus.COMPLETED);
        instance.update();

        GateWayInstanceEntity entity = gateWayInstanceRepository.findById(instance.getId())
                .orElseThrow(IllegalStateException::new);
        Assert.assertEquals(instance.getStatus(), entity.getStatus());
    }

    private FlowGatewayInstance createGatewayInstance(boolean startEndPoint, boolean endEndPoint, String name,
            String activityId) {
        FlowGatewayInstance instance = new FlowGatewayInstance(1L, 1L, startEndPoint, endEndPoint,
                flowableAdaptor, nodeRepository, sequenceRepository, gateWayInstanceRepository);
        instance.create();
        instance.setActivityId(activityId);
        instance.setName(name);
        return instance;
    }

    private FlowGatewayInstance createGatewayInstance(boolean startEndPoint, boolean endEndPoint) {
        return createGatewayInstance(startEndPoint, endEndPoint, "gateway_" + counter.incrementAndGet(),
                UUID.randomUUID().toString());
    }

    private FlowGatewayInstance copyFrom(FlowGatewayInstance instance) {
        GateWayInstanceEntity entity = new GateWayInstanceEntity();
        entity.setId(instance.getId());
        entity.setOrganizationId(instance.getOrganizationId());
        entity.setStatus(instance.getStatus());
        entity.setFlowInstanceId(instance.getFlowInstanceId());
        entity.setCreateTime(instance.getCreateTime());
        entity.setUpdateTime(instance.getUpdateTime());
        entity.setStartEndpoint(instance.isStartEndpoint());
        entity.setEndEndpoint(instance.isEndEndPoint());
        return new FlowGatewayInstance(entity, flowableAdaptor, nodeRepository,
                sequenceRepository, gateWayInstanceRepository);
    }

}
