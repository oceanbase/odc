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
package com.oceanbase.odc.service.cloud;

import com.oceanbase.odc.ITConfigurations;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.objectstorage.cloud.CloudResourceConfigurations;
import com.oceanbase.odc.service.objectstorage.cloud.client.CloudClient;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;

public class OSSObjcetStorageServiceIT extends AbstractCloudObjectStorageServiceTest {
    @Override
    CloudObjectStorageService createCloudObjectStorageService() {
        ObjectStorageConfiguration configuration = ITConfigurations.getS3Configuration();
        CloudClient cloudClient = new CloudResourceConfigurations().cloudClient(() -> configuration);
        return new CloudObjectStorageService(cloudClient, () -> configuration);
    }
}
