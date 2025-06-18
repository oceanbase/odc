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
    // pausing or paused or resuming only support for task who implement restart logic
    // that means task must save checkpoint for it's recovery
    PAUSING,
    PAUSED,
    RESUMING,
    // task is canceling, that will transfer to CANCELED status
    CANCELING,
    // the following is terminate states
    FAILED,
    EXEC_TIMEOUT,
    CANCELED,
    DONE;

    public static List<TaskStatus> getProcessingStatus() {
        return Arrays.asList(PREPARING, RUNNING);
    }

    public boolean isProcessing() {
        return getProcessingStatus().contains(this);
    }

    public boolean isTerminated() {
        return TaskStatus.CANCELED == this || TaskStatus.FAILED == this || TaskStatus.DONE == this
                || TaskStatus.EXEC_TIMEOUT == this;
    }

    public boolean isRecoverable() {
        switch (this) {
            case PAUSED:
            case ABNORMAL:
                return true;
            default:
                return false;
        }
    }

    public boolean isRetryAllowed() {
        // TODO(lx): confirm CANCELED , FAILED , EXEC_TIMEOUT should be a terminate status?
        return CANCELED == this || FAILED == this || EXEC_TIMEOUT == this
                || isRecoverable();
    }

    // only paused and abnormal status can be recovered, this code will be removed in the future
    @Deprecated
    public static List<String> getRetryAllowedStatus() {
        return Arrays.asList(TaskStatus.FAILED.name(), TaskStatus.CANCELED.name(), TaskStatus.DONE.name(),
                TaskStatus.PAUSED.name(), TaskStatus.ABNORMAL.name());
    }
}
