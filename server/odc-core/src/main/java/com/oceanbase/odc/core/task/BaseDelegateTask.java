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
package com.oceanbase.odc.core.task;

import java.util.concurrent.Callable;

import com.oceanbase.odc.common.event.EventPublisher;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Proxy task object, all tasks need to be encapsulated with this task, and the life cycle of the
 * task is managed through this proxy object
 *
 * @author yh263208
 * @date 2021-11-11 12:00
 * @since ODC_release_3.2.2
 */
@Slf4j
public abstract class BaseDelegateTask {

    private final EventPublisher eventPublisher;

    public BaseDelegateTask(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    protected void doRun(@NonNull Runnable delegatedRunnable) {
        try {
            delegatedRunnable.run();
            if (this.eventPublisher != null) {
                this.eventPublisher.publishEvent(new TaskCompleteEvent(this));
            }
        } catch (Exception e) {
            log.warn("Task execution failed", e);
            if (this.eventPublisher != null) {
                this.eventPublisher.publishEvent(new TaskCompleteEvent(this, e));
            }
        }
    }

    protected <T> T doCall(@NonNull Callable<T> delegatedCallable) throws Exception {
        try {
            T returnValue = delegatedCallable.call();
            if (this.eventPublisher != null) {
                this.eventPublisher.publishEvent(new TaskCompleteEvent(this));
            }
            return returnValue;
        } catch (Exception e) {
            log.warn("Task execution failed", e);
            if (this.eventPublisher != null) {
                this.eventPublisher.publishEvent(new TaskCompleteEvent(this, e));
            }
            throw e;
        }
    }

}

