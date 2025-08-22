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
package com.oceanbase.odc.service.collaboration.landingpage.model;

import static com.oceanbase.odc.core.shared.constant.FlowStatus.*;

import java.util.Arrays;
import java.util.Optional;

import com.google.common.collect.ImmutableSet;
import com.oceanbase.odc.core.shared.constant.FlowStatus;

import lombok.Getter;
import lombok.NonNull;

/**
 * @Author: ysj
 * @Date: 2025/8/12 16:14
 * @Since: 4.4.1
 * @Description:
 */
@Getter
public enum FlowStatusCategory {

    PENDING(CREATED, APPROVING, WAIT_FOR_EXECUTION, WAIT_FOR_CONFIRM),

    EXECUTING(COMPLETED, EXECUTION_SUCCEEDED),

    EXECUTION_FAILURE(REJECTED, APPROVAL_EXPIRED, WAIT_FOR_EXECUTION_EXPIRED, EXECUTION_ABNORMAL, EXECUTION_FAILED,
            EXECUTION_EXPIRED, PRE_CHECK_FAILED),

    EXECUTION_SUCCESS(COMPLETED, EXECUTION_SUCCEEDED),

    OTHER();

    private final ImmutableSet<FlowStatus> includesFlowStatus;

    FlowStatusCategory(FlowStatus... includesFlowStatus) {
        this.includesFlowStatus = ImmutableSet.copyOf(includesFlowStatus);
    }

    public static FlowStatusCategory getCategory(@NonNull FlowStatus flowStatus) {
        Optional<FlowStatusCategory> categoryOptional = Arrays.stream(FlowStatusCategory.values())
                .filter(flowStatusCategory -> flowStatusCategory.includesFlowStatus.contains(flowStatus))
                .findFirst();
        return categoryOptional.orElse(OTHER);
    }
}
