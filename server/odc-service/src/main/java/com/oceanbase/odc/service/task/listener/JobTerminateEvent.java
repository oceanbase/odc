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
package com.oceanbase.odc.service.task.listener;

import javax.annotation.Nullable;

import com.oceanbase.odc.common.event.AbstractEvent;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

import lombok.Getter;

/**
 * @author yaobin
 * @date 2024-01-12
 * @since 4.2.4
 */
public class JobTerminateEvent extends AbstractEvent {

    @Getter
    private final JobIdentity ji;

    @Getter
    private final JobStatus status;

    @Nullable
    @Getter
    private String errorMessage;

    /**
     * Constructs a prototypical Event.
     *
     * @param ji job identity
     * @param status job status
     */
    public JobTerminateEvent(JobIdentity ji, JobStatus status) {
        super(ji, "DestroyExecutorEvent");
        this.ji = ji;
        this.status = status;
    }

    public JobTerminateEvent(JobIdentity ji, JobStatus status, String errorMessage) {
        this(ji, status);
        this.errorMessage = errorMessage;
    }
}
