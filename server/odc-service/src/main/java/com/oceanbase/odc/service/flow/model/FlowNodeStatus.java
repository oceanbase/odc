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
package com.oceanbase.odc.service.flow.model;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;

import lombok.Getter;
import lombok.NonNull;

/**
 * Process state, used to mark the execution state of process instance nodes, including process
 * instances, approval nodes, task execution nodes, etc.
 *
 * @author yh263208
 * @date 2022-02-07 11:15
 * @since ODC_release_3.3.0
 */
@Getter
public enum FlowNodeStatus {
    /**
     * Process node has been created but has not started execution
     */
    CREATED(false, false),
    /**
     * Process node is executing
     */
    EXECUTING(false, true),
    /**
     * Only for {@link FlowTaskInstance}, instance is waitting for approval
     */
    PENDING(false, false),

    WAIT_FOR_CONFIRM(false, true),
    /**
     * Process node is terminated
     */
    CANCELLED(true, false),
    /**
     * Process node execution completes
     */
    COMPLETED(true, false),
    /**
     * Process node execution expired
     */
    EXPIRED(true, false),
    /**
     * Process node execution failed
     */
    FAILED(true, false);

    private final boolean finalStatus;
    private final boolean executing;

    FlowNodeStatus(boolean finalStatus, boolean executing) {
        this.finalStatus = finalStatus;
        this.executing = executing;
    }

    public static Set<FlowNodeStatus> getExecutingStatuses() {
        return Arrays.stream(FlowNodeStatus.values()).filter(FlowNodeStatus::isExecuting).collect(Collectors.toSet());
    }

    public static Set<FlowNodeStatus> getExecutingAndFinalStatuses() {
        return Arrays.stream(FlowNodeStatus.values()).filter(t -> t.isExecuting() || t.isFinalStatus()).collect(
                Collectors.toSet());
    }

    public static Set<FlowNodeStatus> getFinalStatuses() {
        return Arrays.stream(FlowNodeStatus.values()).filter(t -> t.isFinalStatus()).collect(Collectors.toSet());
    }

    public static Set<FlowNodeStatus> getNotFinalStatuses() {
        return Arrays.stream(FlowNodeStatus.values()).filter(t -> !t.isFinalStatus()).collect(Collectors.toSet());
    }

}
