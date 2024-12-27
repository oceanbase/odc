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
package com.oceanbase.odc.service.task.enums;

import com.oceanbase.odc.core.shared.constant.TaskStatus;

/**
 * @author yaobin
 * @date 2023-12-25
 * @since 4.2.4
 */
public enum JobStatus {
    PREPARING,
    TIMEOUT,
    RUNNING,
    FAILED,
    CANCELING,
    DO_CANCELING,
    CANCELED,
    DONE;

    public boolean isTerminated() {
        return JobStatus.CANCELED == this || JobStatus.FAILED == this || JobStatus.DONE == this;
    }

    public boolean isExecuting() {
        return !isTerminated();
    }

    public TaskStatus convertTaskStatus() {

        if (CANCELING == this) {
            return TaskStatus.CANCELED;
        }
        return TaskStatus.valueOf(this.name());
    }

    @Override
    public String toString() {
        return this.name();
    }
}
