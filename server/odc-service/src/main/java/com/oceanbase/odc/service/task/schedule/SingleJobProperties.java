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
package com.oceanbase.odc.service.task.schedule;

import java.util.Map;
import java.util.Objects;

import com.oceanbase.odc.common.json.JsonUtils;

import lombok.Data;

/**
 * @author yaobin
 * @date 2024-01-05
 * @since 4.2.4
 * @todo remove this class, use Map and JobPropertiesUtils instead
 * @see {@link com.oceanbase.odc.service.task.util.JobPropertiesUtils}
 */
@Data
@Deprecated
public class SingleJobProperties {

    /**
     * job enable retry when task is expired
     */
    private boolean enableRetryAfterHeartTimeout = false;

    /**
     * job retry max times
     */
    private Integer maxRetryTimesAfterHeartTimeout = 2;

    /**
     * job expired after seconds when job status is preparing and no resources to schedule job
     */
    private Integer jobExpiredIfNotRunningAfterSeconds = null;

    public static SingleJobProperties fromJobProperties(Map<String, String> jobProperties) {
        if (Objects.isNull(jobProperties)) {
            return new SingleJobProperties();
        }
        String json = JsonUtils.toJson(jobProperties);
        return JsonUtils.fromJson(json, SingleJobProperties.class);
    }

    public Map<String, String> toJobProperties() {
        String json = JsonUtils.toJson(this);
        return JsonUtils.fromJsonMap(json, String.class, String.class);
    }
}
