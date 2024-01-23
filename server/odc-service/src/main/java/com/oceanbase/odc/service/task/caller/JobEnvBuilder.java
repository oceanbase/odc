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

import java.util.HashMap;
import java.util.Map;

import com.oceanbase.odc.common.crypto.Encryptors;
import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.security.PasswordUtils;
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
 * @date 2024-01-22
 * @since 4.2.4
 */
public class JobEnvBuilder {

    public static Map<String, String> buildMap(JobContext context, TaskRunModeEnum runMode) {

        Map<String, String> envs = new HashMap<>();

        String key = PasswordUtils.random(32);
        String salt = PasswordUtils.random(8);
        TextEncryptor textEncryptor = Encryptors.aesBase64(key, salt);

        envs.put(JobEnvKeyConstants.ENCRYPT_KEY, key);
        envs.put(JobEnvKeyConstants.ENCRYPT_SALT, salt);

        envs.put(JobEnvKeyConstants.ODC_BOOT_MODE, JobConstants.ODC_BOOT_MODE_EXECUTOR);
        envs.put(JobEnvKeyConstants.ODC_TASK_RUN_MODE, runMode.name());
        if (context != null) {
            envs.put(JobEnvKeyConstants.ODC_JOB_CONTEXT, textEncryptor.encrypt(JobUtils.toJson(context)));
        }
        envs.put(JobEnvKeyConstants.ODC_LOG_DIRECTORY,
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_LOG_DIRECTORY));

        CloudEnvConfigurations cloudEnvConfigurations = JobConfigurationHolder.getJobConfiguration()
                .getCloudEnvConfigurations();
        PreConditions.notNull(cloudEnvConfigurations, "cloudEnvConfigurations");
        envs.put(JobEnvKeyConstants.ODC_OBJECT_STORAGE_CONFIGURATION,
                textEncryptor.encrypt(JsonUtils.toJson(cloudEnvConfigurations.getObjectStorageConfiguration())));
        setDatabaseEnv(envs);
        return envs;

    }

    private static void setDatabaseEnv(Map<String, String> envs) {
        envs.put(JobEnvKeyConstants.DATABASE_HOST,
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.DATABASE_HOST));
        envs.put(JobEnvKeyConstants.DATABASE_PORT,
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.DATABASE_PORT));
        envs.put(JobEnvKeyConstants.DATABASE_NAME,
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.DATABASE_NAME));
        envs.put(JobEnvKeyConstants.DATABASE_USERNAME,
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.DATABASE_USERNAME));
        envs.put(JobEnvKeyConstants.DATABASE_PASSWORD,
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.DATABASE_PASSWORD));
    }


}
