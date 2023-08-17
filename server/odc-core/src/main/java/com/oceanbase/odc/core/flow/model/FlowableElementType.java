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
package com.oceanbase.odc.core.flow.model;

import org.flowable.bpmn.model.IntermediateCatchEvent;

/**
 * Enum type of event
 *
 * @author yh263208
 * @date 2022-02-21 17:17
 * @since ODC_release_3.3.0
 */
public enum FlowableElementType {
    /**
     * Refers to {@link org.flowable.bpmn.model.UserTask}
     */
    USER_TASK,
    /**
     * Refers to {@link org.flowable.bpmn.model.ServiceTask}
     */
    SERVICE_TASK,
    /**
     * Refers to {@link org.flowable.bpmn.model.ExclusiveGateway}
     */
    EXCLUSIVE_GATEWAY,
    /**
     * Refers to {@link IntermediateCatchEvent} and
     * {@link org.flowable.bpmn.model.SignalEventDefinition}
     */
    SIGNAL_CATCH_EVENT,
    /**
     * Refers to {@link IntermediateCatchEvent} and {@link org.flowable.bpmn.model.TimerEventDefinition}
     */
    TIMER_CATCH_EVENT,
    /**
     * Refers to {@link org.flowable.bpmn.model.BoundaryEvent} and
     * {@link org.flowable.bpmn.model.SignalEventDefinition}
     */
    TIMER_BOUNDARY_EVENT,
    /**
     * Refers to {@link org.flowable.bpmn.model.BoundaryEvent} and
     * {@link org.flowable.bpmn.model.ErrorEventDefinition}
     */
    ERROR_BOUNDARY_EVENT

}
