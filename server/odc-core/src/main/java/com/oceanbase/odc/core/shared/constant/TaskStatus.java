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
import java.util.List;

/**
 * @author wenniu.ly
 * @date 2021/3/15
 */
public enum TaskStatus {
    PREPARING,
    RUNNING,
    // task not work, but can be recovered
    ABNORMAL,
    // the following is terminate states
    FAILED,
    CANCELED,
    DONE;

    public static List<TaskStatus> getProcessingStatus() {
        return Arrays.asList(PREPARING, RUNNING);
    }

    public boolean isProcessing() {
        return getProcessingStatus().contains(this);
    }

    public boolean isTerminated() {
        return TaskStatus.CANCELED == this || TaskStatus.FAILED == this || TaskStatus.DONE == this;
    }

    public boolean isRetryAllowed() {
        return CANCELED == this || FAILED == this;
    }

    public static List<String> getRetryAllowedStatus() {
        return Arrays.asList(TaskStatus.FAILED.name(), TaskStatus.CANCELED.name(), TaskStatus.DONE.name());
    }
}
