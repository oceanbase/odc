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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration.CloudProvider;

import lombok.Data;

@Data
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "odc.cloud.object-storage")
public class CloudObjectStorageProperties {
    private CloudProvider provider = CloudProvider.NONE;
    private String region;
    private String publicEndpoint;
    private String internalEndpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    /**
     * for query roleArn from OCP API
     */
    private String roleName = "ODCCloudStoragePutOnly";
    /**
     * if local mode, set roleArn here
     */
    private String roleArn;
    private String roleSessionName;
}
