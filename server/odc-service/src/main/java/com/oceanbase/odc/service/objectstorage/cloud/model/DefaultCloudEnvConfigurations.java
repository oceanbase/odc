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
package com.oceanbase.odc.service.objectstorage.cloud.model;

public class DefaultCloudEnvConfigurations implements CloudEnvConfigurations {
    private final CloudObjectStorageProperties cloudObjectStorageProperties;

    public DefaultCloudEnvConfigurations(CloudObjectStorageProperties cloudObjectStorageProperties) {
        this.cloudObjectStorageProperties = cloudObjectStorageProperties;
    }

    @Override
    public ObjectStorageConfiguration getObjectStorageConfiguration() {
        ObjectStorageConfiguration configuration = new ObjectStorageConfiguration();
        configuration.setCloudProvider(cloudObjectStorageProperties.getProvider());
        configuration.setRegion(cloudObjectStorageProperties.getRegion());
        configuration.setPublicEndpoint(cloudObjectStorageProperties.getPublicEndpoint());
        configuration.setInternalEndpoint(cloudObjectStorageProperties.getInternalEndpoint());
        configuration.setAccessKeyId(cloudObjectStorageProperties.getAccessKeyId());
        configuration.setAccessKeySecret(cloudObjectStorageProperties.getAccessKeySecret());
        configuration.setBucketName(cloudObjectStorageProperties.getBucketName());
        configuration.setRoleArn(cloudObjectStorageProperties.getRoleArn());
        configuration.setRoleSessionName(cloudObjectStorageProperties.getRoleSessionName());
        return configuration;
    }
}
