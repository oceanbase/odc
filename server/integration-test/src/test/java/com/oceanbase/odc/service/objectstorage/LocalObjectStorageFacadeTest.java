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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.core.io.UrlResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.multipart.MultipartFile;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.objectstorage.model.Bucket;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.model.StorageObject;
import com.oceanbase.odc.service.objectstorage.operator.BucketOperator;
import com.oceanbase.odc.service.objectstorage.operator.LocalFileOperator;
import com.oceanbase.odc.service.objectstorage.operator.ObjectBlockIterator;
import com.oceanbase.odc.service.objectstorage.operator.ObjectBlockOperator;
import com.oceanbase.odc.service.objectstorage.operator.ObjectMetaOperator;

import lombok.SneakyThrows;

public class LocalObjectStorageFacadeTest extends ServiceTestEnv {

    private static final String BUCKET_NAME = "test-bucket";
    private static final String OBJECT_NAME = "some-file.tmp";
    private static final String OBJECT_ID = "file-id.tmp";
    private static final long OBJECT_LENGTH = 1000L;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);

    @InjectMocks
    private LocalObjectStorageFacade objectStorageFacade = new LocalObjectStorageFacade(10, 100000, transactionManager);

    @Mock
    private LocalFileOperator localFileOperator;

    @Mock
    private ObjectMetaOperator metaOperator;

    @Mock
    private BucketOperator bucketOperator;

    @Mock
    private ObjectBlockOperator blockOperator;

    @Mock
    private AuthenticationFacade authenticationFacade;



    @Before
    public void setUp() throws Exception {
        folder.newFolder(BUCKET_NAME);
        when(localFileOperator.saveLocalFile(anyString(), anyString(), anyLong(), any(InputStream.class)))
                .thenReturn("sha1");
        when(localFileOperator.getOrCreateLocalFile(eq(BUCKET_NAME), eq(OBJECT_ID)))
                .thenReturn(folder.newFile(OBJECT_ID));
        when(authenticationFacade.currentUserId()).thenReturn(1L);
    }

    @SneakyThrows
    @Test
    public void testPutObject_Success() {
        when(metaOperator.getObjectMetaNonException(anyString(), anyString())).thenReturn(Optional.empty());
        when(metaOperator.initSaving(anyString(), anyLong(), anyString(), anyString(), anyLong(), anyLong()))
                .thenReturn(getObjectMetadata());
        when(bucketOperator.getBucketByName(anyString())).thenReturn(Optional.of(new Bucket()));
        MultipartFile mockFile = new MockMultipartFile(OBJECT_NAME, OBJECT_NAME, null, new byte[(int) OBJECT_LENGTH]);
        ObjectMetadata meta =
                objectStorageFacade.putObject(BUCKET_NAME, OBJECT_NAME, mockFile.getSize(),
                        mockFile.getInputStream());
        assertNotNull(meta);
    }

    @SneakyThrows
    @Test
    public void testLoadObject_FromLocalFile_Success() {
        when(metaOperator.getObjectMeta(eq(BUCKET_NAME), eq(OBJECT_ID))).thenReturn(getObjectMetadata());
        when(localFileOperator.isLocalFileAbsent(any(ObjectMetadata.class))).thenReturn(false);
        when(localFileOperator.loadAsResource(eq(BUCKET_NAME), eq(OBJECT_ID)))
                .thenReturn(new UrlResource(folder.getRoot().getAbsoluteFile().toURI()));
        StorageObject storageObject = objectStorageFacade.loadObject(BUCKET_NAME, OBJECT_ID);
        assertNotNull(storageObject);
    }

    @SneakyThrows
    @Test
    public void testLoadObject_FromDatabase_Success() {
        when(metaOperator.getObjectMeta(eq(BUCKET_NAME), eq(OBJECT_ID))).thenReturn(getObjectMetadata());
        when(localFileOperator.isLocalFileAbsent(any(ObjectMetadata.class))).thenReturn(true);
        ObjectBlockIterator mockIterator = Mockito.mock(ObjectBlockIterator.class);
        when(blockOperator.getBlockIterator(anyString())).thenReturn(mockIterator);
        when(mockIterator.hasNext()).thenReturn(false);
        when(localFileOperator.loadAsResource(eq(BUCKET_NAME), eq(OBJECT_ID)))
                .thenReturn(new UrlResource(folder.getRoot().getAbsoluteFile().toURI()));

        objectStorageFacade.loadObject(BUCKET_NAME, OBJECT_ID);
        verify(localFileOperator, times(1)).deleteLocalFile(eq(BUCKET_NAME), eq(OBJECT_ID));
    }

    @Test
    @SneakyThrows
    public void test_loadMetaData_Success() {
        when(metaOperator.getObjectMeta(eq(BUCKET_NAME), eq(OBJECT_ID))).thenReturn(getObjectMetadata());
        when(localFileOperator.isLocalFileAbsent(any(ObjectMetadata.class))).thenReturn(true);
        ObjectBlockIterator mockIterator = Mockito.mock(ObjectBlockIterator.class);
        when(blockOperator.getBlockIterator(anyString())).thenReturn(mockIterator);
        when(mockIterator.hasNext()).thenReturn(false);

        objectStorageFacade.loadMetaData(BUCKET_NAME, OBJECT_ID);
        verify(localFileOperator, times(1)).deleteLocalFile(eq(BUCKET_NAME), eq(OBJECT_ID));
    }

    @Test(expected = InternalServerError.class)
    public void test_LoadMetaData_Fail() {
        when(metaOperator.getObjectMeta(eq(BUCKET_NAME), eq(OBJECT_ID))).thenReturn(getObjectMetadata());
        when(localFileOperator.isLocalFileAbsent(any(ObjectMetadata.class))).thenReturn(true);
        when(localFileOperator.deleteLocalFile(anyString(), anyString())).thenThrow(new RuntimeException());
        objectStorageFacade.loadMetaData(BUCKET_NAME, OBJECT_ID);
    }

    @Test
    public void testDeleteObject_Success() {
        when(metaOperator.getObjectMeta(anyString(), anyString())).thenReturn(getObjectMetadata());
        objectStorageFacade.deleteObject(BUCKET_NAME, OBJECT_ID);
        verify(metaOperator, times(1)).deleteByObjectId(Arrays.asList(OBJECT_ID));
        verify(blockOperator, times(1)).deleteByObjectId(anyString());
        verify(localFileOperator, times(1)).deleteLocalFile(anyString(), anyString());
    }

    @Test
    public void testIsObjectExists_NotExists_ReturnFalse() {
        assertFalse(objectStorageFacade.isObjectExists("whatever-bucket", "whatever-object"));
    }

    private ObjectMetadata getObjectMetadata() {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setObjectId(OBJECT_ID);
        metadata.setBucketName(BUCKET_NAME);
        metadata.setObjectName(OBJECT_NAME);
        metadata.setTotalLength(1L);
        metadata.setSha1("sha1");
        return metadata;
    }
}
