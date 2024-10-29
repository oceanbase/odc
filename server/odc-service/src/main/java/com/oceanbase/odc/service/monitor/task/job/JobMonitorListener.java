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

import static com.oceanbase.odc.service.monitor.DefaultMeterName.JOB_DESTROY_FAILED_COUNT;
import static com.oceanbase.odc.service.monitor.DefaultMeterName.JOB_DESTROY_SUCCESS_COUNT;
import static com.oceanbase.odc.service.monitor.DefaultMeterName.JOB_START_FAILED_COUNT;
import static com.oceanbase.odc.service.monitor.DefaultMeterName.JOB_START_SUCCESS_COUNT;
import static com.oceanbase.odc.service.monitor.DefaultMeterName.JOB_STOP_FAILED_COUNT;
import static com.oceanbase.odc.service.monitor.DefaultMeterName.JOB_STOP_SUCCESS_COUNT;

import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.monitor.MeterKey;
import com.oceanbase.odc.service.monitor.MeterManager;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifier;
import com.oceanbase.odc.service.task.listener.JobCallerListener;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

public class JobMonitorListener extends JobCallerListener {

    private final MeterManager meterManager;

    public JobMonitorListener() {
        this.meterManager = SpringContextUtil.getBean(MeterManager.class);
    }

    public void startSucceed(JobIdentity ji, ExecutorIdentifier executorIdentifier) {
        meterManager.incrementCounter(MeterKey.ofMeter(JOB_START_SUCCESS_COUNT));
    }

    public void startFailed(JobIdentity ji, Exception ex) {
        meterManager.incrementCounter(MeterKey.ofMeter(JOB_START_FAILED_COUNT));

    }

    public void stopSucceed(JobIdentity ji) {
        meterManager.incrementCounter(MeterKey.ofMeter(JOB_STOP_SUCCESS_COUNT));

    }

    public void stopFailed(JobIdentity ji, Exception ex) {
        meterManager.incrementCounter(MeterKey.ofMeter(JOB_STOP_FAILED_COUNT));

    }

    public void destroySucceed(JobIdentity ji) {
        meterManager.incrementCounter(MeterKey.ofMeter(JOB_DESTROY_SUCCESS_COUNT));

    }

    public void destroyFailed(JobIdentity ji, Exception ex) {
        meterManager.incrementCounter(MeterKey.ofMeter(JOB_DESTROY_FAILED_COUNT));

    }

}
