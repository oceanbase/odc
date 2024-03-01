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

import com.oceanbase.odc.core.flow.model.FlowableElementType;
import com.oceanbase.odc.core.flow.util.EmptyExecutionListener;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceEntity;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceRepository;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.instance.FlowApprovalInstance;
import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.model.FlowNodeType;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * This listener {@link BindingCallbackTaskExecutingCompleteListener} is executed when the task
 * executing to update the status of the {@link FlowApprovalInstance}
 *
 * @author yh263208
 * @date 2022-02-22 20:09
 * @since ODC_release_3.3.0
 * @see EmptyExecutionListener
 */
@Slf4j
public class BindingCallbackTaskExecutingCompleteListener extends BaseStatusModifyListener<FlowTaskInstance> {

    @Autowired
    private FlowInstanceRepository flowInstanceRepository;
    @Autowired
    private UserTaskInstanceRepository userTaskInstanceRepository;
    @Autowired
    private FlowInstanceService flowInstanceService;
    @Autowired
    private FlowableAdaptor flowableAdaptor;

    @Override
    protected FlowNodeStatus doModifyStatusOnStart(FlowTaskInstance target) {
        return internalModify(target, FlowNodeStatus.WAIT_FOR_CONFIRM);
    }

    @Override
    protected FlowNodeStatus doModifyStatusOnEnd(FlowTaskInstance target) {

        return internalModify(target, FlowNodeStatus.COMPLETED);
    }

    @Override
    protected Optional<FlowTaskInstance> getTargetByActivityId(@NonNull String activityId,
            @NonNull Long flowInstanceId, @NonNull FlowableAdaptor flowableAdaptor) {
        return flowableAdaptor.getTaskInstanceByActivityId(activityId, flowInstanceId);
    }

    private FlowNodeStatus internalModify(FlowTaskInstance target, FlowNodeStatus status) {
        Optional<UserTaskInstanceEntity> userTaskInstanceEntity =
                userTaskInstanceRepository.findByInstanceTypeAndInstanceId(FlowNodeType.SERVICE_TASK, target.getId(),
                        target.getFlowInstanceId(), FlowableElementType.USER_TASK);

        if (!userTaskInstanceEntity.isPresent()) {
            log.warn("Flow node instance does not exist, instanceId={}, flowInstanceId={}", target.getId(),
                    target.getFlowInstanceId());
            throw new IllegalStateException("Can not find instance by instanceId=" + target.getId());
        }

        int affectRows = userTaskInstanceRepository.updateApprovedById(userTaskInstanceEntity.get().getId(), false);
        log.info("Modify node instance approved to false, instanceId={}, instanceType={}, affectRows={}",
                target.getId(), target.getNodeType(), affectRows);

        userTaskInstanceRepository.updateStatusById(userTaskInstanceEntity.get().getId(), status);
        log.info("Modify node instance status successfully, instanceId={}, instanceType={}, affectRows={}",
                target.getId(), target.getNodeType(), affectRows);
        return status;
    }

}
