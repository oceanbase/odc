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

import com.oceanbase.odc.core.flow.util.EmptyExecutionListener;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceRepository;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.instance.FlowApprovalInstance;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * This listener {@link ApprovalTaskExpiredListener} is executed when the approval task expires to
 * update the status of the {@link FlowApprovalInstance}
 *
 * @author yh263208
 * @date 2022-02-22 20:09
 * @since ODC_release_3.3.0
 * @see EmptyExecutionListener
 */
@Slf4j
public class ApprovalTaskExpiredListener extends BaseStatusModifyListener<FlowApprovalInstance> {

    @Autowired
    private UserTaskInstanceRepository userTaskRepository;
    @Autowired
    private FlowInstanceRepository flowInstanceRepository;
    @Autowired
    private ScheduleService scheduleService;

    @Override
    protected FlowNodeStatus doModifyStatusOnStart(FlowApprovalInstance target) {
        return null;
    }

    @Override
    protected FlowNodeStatus doModifyStatusOnEnd(FlowApprovalInstance target) {
        flowInstanceRepository.updateStatusById(target.getFlowInstanceId(), FlowStatus.APPROVAL_EXPIRED);
        FlowNodeStatus status = FlowNodeStatus.EXPIRED;
        int affectRows = userTaskRepository.updateStatusById(target.getId(), status);
        scheduleService.updateStatusByFlowInstanceId(target.getFlowInstanceId(), ScheduleStatus.APPROVAL_EXPIRED);
        log.info("Modify node instance status successfully, instanceId={}, instanceType={}, affectRows={}",
                target.getId(), target.getNodeType(), affectRows);
        return status;
    }

    @Override
    protected Optional<FlowApprovalInstance> getTargetByActivityId(@NonNull String activityId,
            @NonNull Long flowInstanceId, @NonNull FlowableAdaptor flowableAdaptor) {
        return flowableAdaptor.getApprovalInstanceByActivityId(activityId, flowInstanceId);
    }

}
