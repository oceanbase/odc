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

import java.util.Map;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudEnvConfigurations;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.enums.TaskRunModeEnum;
import com.oceanbase.odc.service.task.util.JobUtils;

/**
 * @author yaobin
 * @date 2023-11-29
 * @since 4.2.4
 */
public class JobCallerBuilder {

    public static JobCaller buildJvmCaller() {
        return new JvmJobCaller();
    }


    public static JobCaller buildK8sJobCaller(K8sJobClient k8sJobClient, PodConfig podConfig, JobContext context) {

        PodParam podParam = podConfig.getPodParam();

        Map<String, String> envs = podParam.getEnvironments();
        envs.put(JobEnvKeyConstants.ODC_BOOT_MODE, JobConstants.ODC_BOOT_MODE_EXECUTOR);
        envs.put(JobEnvKeyConstants.ODC_TASK_RUN_MODE, TaskRunModeEnum.K8S.name());
        if (context != null) {
            envs.put(JobEnvKeyConstants.ODC_JOB_CONTEXT, JobUtils.toJson(context));
        }

        envs.put(JobEnvKeyConstants.ODC_DATABASE_HOST,
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_DATABASE_HOST));
        envs.put(JobEnvKeyConstants.ODC_DATABASE_PORT,
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_DATABASE_PORT));
        envs.put(JobEnvKeyConstants.ODC_DATABASE_NAME,
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_DATABASE_NAME));
        envs.put(JobEnvKeyConstants.ODC_DATABASE_USERNAME,
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_DATABASE_USERNAME));
        envs.put(JobEnvKeyConstants.ODC_DATABASE_PASSWORD,
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_DATABASE_PASSWORD));

        envs.put(JobEnvKeyConstants.ODC_LOG_DIRECTORY,
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_LOG_DIRECTORY));

        CloudEnvConfigurations cloudEnvConfigurations = JobConfigurationHolder.getJobConfiguration()
                .getCloudEnvConfigurations();
        PreConditions.notNull(cloudEnvConfigurations, "cloudEnvConfigurations");
        envs.put(JobEnvKeyConstants.ODC_OBJECT_STORAGE_CONFIGURATION,
                JsonUtils.toJson(cloudEnvConfigurations.getObjectStorageConfiguration()));

        return new K8sJobCaller(k8sJobClient, podConfig);
    }
}
