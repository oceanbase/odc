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
package com.oceanbase.odc.service.monitor.task;

import lombok.Getter;

@Getter
public enum TaskMeters {
    TASK_EXECUTE_COUNT("odc_server_task_execute_total_count", "total task execute count"),
    TASK_RETRY_COUNT("odc_server_task_execute_retry_count", "total task retry count"),
    TASK_EXECUTE_SUCCESS_COUNT("odc_server_task_execute_success_count", "total task execute success count"),
    TASK_EXECUTE_FAILED_COUNT("odc_server_task_execute_failed_count", "total task execute failed count"),
    RUNNING_TASK_GAUGE("odc_server_task_running_tasks", "Running task count"),
    WAITING_TASK_GAUGE("odc_server_task_waiting_tasks", "Waiting task count"),
    RUNNING_POD_GAUGE("odc_server_task_running_pods", "Running pod count"),
    TASK_RUNNING_TIME("odc_server_task_running_time", "Task running time"),;

    private final String meterName;
    private final String description;

    TaskMeters(String meterName, String description) {
        this.meterName = meterName;
        this.description = description;
    }
}
