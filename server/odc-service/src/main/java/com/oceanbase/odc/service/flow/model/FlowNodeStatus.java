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
import java.util.HashSet;
import java.util.Set;

import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;

import lombok.NonNull;

/**
 * Process state, used to mark the execution state of process instance nodes, including process
 * instances, approval nodes, task execution nodes, etc.
 *
 * @author yh263208
 * @date 2022-02-07 11:15
 * @since ODC_release_3.3.0
 */
public enum FlowNodeStatus {
    /**
     * Process node has been created but has not started execution
     */
    CREATED,
    /**
     * Process node is executing
     */
    EXECUTING,
    /**
     * Only for {@link FlowTaskInstance}, instance is waitting for approval
     */
    PENDING,
    WAIT_FOR_CONFIRM,
    /**
     * Process node is terminated
     */
    CANCELLED,
    /**
     * Process node execution completes
     */
    COMPLETED,
    /**
     * Process node execution expired
     */
    EXPIRED,
    /**
     * Process node execution failed
     */
    FAILED;

    public static Set<FlowNodeStatus> getFinalStatuses() {
        return new HashSet<>(Arrays.asList(FlowNodeStatus.CANCELLED, FlowNodeStatus.COMPLETED, FlowNodeStatus.EXPIRED,
                FlowNodeStatus.FAILED));
    }

    public static boolean isFinalStatus(@NonNull FlowNodeStatus status) {
        return getFinalStatuses().contains(status);
    }

    public boolean isFinalStatus() {
        return getFinalStatuses().contains(this);
    }

}
