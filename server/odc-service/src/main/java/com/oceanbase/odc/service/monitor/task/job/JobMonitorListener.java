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
package com.oceanbase.odc.service.monitor.task.job;

import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.monitor.MonitorEvent;
import com.oceanbase.odc.service.monitor.task.job.TaskJobMonitorEvent.Action;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifier;
import com.oceanbase.odc.service.task.listener.JobCallerListener;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

public class JobMonitorListener extends JobCallerListener {

    public void startSucceed(JobIdentity ji, ExecutorIdentifier executorIdentifier) {
        SpringContextUtil.publishEvent(MonitorEvent.createJobMonitorEvent(Action.START_SUCCESS));
    }

    public void startFailed(JobIdentity ji, Exception ex) {
        SpringContextUtil.publishEvent(MonitorEvent.createJobMonitorEvent(Action.START_FAILED));
    }

    public void stopSucceed(JobIdentity ji) {
        SpringContextUtil.publishEvent(MonitorEvent.createJobMonitorEvent(Action.STOP_SUCCESS));
    }

    public void stopFailed(JobIdentity ji, Exception ex) {
        SpringContextUtil.publishEvent(MonitorEvent.createJobMonitorEvent(Action.STOP_FAILED));
    }

    public void destroySucceed(JobIdentity ji) {
        SpringContextUtil.publishEvent(MonitorEvent.createJobMonitorEvent(Action.DESTROY_SUCCESS));
    }

    public void destroyFailed(JobIdentity ji, Exception ex) {
        SpringContextUtil.publishEvent(MonitorEvent.createJobMonitorEvent(Action.DESTROY_FAILED));
    }

}
