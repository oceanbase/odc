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

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudEnvConfigurations;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.enums.TaskRunMode;
import com.oceanbase.odc.service.task.executor.logger.LogUtils;
import com.oceanbase.odc.service.task.util.JobUtils;

/**
 * @author yaobin
 * @date 2024-01-22
 * @since 4.2.4
 */
public class JobEnvironmentFactory {

    private final Map<String, String> environments = new HashMap<>();

    public Map<String, String> getEnvironments(JobContext context, TaskRunMode runMode) {
        putEnv(JobEnvKeyConstants.ODC_BOOT_MODE, () -> JobConstants.ODC_BOOT_MODE_EXECUTOR);
        putEnv(JobEnvKeyConstants.ODC_TASK_RUN_MODE, runMode::name);
        putEnv(JobEnvKeyConstants.ODC_JOB_CONTEXT, () -> JobUtils.toJson(context));
        CloudEnvConfigurations cloudEnvConfigurations = JobConfigurationHolder.getJobConfiguration()
                .getCloudEnvConfigurations();
        if (cloudEnvConfigurations != null) {
            putEnv(JobEnvKeyConstants.ODC_OBJECT_STORAGE_CONFIGURATION,
                    () -> JsonUtils.toJson(cloudEnvConfigurations.getObjectStorageConfiguration()));
        }

        putEnv(JobEnvKeyConstants.ODC_LOG_DIRECTORY, LogUtils::getBaseLogPath);
        setDatabaseEnv();
        long userId = TraceContextHolder.getUserId() != null ? TraceContextHolder.getUserId() : -1;
        putEnv(JobEnvKeyConstants.ODC_EXECUTOR_USER_ID, () -> userId + "");
        return environments;

    }

    private void setDatabaseEnv() {
        putFromEnv(JobEnvKeyConstants.ODC_EXECUTOR_DATABASE_HOST, "ODC_DATABASE_HOST", "DATABASE_HOST");
        putFromEnv(JobEnvKeyConstants.ODC_EXECUTOR_DATABASE_PORT, "ODC_DATABASE_PORT", "DATABASE_PORT");
        putFromEnv(JobEnvKeyConstants.ODC_EXECUTOR_DATABASE_NAME, "ODC_DATABASE_NAME", "DATABASE_NAME");
        putFromEnv(JobEnvKeyConstants.ODC_EXECUTOR_DATABASE_USERNAME, "ODC_DATABASE_USERNAME", "DATABASE_USERNAME");
        putFromEnv(JobEnvKeyConstants.ODC_EXECUTOR_DATABASE_PASSWORD, "ODC_DATABASE_PASSWORD", "DATABASE_PASSWORD");
    }

    private void putFromEnv(String newEnv, String fromEnv, String otherEnv) {
        putEnv(newEnv, () -> Optional.ofNullable(SystemUtils.getEnvOrProperty(fromEnv)).orElse(otherEnv));
    }

    private void putEnv(String envName, Supplier<String> envSupplier) {
        String envValue;
        if ((envValue = envSupplier.get()) != null) {
            environments.put(envName, (envValue));
        }
    }

}
