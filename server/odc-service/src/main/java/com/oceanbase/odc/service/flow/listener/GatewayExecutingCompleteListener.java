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
package com.oceanbase.odc.service.flow.listener;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.core.flow.BaseTaskListener;
import com.oceanbase.odc.metadb.flow.GateWayInstanceRepository;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.instance.FlowGatewayInstance;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * This {@link GatewayExecutingCompleteListener} is used to monitor the execution of
 * {@code Flowable} gateway {@link org.flowable.bpmn.model.Gateway}, and associate the gateway
 * instance with the {@link FlowGatewayInstance} when the task is start
 *
 * @author yh263208
 * @date 2022-02-16 21:58
 * @since ODC_release_3.3.0
 * @see BaseTaskListener
 */
@Slf4j
public class GatewayExecutingCompleteListener extends BaseStatusModifyListener<FlowGatewayInstance> {

    @Autowired
    private GateWayInstanceRepository gatewayInstanceRepository;

    /**
     * Since the {@link org.flowable.bpmn.model.ExclusiveGateway} does not conceptually have the state
     * of {@code FlowNodeStatus.EXECUTING}, the gateway state is only modified to
     * {@code FlowNodeStatus.COMPLETED} here, and since the mutual
     * {@link org.flowable.bpmn.model.ExclusiveGateway} of {@code Flowable} will not call the
     * {@link #doModifyStatusOnEnd(FlowGatewayInstance)} during execution, the state modification is
     * made in the {@link #doModifyStatusOnStart(FlowGatewayInstance)}.
     */
    @Override
    protected FlowNodeStatus doModifyStatusOnStart(FlowGatewayInstance target) {
        return internalModify(target, FlowNodeStatus.COMPLETED);
    }

    @Override
    protected FlowNodeStatus doModifyStatusOnEnd(FlowGatewayInstance target) {
        return internalModify(target, FlowNodeStatus.COMPLETED);
    }

    @Override
    protected Optional<FlowGatewayInstance> getTargetByActivityId(@NonNull String activityId,
            @NonNull Long flowInstanceId, @NonNull FlowableAdaptor flowableAdaptor) {
        return flowableAdaptor.getGatewayInstanceByActivityId(activityId, flowInstanceId);
    }

    private FlowNodeStatus internalModify(FlowGatewayInstance target, FlowNodeStatus status) {
        int affectRows = gatewayInstanceRepository.updateStatusById(target.getId(), status);
        log.info("Modify node instance status successfully, instanceId={}, instanceType={}, affectRows={}",
                target.getId(), target.getNodeType(), affectRows);
        return status;
    }

}
