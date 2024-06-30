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

import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudEnvConfigurations;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.model.ExecutorMetadbCredential;

/**
 * share the same credentials as odc server by default
 */
public class DefaultJobCredentialProvider implements JobCredentialProvider {

    private final CloudEnvConfigurations cloudEnvConfigurations;

    public DefaultJobCredentialProvider(CloudEnvConfigurations cloudEnvConfigurations) {
        this.cloudEnvConfigurations = cloudEnvConfigurations;
    }

    @Override
    public ObjectStorageConfiguration getCloudObjectStorageCredential(JobContext jobContext) {
        return cloudEnvConfigurations.getObjectStorageConfiguration();
    }

    @Override
    public ExecutorMetadbCredential getExecutorMetadbCredential(JobContext jobContext) {
        ExecutorMetadbCredential credential = new ExecutorMetadbCredential();
        credential.setHost(getFromEnv("ODC_DATABASE_HOST", "DATABASE_HOST"));
        credential.setPort(Integer.parseInt(getFromEnv("ODC_DATABASE_PORT", "DATABASE_PORT")));
        credential.setDatabase(getFromEnv("ODC_DATABASE_NAME", "DATABASE_NAME"));
        credential.setUsername(getFromEnv("ODC_DATABASE_USERNAME", "DATABASE_USERNAME"));
        credential.setPassword(getFromEnv("ODC_DATABASE_PASSWORD", "DATABASE_PASSWORD"));
        return credential;
    }

    private String getFromEnv(String... environmentNames) {
        for (String envKey : environmentNames) {
            String value = SystemUtils.getEnvOrProperty(envKey);
            if (value != null) {
                return value;
            }
        }
        throw new RuntimeException("No environment variable found for " + environmentNames[0]);
    }
}
