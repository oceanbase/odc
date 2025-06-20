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
import java.util.Objects;
import java.util.function.Supplier;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.enums.TaskRunMode;
import com.oceanbase.odc.service.task.model.ExecutorMetadbCredential;
import com.oceanbase.odc.service.task.schedule.JobCredentialProvider;
import com.oceanbase.odc.service.task.util.JobUtils;

/**
 * @author yaobin
 * @date 2024-01-22
 * @since 4.2.4
 */
public class JobEnvironmentFactory {

    private final Map<String, String> environments = new HashMap<>();

    public Map<String, String> build(JobContext context, TaskRunMode runMode, String logPath) {
        return build(context, runMode, JobConfigurationHolder.getJobConfiguration(), logPath);
    }

    public Map<String, String> build(JobContext context, TaskRunMode runMode, JobConfiguration configuration,
            String logPath) {
        putEnv(JobEnvKeyConstants.ODC_BOOT_MODE, () -> JobConstants.ODC_BOOT_MODE_EXECUTOR);
        putEnv(JobEnvKeyConstants.ODC_TASK_RUN_MODE, runMode::name);
        if (runMode.isK8s()) {
            putEnv(JobEnvKeyConstants.ODC_JOB_CONTEXT, () -> JobUtils.toJson(context));
        }
        JobCredentialProvider jobCredentialProvider = configuration.getJobCredentialProvider();

        ObjectStorageConfiguration cloudObjectStorageCredential = jobCredentialProvider
                .getCloudObjectStorageCredential(context);
        if (Objects.nonNull(cloudObjectStorageCredential)) {
            putEnv(JobEnvKeyConstants.ODC_OBJECT_STORAGE_CONFIGURATION,
                    () -> JsonUtils.toJson(cloudObjectStorageCredential));
        }

        ExecutorMetadbCredential executorMetadbCredential = jobCredentialProvider.getExecutorMetadbCredential(context);
        if (Objects.nonNull(executorMetadbCredential)) {
            putEnv(JobEnvKeyConstants.ODC_EXECUTOR_DATABASE_HOST, executorMetadbCredential::getHost);
            putEnv(JobEnvKeyConstants.ODC_EXECUTOR_DATABASE_PORT, () -> executorMetadbCredential.getPort() + "");
            putEnv(JobEnvKeyConstants.ODC_EXECUTOR_DATABASE_NAME, executorMetadbCredential::getDatabase);
            putEnv(JobEnvKeyConstants.ODC_EXECUTOR_DATABASE_USERNAME, executorMetadbCredential::getUsername);
            putEnv(JobEnvKeyConstants.ODC_EXECUTOR_DATABASE_PASSWORD, executorMetadbCredential::getPassword);
        }

        putEnv(JobEnvKeyConstants.ODC_LOG_DIRECTORY, () -> logPath);

        long userId = TraceContextHolder.getUserId() != null ? TraceContextHolder.getUserId() : -1;
        putEnv(JobEnvKeyConstants.ODC_EXECUTOR_USER_ID, () -> userId + "");
        return environments;
    }


    private void putEnv(String envName, Supplier<String> envSupplier) {
        String envValue;
        if ((envValue = envSupplier.get()) != null) {
            environments.put(envName, (envValue));
        }
    }

}
