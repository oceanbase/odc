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
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * This listener {@link ServiceTaskPendingExpiredListener} is executed when the task expires to
 * update the status of the {@link FlowTaskInstance}
 *
 * @author yh263208
 * @date 2022-02-22 20:09
 * @since ODC_release_3.3.0
 * @see EmptyExecutionListener
 */
@Slf4j
public class ServiceTaskExecutingCompleteListener extends BaseStatusModifyListener<FlowTaskInstance> {

    @Autowired
    private FlowInstanceRepository flowInstanceRepository;
    @Autowired
    private ServiceTaskInstanceRepository serviceTaskRepository;

    @Override
    protected FlowNodeStatus doModifyStatusOnStart(FlowTaskInstance target) {
        flowInstanceRepository.updateStatusById(target.getFlowInstanceId(), FlowStatus.EXECUTING);
        return internalModify(target, FlowNodeStatus.EXECUTING);
    }

    @Override
    protected FlowNodeStatus doModifyStatusOnEnd(FlowTaskInstance target) {
        return FlowNodeStatus.EXECUTING;
    }

    @Override
    protected Optional<FlowTaskInstance> getTargetByActivityId(@NonNull String activityId, @NonNull Long flowInstanceId,
            @NonNull FlowableAdaptor flowableAdaptor) {
        return flowableAdaptor.getTaskInstanceByActivityId(activityId, flowInstanceId);
    }

    private FlowNodeStatus internalModify(FlowTaskInstance target, FlowNodeStatus status) {
        int affectRows = serviceTaskRepository.updateStatusById(target.getId(), status);
        log.info("Modify node instance status successfully, instanceId={}, instanceType={}, affectRows={}",
                target.getId(), target.getNodeType(), affectRows);
        return status;
    }

}
