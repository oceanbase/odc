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
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.instance.FlowApprovalInstance;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * This listener {@link BaseTaskExecutingCompleteListener} is executed when the task executing to
 * update the status of the {@link FlowApprovalInstance}
 *
 * @author yh263208
 * @date 2022-02-22 20:09
 * @since ODC_release_3.3.0
 * @see EmptyExecutionListener
 */
@Slf4j
public class BaseTaskExecutingCompleteListener extends BaseStatusModifyListener<FlowApprovalInstance> {

    @Autowired
    private FlowInstanceRepository flowInstanceRepository;
    @Autowired
    private UserTaskInstanceRepository userTaskInstanceRepository;
    @Autowired
    private FlowInstanceService flowInstanceService;

    @Override
    protected FlowNodeStatus doModifyStatusOnStart(FlowApprovalInstance target) {
        Boolean isWaitForConfirm = userTaskInstanceRepository.findConfirmById(target.getId());
        if (!isWaitForConfirm) {
            flowInstanceRepository.updateStatusById(target.getFlowInstanceId(), FlowStatus.APPROVING);
            return internalModify(target, FlowNodeStatus.EXECUTING);
        } else {
            flowInstanceRepository.updateStatusById(target.getFlowInstanceId(), FlowStatus.WAIT_FOR_CONFIRM);
            return internalModify(target, FlowNodeStatus.WAIT_FOR_CONFIRM);
        }
    }

    @Override
    protected FlowNodeStatus doModifyStatusOnEnd(FlowApprovalInstance target) {
        return internalModify(target, FlowNodeStatus.COMPLETED);
    }

    @Override
    protected Optional<FlowApprovalInstance> getTargetByActivityId(@NonNull String activityId,
            @NonNull Long flowInstanceId, @NonNull FlowableAdaptor flowableAdaptor) {
        return flowableAdaptor.getApprovalInstanceByActivityId(activityId, flowInstanceId);
    }

    private FlowNodeStatus internalModify(FlowApprovalInstance target, FlowNodeStatus status) {
        int affectRows = userTaskInstanceRepository.updateStatusById(target.getId(), status);
        log.info("Modify node instance status successfully, instanceId={}, instanceType={}, affectRows={}",
                target.getId(), target.getNodeType(), affectRows);
        return status;
    }

}
