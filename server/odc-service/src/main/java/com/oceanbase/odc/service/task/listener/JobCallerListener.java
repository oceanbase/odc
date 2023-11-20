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

/**
 * @author yaobin
 * @date 2023-11-16
 * @since 4.2.4
 */
public class JobCallerListener extends AbstractEventListener<JobCallerEvent> {

    @Override
    public void onEvent(JobCallerEvent event) {
        if (event.getJobAction() == JobCallerAction.START) {
            if (event.isStatus()) {
                startSucceed(event.getTaskId());
            } else {
                startFailed(event.getTaskId(), event.getEx());
            }
        } else if (event.getJobAction() == JobCallerAction.STOP) {
            if (event.isStatus()) {
                stopSucceed(event.getTaskId());
            }else {
                stopFailed(event.getTaskId(), event.getEx());
            }
        }
    }

    protected void startSucceed(Long taskId) {

    }

    protected void startFailed(Long taskId, Exception ex) {

    }

    protected void stopSucceed(Long taskId) {

    }

    protected void stopFailed(Long taskId, Exception ex) {

    }
}
