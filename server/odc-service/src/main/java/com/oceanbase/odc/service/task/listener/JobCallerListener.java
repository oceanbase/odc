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

import com.oceanbase.odc.common.event.AbstractEventListener;
import com.oceanbase.odc.service.task.enums.JobCallerAction;
import com.oceanbase.odc.service.task.schedule.ExecutorIdentifier;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

/**
 * @author yaobin
 * @date 2023-11-16
 * @since 4.2.4
 */
public class JobCallerListener extends AbstractEventListener<JobCallerEvent> {

    @Override
    public void onEvent(JobCallerEvent event) {
        if (event.getJobAction() == JobCallerAction.START) {
            if (event.isSuccess()) {
                startSucceed(event.getJobIdentity(), event.getExecutorIdentifier());
            } else {
                startFailed(event.getJobIdentity(), event.getEx());
            }
        } else if (event.getJobAction() == JobCallerAction.STOP) {
            if (event.isSuccess()) {
                stopSucceed(event.getJobIdentity());
            } else {
                stopFailed(event.getJobIdentity(), event.getEx());
            }
        }
    }

    protected void startSucceed(JobIdentity ji, ExecutorIdentifier executorIdentifier) {

    }

    protected void startFailed(JobIdentity ji, Exception ex) {


    }

    protected void stopSucceed(JobIdentity ji) {

    }

    protected void stopFailed(JobIdentity ji, Exception ex) {

    }
}
