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
package com.oceanbase.odc.service.task.executor;

import java.util.Map;

import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

import lombok.Data;

/**
 * @author yaobin
 * @date 2023-11-29
 * @since 4.2.4
 */
@Data
public class DefaultTaskResult implements TaskResult {

    private JobIdentity jobIdentity;

    private JobStatus status;

    private String resultJson;

    private String executorEndpoint;

    private double progress;

    private Map<String, String> logMetadata;

    public boolean progressChanged(DefaultTaskResult previous) {
        if (previous == null) {
            return true;
        }
        if (status != previous.getStatus()) {
            return true;
        }
        if (Double.compare(progress, previous.getProgress()) != 0) {
            return true;
        }
        if (logMetadata != null && !logMetadata.equals(previous.getLogMetadata())) {
            return true;
        }
        if (resultJson != null && !resultJson.equals(previous.getResultJson())) {
            return true;
        }
        return false;
    }

}
