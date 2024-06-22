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
package com.oceanbase.odc.service.task.constants;

/**
 * odc server call task executor
 */
public class JobExecutorUrls {

    public static final String QUERY_LOG = "/api/v2/task/%s/log";
    public static final String STOP_TASK = "/api/v2/task/%s/stop";

    /**
     * for odc server monitor task use pull mode
     */
    public static final String HEARTBEAT = "/api/v2/task/%s/heartbeat";
    public static final String GET_RESULT = "/api/v2/task/%s/result";

    /**
     * TODO: seem like modifyJobParameters method is not used
     */
    public static final String MODIFY_JOB_PARAMETERS = "/api/v2/task/%s/modifyJobParameters";

}
