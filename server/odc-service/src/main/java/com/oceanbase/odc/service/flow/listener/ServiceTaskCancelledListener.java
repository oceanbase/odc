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

import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Execute the listener when the task execution cancelled and set the task status to cancelled
 *
 * @author yh263208
 * @date 2022-03-01 15:37
 * @since ODC_release_3.3.0
 * @see BaseStatusModifyListener
 */
@Slf4j
public class ServiceTaskCancelledListener extends BaseStatusModifyListener<FlowTaskInstance> {

    @Autowired
    private FlowInstanceRepository flowInstanceRepository;
    @Autowired
    private ServiceTaskInstanceRepository serviceTaskRepository;

    @Override
    protected FlowNodeStatus doModifyStatusOnStart(FlowTaskInstance target) {
        return null;
    }

    /**
     * since the {@link org.flowable.bpmn.model.ErrorEventDefinition} in
     * {@link org.flowable.bpmn.model.BoundaryEvent} of {@code Flowable} will not call the
     * {@link #doModifyStatusOnStart(FlowTaskInstance)} during execution, the state modification is made
     * in the {@link #doModifyStatusOnEnd(FlowTaskInstance)}.
     */
    @Override
    protected FlowNodeStatus doModifyStatusOnEnd(FlowTaskInstance target) {
        flowInstanceRepository.updateStatusById(target.getFlowInstanceId(), FlowStatus.CANCELLED);
        FlowNodeStatus status = FlowNodeStatus.CANCELLED;
        int affectRows = serviceTaskRepository.updateStatusById(target.getId(), status);
        log.info("Modify node instance status successfully, instanceId={}, instanceType={}, affectRows={}",
                target.getId(), target.getNodeType(), affectRows);
        return status;
    }

    @Override
    protected Optional<FlowTaskInstance> getTargetByActivityId(@NonNull String activityId, @NonNull Long flowInstanceId,
            @NonNull FlowableAdaptor flowableAdaptor) {
        return flowableAdaptor.getTaskInstanceByActivityId(activityId, flowInstanceId);
    }

}
