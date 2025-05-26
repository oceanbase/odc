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
package com.oceanbase.odc.service.objectstorage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.oceanbase.odc.service.objectstorage.cloud.client.AzureCloudClient;

/**
 * @author longpeng.zlp
 * @date 2025/5/12 11:46
 */
public class AzureCloudClientTest {

    private AzureCloudClient azureCloudClient;
    private BlobServiceClient serviceClient;

    @Before
    public void init() {
        serviceClient = Mockito.mock(BlobServiceClient.class);
        azureCloudClient = new AzureCloudClient(serviceClient, "ss");
    }

    @Test
    public void testBucketExits() {
        BlobContainerClient existClient = Mockito.mock(BlobContainerClient.class);
        Mockito.when(existClient.exists()).thenReturn(true);
        BlobContainerClient notExistClient = Mockito.mock(BlobContainerClient.class);
        Mockito.when(notExistClient.exists()).thenReturn(false);

        Mockito.when(serviceClient.getBlobContainerClient("odccontainer")).thenReturn(existClient);
        Mockito.when(serviceClient.getBlobContainerClient("ssssss")).thenReturn(notExistClient);
        Assert.assertTrue(azureCloudClient.doesBucketExist("odccontainer"));
        Assert.assertFalse(azureCloudClient.doesBucketExist("ssssss"));
    }

    @Test
    public void testGetLocation() {
        Assert.assertEquals("ss", azureCloudClient.getBucketLocation("odccontainer"));
    }
}
