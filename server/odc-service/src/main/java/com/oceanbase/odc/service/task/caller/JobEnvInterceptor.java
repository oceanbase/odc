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

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;

/**
 * @author yaobin
 * @date 2024-01-26
 * @since 4.2.4
 */
public interface JobEnvInterceptor {

    void intercept(Map<String, String> environments);

    static List<String> getSensitiveKeys() {
        return Lists.newArrayList(
                JobEnvKeyConstants.ODC_DATABASE_HOST,
                JobEnvKeyConstants.ODC_DATABASE_PORT,
                JobEnvKeyConstants.ODC_DATABASE_NAME,
                JobEnvKeyConstants.ODC_DATABASE_USERNAME,
                JobEnvKeyConstants.ODC_DATABASE_PASSWORD,
                JobEnvKeyConstants.ODC_OBJECT_STORAGE_CONFIGURATION,
                JobEnvKeyConstants.ODC_JOB_CONTEXT);
    }
}
