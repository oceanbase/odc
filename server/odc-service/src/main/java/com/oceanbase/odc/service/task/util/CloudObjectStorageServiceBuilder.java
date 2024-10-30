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
package com.oceanbase.odc.service.task.util;

import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.objectstorage.cloud.CloudResourceConfigurations;
import com.oceanbase.odc.service.objectstorage.cloud.client.CloudClient;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;

/**
 * @author yaobin
 * @date 2023-12-19
 * @since 4.2.4
 */
public class CloudObjectStorageServiceBuilder {

    public static CloudObjectStorageService build(ObjectStorageConfiguration storageConfig) {
        CloudClient publicCloudClient =
                new CloudResourceConfigurations.CloudClientBuilder().generatePublicCloudClient(storageConfig);
        CloudClient interalCloudClient =
                new CloudResourceConfigurations.CloudClientBuilder().generateCloudClient(storageConfig);
        return new CloudObjectStorageService(publicCloudClient, interalCloudClient, storageConfig);
    }
}
