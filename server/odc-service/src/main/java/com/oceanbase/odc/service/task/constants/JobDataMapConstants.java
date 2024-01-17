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

import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.service.flow.task.model.RuntimeTaskConstants;

/**
 * @author yaobin
 * @date 2023-12-06
 * @since 4.2.4
 */
public class JobDataMapConstants {

    public static final String CONNECTION_CONFIG = "connectionConfig";

    public static final String META_DB_TASK_PARAMETER = "metaDbTaskParameter";

    public static final String FLOW_INSTANCE_ID = "flowInstanceId";

    public static final String CURRENT_SCHEMA_KEY = ConnectionSessionConstants.CURRENT_SCHEMA_KEY;

    public static final String SESSION_TIME_ZONE = ConnectionSessionConstants.SESSION_TIME_ZONE;

    public static final String OBJECT_METADATA = "objectMetadata";

    public static final String TASK_EXECUTION_TIMEOUT_MILLIS = RuntimeTaskConstants.TIMEOUT_MILLI_SECONDS;
}
