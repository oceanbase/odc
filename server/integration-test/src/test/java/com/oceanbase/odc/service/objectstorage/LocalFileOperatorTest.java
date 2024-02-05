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

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.common.util.HashUtils;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.operator.LocalFileOperator;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocalFileOperatorTest extends ServiceTestEnv {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static final String BUCKET = "test-bucket";

    @Autowired
    private LocalFileOperator localFileOperator;

    @Before
    public void setup() throws IOException {
        folder.newFolder(BUCKET);
        log.info("Temp file path={}", folder.getRoot().getAbsolutePath());
        localFileOperator = new LocalFileOperator(folder.getRoot().getAbsolutePath());
        localFileOperator.init();
    }

    @Test
    public void testSaveLocalFile_Success() throws IOException {
        byte[] data = new byte[10];
        MultipartFile mockFile = new MockMultipartFile("some-file", "some-file", null, data);
        String sha1 = localFileOperator.saveLocalFile(BUCKET, mockFile.getOriginalFilename(), mockFile.getSize(),
                mockFile.getInputStream());
        assertEquals(HashUtils.sha1(data), sha1);
    }

    @SneakyThrows
    @Test
    public void testGetOrCreateLocalFile_Success() {
        MultipartFile mockFile = new MockMultipartFile("some-file", "some-file", null, new byte[10]);
        localFileOperator.saveLocalFile(BUCKET, mockFile.getOriginalFilename(), mockFile.getSize(),
                mockFile.getInputStream());
        File file = localFileOperator.getOrCreateLocalFile(BUCKET, "some-file");
        assertNotNull(file);
        assertTrue(file.exists());
    }

    @Test
    public void testDeleteQuietly_NonExists_ReturnFalse() {
        boolean result = localFileOperator.deleteLocalFile(BUCKET, "any-file-name");

        Assert.assertFalse(result);
    }

    @SneakyThrows
    @Test
    public void testDeleteQuietly_Exists_ReturnTrue() {
        MultipartFile mockFile = new MockMultipartFile("some-file", "some-file", null, new byte[10]);
        localFileOperator.saveLocalFile(BUCKET, mockFile.getOriginalFilename(), mockFile.getSize(),
                mockFile.getInputStream());

        boolean result = localFileOperator.deleteLocalFile(BUCKET, "some-file");
        Assert.assertTrue(result);
    }

    @Test
    public void testIsLocalFileAbsent_ReturnTrue() {
        ObjectMetadata ossMeta = new ObjectMetadata();
        ossMeta.setBucketName(BUCKET);
        ossMeta.setObjectId("some-file");
        boolean isLocalAbsent = localFileOperator.isLocalFileAbsent(ossMeta);
        assertTrue(isLocalAbsent);
    }

    @Test
    public void testIsLocalFileAbsent_ReturnFalse() throws IOException {
        String fileName = "someFile";
        byte[] data = new byte[10];

        MultipartFile mockFile = new MockMultipartFile("some-file", fileName, null, new byte[10]);
        localFileOperator.saveLocalFile(BUCKET, mockFile.getOriginalFilename(), mockFile.getSize(),
                mockFile.getInputStream());

        ObjectMetadata ossMeta = new ObjectMetadata();
        ossMeta.setBucketName(BUCKET);
        ossMeta.setObjectName(fileName);
        ossMeta.setSha1(HashUtils.sha1(data));
        ossMeta.setTotalLength(data.length);
        ossMeta.setObjectId(fileName);
        boolean isLocalAbsent = localFileOperator.isLocalFileAbsent(ossMeta);
        assertFalse(isLocalAbsent);
    }


}
