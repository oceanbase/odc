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
package com.oceanbase.odc.core.shared.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author wenniu.ly
 * @date 2022/2/9
 */
public enum FlowStatus {
    /**
     * {@code FlowInstance} is created
     */
    CREATED,
    /**
     * {@code FlowInstance} is waitting for approval
     */
    APPROVING,
    /**
     * {@code FlowInstance} is rejected
     */
    REJECTED,
    /**
     * {@code FlowInstance} is expired for waitting for approval
     */
    APPROVAL_EXPIRED,
    /**
     * {@code FlowInstance} is waitting for execution
     */
    WAIT_FOR_EXECUTION,
    /**
     * The task waiting for manual execution expired
     */
    WAIT_FOR_EXECUTION_EXPIRED,

    WAIT_FOR_CONFIRM,
    /**
     * {@code FlowInstance} is executing
     */
    EXECUTING,
    /**
     * {@code FlowInstance} is succeed
     */
    EXECUTION_SUCCEEDED,
    /**
     * {@code FlowInstance} is failed
     */
    EXECUTION_ABNORMAL,
    /**
     * {@code FlowInstance} is failed
     */
    EXECUTION_FAILED,
    /**
     * {@code FlowInstance} is expired, cause service task is expired
     */
    EXECUTION_EXPIRED,
    /**
     * {@code FlowInstance} is rollbacking
     */
    ROLLBACKING,
    /**
     * {@code FlowInstance} is failed when rollbacking
     */
    ROLLBACK_FAILED,
    /**
     * {@code FlowInstance} is succeed when rollbacking
     */
    ROLLBACK_SUCCEEDED,
    /**
     * {@code FlowInstance} is killed by user
     */
    CANCELLED,
    /**
     * {@code FlowInstance} is done
     */
    COMPLETED,

    PRE_CHECK_FAILED;

    public static List<FlowStatus> listUnfinishedStatus() {
        return Collections.unmodifiableList(
                Arrays.asList(CREATED, APPROVING, WAIT_FOR_EXECUTION, WAIT_FOR_CONFIRM, EXECUTING, ROLLBACKING));
    }
}
