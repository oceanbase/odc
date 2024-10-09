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
package com.oceanbase.odc.service.objectstorage.client;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.FileEntity;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.oceanbase.odc.ITConfigurations;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.common.util.OdcFileUtil;
import com.oceanbase.odc.service.objectstorage.cloud.CloudResourceConfigurations;
import com.oceanbase.odc.service.objectstorage.cloud.client.CloudClient;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudObjectStorageConstants;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectTagging;
import com.oceanbase.odc.service.objectstorage.cloud.util.CloudObjectStorageUtil;

/**
 * @author keyang
 * @date 2024/08/20
 * @since 4.3.2
 */
@Ignore
public class OSSCloudObjectStorageClientTest {
    CloudObjectStorageClient ossObjectStorageClient;
    public static final String TEST_FILE_PATH = "src/test/resources/data/test0001.txt";
    public static final String TEST_DOWNLOAD_FILE =
            CloudObjectStorageConstants.TEMP_DIR + "/download/temp";
    public static final String TEST_FILE_CN_ZH_PATH = "src/test/resources/data/中文名称.txt";
    private File tempFile;
    private String objectName;

    @Before
    public void setUp() {
        ObjectStorageConfiguration configuration = ITConfigurations.getOssConfiguration();
        CloudClient cloudClient = new CloudResourceConfigurations().publicEndpointCloudClient(() -> configuration);
        CloudClient internalCloudClient =
                new CloudResourceConfigurations().internalEndpointCloudClient(() -> configuration);
        ossObjectStorageClient = new CloudObjectStorageClient(cloudClient,
                internalCloudClient, configuration);
    }

    @After
    public void clear() {
        OdcFileUtil.deleteFiles(new File(CloudObjectStorageConstants.TEMP_DIR));
        ossObjectStorageClient.deleteObjects(Collections.singletonList(objectName));
    }


    @Test
    public void uploadFile() throws IOException {
        objectName = CloudObjectStorageUtil.generateObjectName(null, UUID.randomUUID().toString(),
                CloudObjectStorageConstants.ODC_SERVER_PREFIX, "fileName");
        ossObjectStorageClient.putObject(objectName, new File(TEST_FILE_PATH), new ObjectTagging());
        tempFile = createFileWithParent(TEST_DOWNLOAD_FILE);
        ossObjectStorageClient.downloadToFile(objectName, tempFile);
        Assert.assertEquals("test0001", readFirstLine(tempFile));
    }


    @Test
    public void generateDownloadUrl() throws IOException {
        objectName = CloudObjectStorageUtil.generateObjectName(null, UUID.randomUUID().toString(),
                CloudObjectStorageConstants.ODC_SERVER_PREFIX, "fileName");
        ossObjectStorageClient.putObject(objectName, new File(TEST_FILE_PATH), new ObjectTagging());
        String customFileName = "customFileName";
        String downloadUrl = ossObjectStorageClient.generateDownloadUrl(objectName, 1000L, customFileName).toString();
        tempFile = createFileWithParent(TEST_DOWNLOAD_FILE);
        downloadFromUrlToFile(new URL(downloadUrl), tempFile);
        Assert.assertTrue(downloadUrl.contains(customFileName));
        Assert.assertEquals("test0001", readFirstLine(tempFile));
    }

    @Test
    public void generateUploadUrl() throws IOException {
        objectName = CloudObjectStorageUtil.generateObjectName(null, UUID.randomUUID().toString(),
                CloudObjectStorageConstants.ODC_SERVER_PREFIX, "fileName");
        String uploadUrl = ossObjectStorageClient.generateUploadUrl(this.objectName).toString();
        uploadByPreSignedUrl(uploadUrl, new File(TEST_FILE_PATH));
        tempFile = createFileWithParent(TEST_DOWNLOAD_FILE);
        ossObjectStorageClient.downloadToFile(objectName, tempFile);
        Assert.assertEquals("test0001", readFirstLine(tempFile));
    }


    @Test
    public void readContent() throws IOException {
        objectName = CloudObjectStorageUtil.generateObjectName(null, UUID.randomUUID().toString(),
                CloudObjectStorageConstants.ODC_SERVER_PREFIX, "fileName");
        ossObjectStorageClient.putObject(objectName, new File(TEST_FILE_PATH), new ObjectTagging());
        String contentValue = new String(ossObjectStorageClient.readContent(objectName), StandardCharsets.UTF_8);
        Assert.assertEquals("test0001", contentValue);
    }

    private void downloadFromUrlToFile(URL url, File file) throws IOException {
        FileUtils.copyURLToFile(url, file, 3000, 5000);
    }

    private String readFirstLine(File file) {
        Verify.notNull(file, "file");
        try {
            List<String> strings = Files.readLines(file, Charsets.UTF_8);
            return strings.get(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void uploadByPreSignedUrl(String presignedUrl, File file) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPut putRequest = new HttpPut(presignedUrl);
        putRequest.setHeader("Content-Type", ContentType.APPLICATION_OCTET_STREAM.getMimeType());
        FileEntity fileEntity = new FileEntity(file, ContentType.APPLICATION_OCTET_STREAM);
        putRequest.setEntity(fileEntity);
        HttpResponse response = httpClient.execute(putRequest);
        httpClient.close();
        assert response.getCode() == 200;
    }

    private File createFileWithParent(String filePath) throws IOException {
        File file = new File(filePath);
        File parentDirectories = FileUtils.createParentDirectories(file);
        while (!parentDirectories.exists()) {
            parentDirectories = FileUtils.createParentDirectories(parentDirectories);
        }
        return file;
    }
}
