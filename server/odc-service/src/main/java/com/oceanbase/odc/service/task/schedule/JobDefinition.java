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

import com.oceanbase.odc.service.task.Task;

/**
 * Define a job and describe all job information for schedule
 *
 * @author yaobin
 * @date 2023-11-23
 * @since 4.2.4
 */
public interface JobDefinition {

    /**
     * job class
     */
    Class<? extends Task<?>> getJobClass();

    /**
     * job type
     */
    String getJobType();

    /**
     * job parameters for task executor
     */
    Map<String, String> getJobParameters();

    /**
     * job properties for job scheduler
     */
    Map<String, String> getJobProperties();

}
