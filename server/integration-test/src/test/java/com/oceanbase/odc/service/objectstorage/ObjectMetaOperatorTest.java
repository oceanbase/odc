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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.objectstorage.BucketEntity;
import com.oceanbase.odc.metadb.objectstorage.BucketRepository;
import com.oceanbase.odc.metadb.objectstorage.ObjectMetadataEntity;
import com.oceanbase.odc.metadb.objectstorage.ObjectMetadataRepository;
import com.oceanbase.odc.service.objectstorage.operator.ObjectMetaOperator;

public class ObjectMetaOperatorTest extends ServiceTestEnv {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private ObjectMetadataRepository metaRepository;

    @Mock
    private BucketRepository bucketRepository;

    @InjectMocks
    private ObjectMetaOperator metaOperation;

    @Before
    public void setUp() {
        metaRepository.deleteAll();
    }

    @After
    public void tearDown() {
        metaRepository.deleteAll();
    }

    @Test
    public void testInitSaving_BucketNotExist_ThrowNotFoundException() {
        when(bucketRepository.findByName("bucketName")).thenReturn(Optional.empty());
        thrown.expect(NotFoundException.class);
        metaOperation.initSaving("bucketName", 1L, "filename", "objectId", 10L, 10L);
        verify(bucketRepository, times(1)).findByName(anyString());
    }

    @Test
    public void testInitSaving_BucketExist_Success() {
        when(bucketRepository.findByName("bucketName")).thenReturn(Optional.of(new BucketEntity()));
        when(metaRepository.saveAndFlush(any())).thenReturn(new ObjectMetadataEntity());
        metaOperation.initSaving("bucketName", 1L, "fileName", "objectId", 10L, 10L);
        verify(bucketRepository, times(1)).findByName(anyString());
        verify(metaRepository, times(1)).findByObjectId(anyString());
        verify(metaRepository, times(1)).saveAndFlush(any(ObjectMetadataEntity.class));
    }

    @Test
    public void testGetObjectMeta_BucketNotExist_ThrowNotFoundException() {
        when(metaRepository.findByBucketNameAndObjectId(anyString(), anyString())).thenReturn(Optional.empty());
        thrown.expect(NotFoundException.class);
        metaOperation.getObjectMeta("bucketName", "objectId");
        verify(metaRepository, times(1)).findByBucketNameAndObjectId(anyString(), anyString());
    }

    @Test
    public void testGetObjectMeta_Success() {
        when(metaRepository.findByBucketNameAndObjectId(anyString(), anyString()))
                .thenReturn(Optional.of(new ObjectMetadataEntity()));
        metaOperation.getObjectMeta("bucketName", "objectId");
        verify(metaRepository, times(1)).findByBucketNameAndObjectId(anyString(), anyString());
    }

    @Test
    public void testDeleteObjectMeta_Success() {
        when(metaRepository.deleteAllByObjectIdIn(anyCollection()))
                .thenReturn(1);
        metaOperation.deleteByObjectId(Arrays.asList("objectId1, objectId2"));
        verify(metaRepository, times(1)).deleteAllByObjectIdIn(anyCollection());
    }
}
