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

package com.oceanbase.odc.service.task.caller;

import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.common.event.LocalEventPublisher;
import com.oceanbase.odc.service.task.listener.JobCallerAction;
import com.oceanbase.odc.service.task.listener.JobCallerEvent;

/**
 * @author yaobin
 * @date 2023-11-16
 * @since 4.2.4
 */
public abstract class BaseJobCaller implements JobCaller {

    private final EventPublisher publisher = new LocalEventPublisher();

    @Override
    public void start(JobContext context) throws JobException {

        try {
            doStart(context);
            publisher.publishEvent(new JobCallerEvent(context.getTaskId(), JobCallerAction.START, true, null));
        } catch (JobException ex) {
            publisher.publishEvent(new JobCallerEvent(context.getTaskId(), JobCallerAction.START, false, ex));
            throw ex;
        }

    }

    @Override
    public void stop(Long taskId) throws JobException {
        try {
            doStop(taskId);
            publisher.publishEvent(new JobCallerEvent(taskId, JobCallerAction.STOP, true, null));
        } catch (JobException ex) {
            publisher.publishEvent(new JobCallerEvent(taskId, JobCallerAction.STOP, false, ex));
            throw ex;
        }
    }

    @Override
    public EventPublisher getEventPublisher() {
        return this.publisher;
    }

    protected abstract void doStart(JobContext context) throws JobException;

    protected abstract void doStop(Long taskId) throws JobException;

}
