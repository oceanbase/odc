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
 * @author yaobin
 * @date 2024-01-15
 * @since 4.2.4
 */
public class JobEntityColumn {

    public static final String ID = "id";

    public static final String STATUS = "status";

    public static final String LAST_HEART_TIME = "lastHeartTime";

    public static final String CREATE_TIME = "createTime";

    public static final String STARTED_TIME = "startedTime";

    public static final String FINISHED_TIME = "finishedTime";

    public static final String CANCELLING_TIME = "cancellingTime";

    public static final String EXECUTOR_DESTROYED_TIME = "executorDestroyedTime";

    public static final String DESCRIPTION = "description";

    public static final String EXECUTOR_ENDPOINT = "executorEndpoint";

    public static final String RUN_MODE = "runMode";

    public static final String EXECUTOR_IDENTIFIER = "executorIdentifier";

    public static final String JOB_PARAMETERS_JSON = "jobParametersJson";

    public static final String EXECUTION_TIMES = "executionTimes";

}
