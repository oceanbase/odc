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
import java.util.Optional;

import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.service.delegate.DelegateTask;

import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.core.flow.builder.TimerBoundaryEventBuilder;
import com.oceanbase.odc.core.flow.model.FlowableElement;
import com.oceanbase.odc.core.flow.model.FlowableElementType;
import com.oceanbase.odc.service.flow.instance.BaseFlowNodeInstance;
import com.oceanbase.odc.service.flow.instance.FlowApprovalInstance;
import com.oceanbase.odc.service.flow.instance.FlowGatewayInstance;
import com.oceanbase.odc.service.flow.instance.FlowInstance;
import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;
import com.oceanbase.odc.service.flow.model.FlowNodeType;

import lombok.NonNull;

/**
 * Service object for {@code Flowable}
 *
 * @author yh263208
 * @date 2022-02-17 19:28
 * @since ODC_release_3.3.0
 */
public interface FlowableAdaptor {
    /**
     * Get {@link FlowInstance#getId()} by {@link ProcessInstance#getId()}
     *
     * @param processInstanceId {@link ProcessInstance#getId()}
     * @return {@link FlowInstance#getId()}
     */
    Optional<Long> getFlowInstanceIdByProcessInstanceId(@NonNull String processInstanceId);

    /**
     * Get {@link FlowInstance#getId()} by
     * {@link org.flowable.engine.repository.ProcessDefinition#getId()}
     *
     * @param processDefinitionId {@link org.flowable.engine.repository.ProcessDefinition#getId()}
     * @return {@link FlowInstance#getId()}
     */
    Optional<Long> getFlowInstanceIdByProcessDefinitionId(@NonNull String processDefinitionId);

    /**
     * Get {@link ProcessInstance#getId()} by {@link FlowInstance#getId()}
     *
     * @param flowInstanceId {@link FlowInstance#getId()}
     * @return {@link ProcessInstance#getId()}
     */
    Optional<String> getProcessInstanceIdByFlowInstanceId(@NonNull Long flowInstanceId);

    /**
     * Get {@link Execution#getActivityId()} for a node instance
     *
     * @param instanceId id for this instance
     * @param instanceType node type
     * @param type
     * @return activity id value
     */
    List<FlowableElement> getFlowableElementByType(@NonNull Long instanceId, @NonNull FlowNodeType instanceType,
            @NonNull FlowableElementType type);

    /**
     * Get {@link FlowTaskInstance} by {@link Execution#getActivityId()}
     *
     * @param activityId refers to {@link Execution#getActivityId()}
     * @param flowInstanceId refers to {@link FlowInstance#getId()}
     * @return {@link FlowTaskInstance}
     */
    Optional<FlowTaskInstance> getTaskInstanceByActivityId(@NonNull String activityId, @NonNull Long flowInstanceId);

    /**
     * Get {@link FlowApprovalInstance} by {@link Execution#getActivityId()}
     *
     * @param activityId refers to {@link Execution#getActivityId()}
     * @param flowInstanceId refers to {@link FlowInstance#getId()}
     * @return {@link FlowApprovalInstance}
     */
    Optional<FlowApprovalInstance> getApprovalInstanceByActivityId(@NonNull String activityId,
            @NonNull Long flowInstanceId);

    /**
     * Get {@link FlowApprovalInstance} by {@link Execution#getActivityId()}
     *
     * @param name refers to {@link DelegateTask#getName()}
     * @param flowInstanceId refers to {@link FlowInstance#getId()}
     * @return {@link FlowApprovalInstance}
     */
    Optional<FlowApprovalInstance> getApprovalInstanceByName(@NonNull String name, @NonNull Long flowInstanceId);

    /**
     * Get {@link FlowGatewayInstance} by {@link Execution#getActivityId()}
     *
     * @param activityId refers to {@link Execution#getActivityId()}
     * @param flowInstanceId refers to {@link FlowInstance#getId()}
     * @return {@link FlowGatewayInstance}
     */
    Optional<FlowGatewayInstance> getGatewayInstanceByActivityId(@NonNull String activityId,
            @NonNull Long flowInstanceId);

    /**
     * Bind events to node instances, for example, the {@link TimerBoundaryEventBuilder} is bound to the
     * {@link FlowApprovalInstance}
     *
     * @param elements
     */
    void setFlowableElements(@NonNull List<Pair<BaseFlowNodeInstance, FlowableElement>> elements);

    /**
     * Bind the {@link FlowInstance#getId()} to the {@link ProcessInstance#getId()}
     *
     * @param flowInstanceId {@link FlowInstance#getId()}
     * @param processInstanceId {@link ProcessInstance#getId()}
     */
    void setProcessInstanceId(@NonNull Long flowInstanceId, @NonNull String processInstanceId);

    /**
     * Bind the {@link FlowInstance#getId()} to the
     * {@link org.flowable.engine.repository.ProcessDefinition#getId()}
     *
     * @param flowInstanceId {@link FlowInstance#getId()}
     * @param processDefinitionId {@link org.flowable.engine.repository.ProcessDefinition#getId()}
     */
    void setProcessDefinitionId(@NonNull Long flowInstanceId, @NonNull String processDefinitionId);

}
