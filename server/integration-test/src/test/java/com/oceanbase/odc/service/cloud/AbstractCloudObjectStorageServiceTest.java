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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
abstract class AbstractCloudObjectStorageServiceTest {
    public static final String TEST_FILE_PATH = "src/test/resources/data/test0001.txt";
    public static final String TEST_FILE_CN_ZH_PATH = "src/test/resources/data/中文名称.txt";
    private final CloudObjectStorageService cloudObjectStorageService;
    private File tempFile;
    private String objectName;

    public AbstractCloudObjectStorageServiceTest() {
        this.cloudObjectStorageService = createCloudObjectStorageService();
    }

    abstract CloudObjectStorageService createCloudObjectStorageService();

    @Before
    public void setUp() throws Exception {
        this.tempFile = null;
        this.objectName = null;
    }

    @After
    public void tearDown() throws Exception {
        if (Objects.nonNull(this.tempFile)) {
            FileUtils.forceDelete(this.tempFile);
        }
        if (Objects.nonNull(this.objectName)) {
            cloudObjectStorageService.delete(this.objectName);
        }
    }

    @Test
    public void supported() {
        boolean supported = cloudObjectStorageService.supported();
        Assert.assertTrue(supported);
    }

    @Test
    public void upload_ByFileNameAndFile() throws IOException {
        objectName = cloudObjectStorageService.upload("test", new File(TEST_FILE_PATH));

        tempFile = cloudObjectStorageService.downloadToTempFile(objectName);
        Assert.assertEquals("test0001", readFirstLine(tempFile));
    }

    @Test
    public void upload_ByFileNameAndFileCNZH() throws IOException {
        objectName = cloudObjectStorageService.upload("中文名称.txt", new File(TEST_FILE_CN_ZH_PATH));

        tempFile = cloudObjectStorageService.downloadToTempFile(objectName);
        Assert.assertEquals("中文内容", readFirstLine(tempFile));
    }

    @Test
    public void upload_ByInputStream() throws IOException {
        try (InputStream is = new FileInputStream(new File(TEST_FILE_PATH))) {
            objectName = cloudObjectStorageService.upload("test", is);
        }
        tempFile = cloudObjectStorageService.downloadToTempFile(objectName);
        Assert.assertEquals("test0001", readFirstLine(tempFile));
    }

    @Test
    public void upload_LargeFileUseMultiPart() throws IOException {
        tempFile = File.createTempFile("test", null, new File("."));
        generateSpecificSizeFile(tempFile, 10 * 1024 * 1024L);

        String objectName = cloudObjectStorageService.upload("test", tempFile);
        log.info("objectName={}", objectName);
    }

    @Test
    public void generateDownloadUrl() throws IOException {
        objectName = cloudObjectStorageService.upload("test", new File(TEST_FILE_PATH));
        URL downloadUrl = cloudObjectStorageService.generateDownloadUrl(objectName);

        tempFile = File.createTempFile("test", null, new File("."));
        downloadFromUrlToFile(downloadUrl, tempFile);

        Assert.assertEquals("test0001", readFirstLine(tempFile));
    }

    @Test
    public void generateDownloadUrl_CN_ZH() throws IOException {
        objectName = cloudObjectStorageService.upload("中文名称", new File(TEST_FILE_CN_ZH_PATH));
        URL downloadUrl = cloudObjectStorageService.generateDownloadUrl(objectName);
        tempFile = File.createTempFile("test", null, new File("."));
        downloadFromUrlToFile(downloadUrl, tempFile);
        Assert.assertEquals("中文内容", readFirstLine(tempFile));
    }

    @Test
    public void generateUploadUrl() {
        this.objectName = cloudObjectStorageService.generateObjectName("test.txt");
        URL uploadUrl = cloudObjectStorageService.generateUploadUrl(this.objectName);
        log.info("uploadUrl={}", uploadUrl);
    }

    @Test
    public void generateUploadUrl_CNZH() {
        this.objectName = cloudObjectStorageService.generateObjectName("中文名称.txt");
        URL uploadUrl = cloudObjectStorageService.generateUploadUrl(this.objectName);
        log.info("uploadUrl={}", uploadUrl);
    }

    @Test
    public void readContent() throws IOException {
        objectName = cloudObjectStorageService.upload("test", new File(TEST_FILE_PATH));

        byte[] content = cloudObjectStorageService.readContent(objectName);
        String contentValue = new String(content, "UTF-8");

        Assert.assertEquals("test0001", contentValue);
    }

    private File downloadFromUrlToFile(URL url, File file) throws IOException {
        FileUtils.copyURLToFile(url, file, 3000, 5000);
        return file;
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

    private void generateSpecificSizeFile(File file, long size) throws IOException {
        RandomAccessFile f = new RandomAccessFile(file, "rw");
        f.setLength(size);
        f.close();
    }

}
