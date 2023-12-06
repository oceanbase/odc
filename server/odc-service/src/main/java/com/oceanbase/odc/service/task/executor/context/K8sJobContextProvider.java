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

package com.oceanbase.odc.service.task.executor.context;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.task.caller.DefaultJobContext;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.constants.JobEnvConstants;

/**
 * @author gaoda.xy
 * @date 2023/11/22 20:21
 */
public class K8sJobContextProvider implements JobContextProvider {

    @Override
    public JobContext provide() {
        String jobContextJson = SystemUtils.getEnvOrProperty(JobEnvConstants.TASK_ALL_PARAMETERS);
        Verify.notBlank(jobContextJson, JobEnvConstants.TASK_RUN_MODE);
        return JsonUtils.fromJson(jobContextJson, DefaultJobContext.class);
    }

}
