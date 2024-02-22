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
 * @date 2024-01-12
 * @since 4.2.4
 */
public class JobUrlConstants {

    public static final String TASK_RESULT_UPLOAD = "/api/v2/task/result";

    public static final String TASK_HEART = "/api/v2/task/heart";

    public static final String TASK_QUERY_SENSITIVE_COLUMN = "/api/v2/task/querySensitiveColumn";

    public static final String LOG_QUERY = "/api/v2/task/%s/log";

    public static final String STOP_TASK = "/api/v2/task/%s/stop";

}
