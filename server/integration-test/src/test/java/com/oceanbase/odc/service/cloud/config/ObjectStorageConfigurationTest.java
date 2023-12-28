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
package com.oceanbase.odc.service.cloud.config;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration.CloudProvider;

public class ObjectStorageConfigurationTest {

    @Test
    public void getEndpoint_S3EmptyEndpoint_FromRegion() {
        ObjectStorageConfiguration properties = new ObjectStorageConfiguration();
        properties.setCloudProvider(CloudProvider.AWS);
        properties.setRegion("cn-northwest-1");
        String endpoint = properties.getPublicEndpoint();
        Assert.assertEquals("s3.cn-northwest-1.amazonaws.com.cn", endpoint);
    }

    @Test
    public void getEndpoint_OssEmptyEndpoint_FromRegion() {
        ObjectStorageConfiguration properties = new ObjectStorageConfiguration();
        properties.setCloudProvider(CloudProvider.ALIBABA_CLOUD);
        properties.setRegion("cn-hangzhou");
        String endpoint = properties.getPublicEndpoint();
        Assert.assertEquals("oss-cn-hangzhou.aliyuncs.com", endpoint);
    }
}
