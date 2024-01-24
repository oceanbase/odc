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
import java.util.Optional;
import java.util.function.Supplier;

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
    private final Map<String, String> ENVS = new HashMap<>();
    private final TextEncryptor textEncryptor;

    public JobEnvBuilder() {
        String key = PasswordUtils.random(32);
        String salt = PasswordUtils.random(8);
        textEncryptor = Encryptors.aesBase64(key, salt);
        ENVS.put(JobEnvKeyConstants.ENCRYPT_KEY, key);
        ENVS.put(JobEnvKeyConstants.ENCRYPT_SALT, salt);
    }

    public Map<String, String> buildMap(JobContext context, TaskRunModeEnum runMode) {

        putEnv(JobEnvKeyConstants.ODC_BOOT_MODE, () -> JobConstants.ODC_BOOT_MODE_EXECUTOR);
        putEnv(JobEnvKeyConstants.ODC_TASK_RUN_MODE, runMode::name);

        putEnvWithEncrypted(JobEnvKeyConstants.ODC_JOB_CONTEXT, () -> JobUtils.toJson(context));

        CloudEnvConfigurations cloudEnvConfigurations = JobConfigurationHolder.getJobConfiguration()
                .getCloudEnvConfigurations();
        PreConditions.notNull(cloudEnvConfigurations, "cloudEnvConfigurations");
        putEnvWithEncrypted(JobEnvKeyConstants.ODC_OBJECT_STORAGE_CONFIGURATION,
                () -> JsonUtils.toJson(cloudEnvConfigurations.getObjectStorageConfiguration()));

        putEnv(JobEnvKeyConstants.ODC_LOG_DIRECTORY,
                () -> SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_LOG_DIRECTORY));

        setDatabaseEnv();
        return ENVS;

    }

    private void setDatabaseEnv() {
        putOrAliasInEnv(JobEnvKeyConstants.DATABASE_HOST, JobEnvKeyConstants.DATABASE_HOST_ALIAS);
        putOrAliasInEnv(JobEnvKeyConstants.DATABASE_PORT, JobEnvKeyConstants.DATABASE_PORT_ALIAS);
        putOrAliasInEnv(JobEnvKeyConstants.DATABASE_NAME, JobEnvKeyConstants.DATABASE_NAME_ALIAS);
        putOrAliasInEnv(JobEnvKeyConstants.DATABASE_USERNAME, JobEnvKeyConstants.DATABASE_USERNAME_ALIAS);
        putOrAliasInEnv(JobEnvKeyConstants.DATABASE_PASSWORD, JobEnvKeyConstants.DATABASE_PASSWORD_ALIAS);
    }

    private void putOrAliasInEnv(String envName, String envAlias) {
        putEnvWithEncrypted(envName, () -> Optional.ofNullable(SystemUtils.getEnvOrProperty(envName))
                .orElse(SystemUtils.getEnvOrProperty(envAlias)));
    }


    private void putEnvWithEncrypted(String envName, Supplier<String> envSupplier) {
        String envValue;
        if ((envValue = envSupplier.get()) != null) {
            ENVS.put(envName, textEncryptor.encrypt(envValue));
        }
    }

    private void putEnv(String envName, Supplier<String> envSupplier) {
        String envValue;
        if ((envValue = envSupplier.get()) != null) {
            ENVS.put(envName, (envValue));
        }
    }

}
