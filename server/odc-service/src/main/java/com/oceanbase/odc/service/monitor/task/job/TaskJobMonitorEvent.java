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

import com.oceanbase.odc.service.monitor.MeterName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@AllArgsConstructor
public class TaskJobMonitorEvent {

    Action action;

    @AllArgsConstructor
    @Getter
    public enum Action {
        START_SUCCESS(MeterName.JOB_START_SUCCESS_COUNT),
        START_FAILED(MeterName.JOB_START_FAILED_COUNT),
        STOP_SUCCESS(MeterName.JOB_STOP_SUCCESS_COUNT),
        STOP_FAILED(MeterName.JOB_STOP_FAILED_COUNT),
        DESTROY_SUCCESS(MeterName.JOB_DESTROY_SUCCESS_COUNT),
        DESTROY_FAILED(MeterName.JOB_DESTROY_FAILED_COUNT);

        private MeterName meterName;

    }
}
