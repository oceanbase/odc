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
package com.oceanbase.odc.service.cloud.resource.aws;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.oceanbase.odc.service.objectstorage.cloud.client.AmazonCloudClient;

/**
 * @Author：tinker
 * @Date: 2023/2/15 15:21
 * @Descripition:
 */
public class AmazonCloudClientTest {

    @Test
    public void generatePresignedUrl() throws MalformedURLException {
        AmazonS3 s3 = mock(AmazonS3Client.class);
        Mockito.when(s3.generatePresignedUrl(any())).thenReturn(new URL("http://test.sql"));
        AmazonCloudClient alibabaCloudClient = new AmazonCloudClient(s3, null, "test", "test");
        URL url = alibabaCloudClient.generatePresignedUrl("test", "test", new Date());
        Assert.assertNotNull(url);
    }
}
