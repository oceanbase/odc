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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.oceanbase.odc.service.task.schedule.JobIdentity;

/**
 * @author yaobin
 * @date 2023-11-15
 * @since 4.2.4
 */
public interface JobContext {
    /**
     * get job identity
     */
    JobIdentity getJobIdentity();

    /**
     * get job class
     */
    String getJobClass();

    /**
     * get job properties. <br>
     * different from job parameters, job properties is for task-framework, job parameters is for task
     * executor.
     */
    Map<String, String> getJobProperties();


    /**
     * get odc server host properties <br>
     * deprecated, use getJobProperties instead
     */
    @Deprecated
    default List<String> getHostUrls() {
        Map<String, String> properties = getJobProperties();
        return properties == null ? Collections.emptyList()
                : Arrays.asList(properties.get("odcHostUrls").split(","));
    }

    /**
     * get job parameters
     */
    Map<String, String> getJobParameters();
}
