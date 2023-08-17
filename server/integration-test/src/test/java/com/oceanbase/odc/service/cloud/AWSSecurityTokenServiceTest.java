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

import java.io.File;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.oceanbase.odc.ITConfigurations;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.objectstorage.cloud.CloudResourceConfigurations;
import com.oceanbase.odc.service.objectstorage.cloud.CloudResourceService;
import com.oceanbase.odc.service.objectstorage.cloud.CloudResourceService.GenerateTempCredentialReq;
import com.oceanbase.odc.service.objectstorage.cloud.client.CloudClient;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.objectstorage.cloud.model.UploadObjectTemporaryCredential;

public class AWSSecurityTokenServiceTest {
    private static final String FILE_NAME = "src/test/resources/data/test0001.txt";
    private static final String FILE_NAME_CN_ZH = "src/test/resources/data/中文名称.txt";
    private static ObjectStorageConfiguration configuration;
    private static CloudResourceService service;

    @BeforeClass
    public static void beforeClass() {
        configuration = ITConfigurations.getS3Configuration();
        CloudClient cloudClient = new CloudResourceConfigurations().cloudClient(() -> configuration);
        CloudObjectStorageService cloudObjectStorageService =
                new CloudObjectStorageService(cloudClient, () -> configuration);
        service = new CloudResourceService(cloudObjectStorageService, cloudClient);
    }

    @Test
    public void generateTempCredential() {
        generateTempCredential("test0001.txt", FILE_NAME);
    }

    @Test
    public void generateTempCredential_CNZH() {
        generateTempCredential("中文名称.txt", FILE_NAME_CN_ZH);
    }

    public void generateTempCredential(String fileName, String filePath) {
        GenerateTempCredentialReq req = new GenerateTempCredentialReq();
        req.setFileName(fileName);
        UploadObjectTemporaryCredential credential = service.generateTempCredential(req);

        AWSSessionCredentials sessionCredentials = new BasicSessionCredentials(
                credential.getAccessKeyId(), credential.getAccessKeySecret(),
                credential.getSecurityToken());

        AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(sessionCredentials))
                .withRegion(credential.getRegion())
                .disableChunkedEncoding()
                .build();

        PutObjectResult putObjectResult =
                s3.putObject(configuration.getBucketName(), credential.getFilePath(), new File(filePath));

        Assert.assertNotNull(putObjectResult.getETag());
    }


}
