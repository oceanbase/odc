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
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/8/8 20:27
 * @Description: []
 */
@Slf4j
public class PreCheckServiceTaskFailedListener extends BaseStatusModifyListener<FlowTaskInstance> {

    @Autowired
    private FlowInstanceRepository flowInstanceRepository;
    @Autowired
    private ServiceTaskInstanceRepository serviceTaskRepository;
    @Autowired
    private ScheduleService scheduleService;

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
        flowInstanceRepository.updateStatusById(target.getFlowInstanceId(), FlowStatus.PRE_CHECK_FAILED);
        FlowNodeStatus status = FlowNodeStatus.FAILED;
        int affectRows = serviceTaskRepository.updateStatusById(target.getId(), status);
        scheduleService.updateStatusByFlowInstanceId(target.getFlowInstanceId(), ScheduleStatus.TERMINATION);
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
