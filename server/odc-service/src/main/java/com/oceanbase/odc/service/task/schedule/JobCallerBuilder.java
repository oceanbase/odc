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

import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.task.caller.JobCaller;
import com.oceanbase.odc.service.task.caller.JvmJobCaller;
import com.oceanbase.odc.service.task.caller.K8sJobCaller;
import com.oceanbase.odc.service.task.caller.K8sJobClient;
import com.oceanbase.odc.service.task.caller.PodConfig;
import com.oceanbase.odc.service.task.caller.PodParam;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.constants.JobEnvConstants;
import com.oceanbase.odc.service.task.enums.TaskRunModeEnum;

/**
 * @author yaobin
 * @date 2023-11-29
 * @since 4.2.4
 */
public class JobCallerBuilder {

    public static JobCaller buildJvmCaller() {
        return new JvmJobCaller();
    }


    public static JobCaller buildK8sJobCaller(K8sJobClient k8sJobClient, PodConfig podConfig) {

        PodParam podParam = podConfig.getPodParam();

        Map<String, String> envs = podParam.getEnvironments();
        envs.put(JobEnvConstants.BOOT_MODE, JobConstants.ODC_BOOT_MODE_EXECUTOR);
        envs.put(JobEnvConstants.TASK_RUN_MODE, TaskRunModeEnum.K8S.name());

        envs.put("DATABASE_HOST", SystemUtils.getEnvOrProperty("ODC_DATABASE_HOST"));
        envs.put("DATABASE_PORT", SystemUtils.getEnvOrProperty("ODC_DATABASE_PORT"));
        envs.put("DATABASE_NAME", SystemUtils.getEnvOrProperty("ODC_DATABASE_NAME"));
        envs.put("DATABASE_USERNAME", SystemUtils.getEnvOrProperty("ODC_DATABASE_USERNAME"));
        envs.put("DATABASE_PASSWORD", SystemUtils.getEnvOrProperty("ODC_DATABASE_PASSWORD"));

        podParam.setRequestCpu(1.0);
        podParam.setRequestMem(128L);
        return new K8sJobCaller(k8sJobClient, podConfig);
    }
}
