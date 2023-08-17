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

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.common.util.RetryExecutor;
import com.oceanbase.odc.core.flow.util.EmptyExecutionListener;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.instance.BaseFlowNodeInstance;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * A listener for modifying the state of an instance. Listeners are used in flowable integration to
 * manage the state of each instance.
 *
 * @author yh263208
 * @date 2022-02-23 10:39
 * @since ODC_release_3.3.0
 * @see EmptyExecutionListener
 */
@Slf4j
public abstract class BaseStatusModifyListener<T extends BaseFlowNodeInstance> extends EmptyExecutionListener {

    @Autowired
    private FlowableAdaptor flowableAdaptor;
    private final RetryExecutor retryExecutor = RetryExecutor.builder().retryIntervalMillis(1000).retryTimes(3).build();

    @Override
    protected void onExecutiuonStart(DelegateExecution execution) {
        super.onExecutiuonStart(execution);
        internalModifyStatus(execution, this::doModifyStatusOnStart);
    }

    @Override
    protected void onExecutionEnd(DelegateExecution execution) {
        super.onExecutionEnd(execution);
        internalModifyStatus(execution, this::doModifyStatusOnEnd);
    }

    private void internalModifyStatus(DelegateExecution execution, Function<T, FlowNodeStatus> function) {
        String activityId = execution.getCurrentActivityId();
        String processDefinitionId = execution.getProcessDefinitionId();
        Optional<Optional<Long>> retryOptional = retryExecutor.run(
                () -> flowableAdaptor.getFlowInstanceIdByProcessDefinitionId(processDefinitionId), Optional::isPresent);
        if (!retryOptional.isPresent()) {
            log.warn("Flow instance id does not exist, activityId={}, processDefinitionId={}", activityId,
                    processDefinitionId);
            throw new IllegalStateException(
                    "Can not find flow instance id by process definition id " + processDefinitionId);
        }
        Long flowInstanceId = retryOptional.get().get();
        Optional<T> targetOptional = getTargetByActivityId(activityId, flowInstanceId, flowableAdaptor);
        if (!targetOptional.isPresent()) {
            log.warn("Flow node instance does not exist, activityId={}, flowInstanceId={}", activityId, flowInstanceId);
            throw new IllegalStateException("Can not find instance by activityId " + activityId);
        }
        T target = targetOptional.get();
        try {
            Optional<FlowNodeStatus> optional = this.retryExecutor.run(() -> {
                try {
                    return function.apply(target);
                } catch (Exception e) {
                    return null;
                }
            }, Objects::nonNull);
            if (optional.isPresent()) {
                log.info("Update the instance status, activityId={}, instanceType={}, instanceId={}, status={}",
                        activityId, target.getNodeType(), target.getId(), optional.get());
            } else {
                throw new IllegalStateException("Failed to update node's status");
            }
        } catch (Exception e) {
            log.warn("Failed to update status, activityId={}, instanceType={}, instanceId={}", activityId,
                    target.getNodeType(), target.getId(), e);
            throw new RuntimeException(e);
        } finally {
            target.dealloc();
        }
    }

    abstract protected FlowNodeStatus doModifyStatusOnStart(T target);

    abstract protected FlowNodeStatus doModifyStatusOnEnd(T target);

    abstract protected Optional<T> getTargetByActivityId(@NonNull String activityId, @NonNull Long flowInstanceId,
            @NonNull FlowableAdaptor flowableAdaptor);

}
