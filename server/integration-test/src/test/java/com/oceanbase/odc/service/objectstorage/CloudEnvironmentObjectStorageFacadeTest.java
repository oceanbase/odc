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

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.transaction.PlatformTransactionManager;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.operator.LocalFileOperator;
import com.oceanbase.odc.service.objectstorage.operator.ObjectMetaOperator;

public class CloudEnvironmentObjectStorageFacadeTest extends ServiceTestEnv {
    private static final String BUCKET_NAME = "test-bucket";
    private static final String OBJECT_NAME = "some-file.tmp";
    private static final String OBJECT_ID = "file-id.tmp";
    private static final String TARGET_NAME = "target-file.tmp";

    private PlatformTransactionManager transactionManager = Mockito.mock(PlatformTransactionManager.class);

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Mock
    private ObjectMetaOperator metaOperator;

    @Mock
    private LocalFileOperator localFileOperator;

    @Mock
    private CloudObjectStorageService cloudObjectStorageService;

    @InjectMocks
    private CloudEnvironmentObjectStorageFacade objectStorageFacade =
            new CloudEnvironmentObjectStorageFacade(10, 100000, transactionManager);

    @Before
    public void setUp() throws IOException {
        folder.newFolder(BUCKET_NAME);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setObjectId(OBJECT_ID);
        metadata.setBucketName(BUCKET_NAME);
        metadata.setObjectName(OBJECT_NAME);
        metadata.setTotalLength(1L);
        metadata.setSha1("sha1");
        Mockito.when(metaOperator.getObjectMeta(Mockito.any(), Mockito.any())).thenReturn(metadata);
        Mockito.when(localFileOperator.isLocalFileAbsent(Mockito.any())).thenReturn(true);
    }

    @Test
    public void test_loadMetaData_Success() throws IOException {
        Mockito.when(cloudObjectStorageService.downloadToTempFile(Mockito.any())).thenReturn(folder.newFile(OBJECT_ID));
        Mockito.when(localFileOperator.getOrCreateLocalFile(Mockito.any(), Mockito.any()))
                .thenReturn(folder.newFile(TARGET_NAME));
        objectStorageFacade.loadMetaData(BUCKET_NAME, OBJECT_ID);
        Assert.assertTrue(new File(folder.getRoot(), TARGET_NAME).exists());
    }

    @Test(expected = InternalServerError.class)
    public void test_loadMetaData_Fail() {
        Mockito.when(localFileOperator.isLocalFileAbsent(Mockito.any(ObjectMetadata.class))).thenReturn(true);
        Mockito.when(localFileOperator.deleteLocalFile(Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new RuntimeException());
        objectStorageFacade.loadMetaData(Mockito.anyString(), Mockito.anyString());
    }
}
