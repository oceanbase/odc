/*
 * Copyright (c) 2024 OceanBase.
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
package com.oceanbase.odc.service.objectstorage.cloud;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.service.objectstorage.client.CloudObjectStorageClient;
import com.oceanbase.odc.service.objectstorage.cloud.client.NullCloudClient;

public class CloudObjectStorageClientTest {
    private CloudObjectStorageClient service =
            new CloudObjectStorageClient(new NullCloudClient(), new NullCloudClient(), null);

    @Test
    public void calculatePartSize_LessThanPartCount_MINSIZE() {
        long partSize = service.calculatePartSize(1L);
        Assert.assertEquals(1024 * 1024 * 5L, partSize);
    }

    @Test
    public void calculatePartSize_GreaterThanPartCount_MINSIZE() {
        long partSize = service.calculatePartSize(1001L);
        Assert.assertEquals(1024 * 1024 * 5L, partSize);
    }

    @Test
    public void calculatePartSize_EqualsMin_MINSIZE() {
        long partSize = service.calculatePartSize(102400L * 10000);
        Assert.assertEquals(1024 * 1024 * 5L, partSize);
    }

    @Test
    public void calculatePartSize_EqualsMinPlus1_MINSIZE() {
        long partSize = service.calculatePartSize(102400L * 10000 + 1L);
        Assert.assertEquals(1024 * 1024 * 5L, partSize);
    }
}
