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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.core.flow.model.FlowableElementType;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.flow.GateWayInstanceEntity;
import com.oceanbase.odc.metadb.flow.GateWayInstanceRepository;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntityRepository;
import com.oceanbase.odc.metadb.flow.SequenceInstanceRepository;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.model.FlowNodeType;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Flow node instance, refers to {@link org.flowable.bpmn.model.Gateway}
 *
 * @author yh263208
 * @date 2022-02-17 21:42
 * @since ODC_release_3.3.0
 * @see BaseFlowNodeInstance
 */
@Slf4j
public class FlowGatewayInstance extends BaseFlowNodeInstance {

    private final GateWayInstanceRepository gateWayInstanceRepository;

    public FlowGatewayInstance(@NonNull GateWayInstanceEntity entity, @NonNull FlowableAdaptor flowableAdaptor,
            @NonNull NodeInstanceEntityRepository nodeRepository,
            @NonNull SequenceInstanceRepository sequenceRepository,
            @NonNull GateWayInstanceRepository gatewayInstanceRepository) {
        super(FlowNodeType.GATEWAY, entity.getId(), entity.getOrganizationId(), entity.getStatus(),
                entity.getFlowInstanceId(), entity.getCreateTime(), entity.getUpdateTime(), entity.isStartEndpoint(),
                entity.isEndEndpoint(), flowableAdaptor, nodeRepository, sequenceRepository);
        this.gateWayInstanceRepository = gatewayInstanceRepository;
        alloc();
    }

    public FlowGatewayInstance(@NonNull Long organizationId,
            @NonNull Long flowInstanceId, boolean startEndpoint, boolean endEndPoint,
            @NonNull FlowableAdaptor flowableAdaptor,
            @NonNull NodeInstanceEntityRepository nodeRepository,
            @NonNull SequenceInstanceRepository sequenceRepository,
            @NonNull GateWayInstanceRepository gatewayInstanceRepository) {
        super(FlowNodeType.GATEWAY, organizationId, flowInstanceId, startEndpoint, endEndPoint, flowableAdaptor,
                nodeRepository, sequenceRepository);
        this.gateWayInstanceRepository = gatewayInstanceRepository;
        alloc();
    }

    @Override
    public FlowableElementType getCoreFlowableElementType() {
        return FlowableElementType.EXCLUSIVE_GATEWAY;
    }

    public static List<FlowGatewayInstance> batchCreate(List<FlowGatewayInstance> instances,
            @NonNull GateWayInstanceRepository gateWayInstanceRepository) {
        if (CollectionUtils.isEmpty(instances)) {
            return Collections.emptyList();
        }
        List<GateWayInstanceEntity> entities = instances.stream()
                .map(FlowGatewayInstance::mapToGatewayEntity).collect(Collectors.toList());
        entities = gateWayInstanceRepository.batchCreate(entities);
        for (int i = 0; i < instances.size(); i++) {
            instances.get(i).setId(entities.get(i).getId());
            instances.get(i).createTime = entities.get(i).getCreateTime();
            instances.get(i).updateTime = entities.get(i).getUpdateTime();
        }
        return instances;
    }

    @Override
    public void create() {
        validNotExists();
        GateWayInstanceEntity entity = mapToGatewayEntity(this);
        entity = gateWayInstanceRepository.save(entity);
        Verify.notNull(entity.getId(), "id");
        Verify.notNull(entity.getCreateTime(), "CreateTime");
        Verify.notNull(entity.getUpdateTime(), "UpdateTime");
        this.id = entity.getId();
        this.createTime = entity.getCreateTime();
        this.updateTime = entity.getUpdateTime();
        log.info("Create gateway instance successfully, gateway={}", entity);
    }

    @Override
    public void update() {
        validExists();
        int affectRows = gateWayInstanceRepository.updateStatusById(getId(), getStatus());
        log.info("Update gateway instance successfully, affectRows={}", affectRows);
    }

    @Override
    protected void doDelete(@NonNull Long instanceId) {
        gateWayInstanceRepository.deleteById(instanceId);
        log.info("Delete gateway instance successfully, gatewayInstanceId={}", instanceId);
    }

    @Override
    public boolean isPresentOnThisMachine() {
        return true;
    }

    @Override
    public void dealloc() {}

    @Override
    public void alloc() {}

    @Override
    public String resourceType() {
        return ResourceType.ODC_FLOW_GATEWAY_INSTANCE.name();
    }

    private static GateWayInstanceEntity mapToGatewayEntity(FlowGatewayInstance instance) {
        GateWayInstanceEntity entity = new GateWayInstanceEntity();
        entity.setOrganizationId(instance.getOrganizationId());
        entity.setStatus(instance.getStatus());
        entity.setStartEndpoint(instance.isStartEndpoint());
        entity.setEndEndpoint(instance.isEndEndPoint());
        entity.setFlowInstanceId(instance.getFlowInstanceId());
        return entity;
    }

}
