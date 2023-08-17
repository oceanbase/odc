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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.metadb.objectstorage.BucketEntity;
import com.oceanbase.odc.metadb.objectstorage.BucketRepository;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.objectstorage.model.Bucket;
import com.oceanbase.odc.service.objectstorage.operator.BucketOperator;

public class BucketOperatorTest extends ServiceTestEnv {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private BucketRepository bucketRepository;

    @Mock
    private AuthenticationFacade authenticationFacade;

    @InjectMocks
    private BucketOperator bucketOperation;

    @Before
    public void setUp() {
        bucketRepository.deleteAll();
        Mockito.when(authenticationFacade.currentUserId()).thenReturn(1L);
    }

    @After
    public void tearDown() {
        bucketRepository.deleteAll();
    }

    @Test
    public void testCreateBucket_IllegalName_ThrowBadArgumentException() {
        thrown.expect(BadArgumentException.class);
        bucketOperation.createBucket("te_st");
        verify(bucketRepository, times(0)).findByName(anyString());
    }

    @Test
    public void testCreateBucket_OnWindows_Success() {
        when(bucketRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(bucketRepository.saveAndFlush(any())).thenReturn(new BucketEntity());
        Bucket windowsBucket = bucketOperation.createBucket("script" + "\\" + "0");
        assertNotNull(windowsBucket);
        verify(bucketRepository, times(1)).findByName(anyString());
        verify(bucketRepository, times(1)).saveAndFlush(any());
    }

    @Test
    public void testCreateBucket_OnUnix_Success() {
        when(bucketRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(bucketRepository.saveAndFlush(any())).thenReturn(new BucketEntity());
        Bucket unixBucket = bucketOperation.createBucket("script" + "/" + "0");
        assertNotNull(unixBucket);
        verify(bucketRepository, times(1)).findByName(anyString());
        verify(bucketRepository, times(1)).saveAndFlush(any());
    }

    @Test
    public void testCreateBucket_BucketExists_ThrowBadRequestException() {
        when(bucketRepository.findByName(anyString())).thenReturn(Optional.of(new BucketEntity()));
        thrown.expect(BadRequestException.class);
        bucketOperation.createBucket("te-st");
        verify(bucketRepository, times(1)).findByName(anyString());
    }

    @Test
    public void testCreateBucket_Success() {
        when(bucketRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(bucketRepository.saveAndFlush(any())).thenReturn(new BucketEntity());
        Bucket ossBucket = bucketOperation.createBucket("te-st");
        assertNotNull(ossBucket);
        verify(bucketRepository, times(1)).findByName(anyString());
        verify(bucketRepository, times(1)).saveAndFlush(any());
    }

    @Test
    public void testGetBucketByName_NotExists_ReturnFalse() {
        when(bucketRepository.findByName(anyString())).thenReturn(Optional.empty());
        Optional<Bucket> ossBucketOpt = bucketOperation.getBucketByName("whatever");
        assertNotNull(ossBucketOpt);
        assertFalse(ossBucketOpt.isPresent());
    }

    @Test
    public void testGetBucketByName_Success() {
        when(bucketRepository.findByName(anyString())).thenReturn(Optional.of(new BucketEntity()));
        Optional<Bucket> ossBucketOpt = bucketOperation.getBucketByName("whatever");
        assertNotNull(ossBucketOpt);
        assertTrue(ossBucketOpt.isPresent());
    }

    @Test
    public void testListBucket_Success() {
        when(bucketRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(Collections.emptyList()));
        Page<Bucket> bucketPage = bucketOperation.listBucket(Pageable.unpaged());
        assertNotNull(bucketPage);
        assertTrue(bucketPage.isEmpty());
    }

    @Test
    public void testDeleteBucketByName_Success() {
        bucketOperation.deleteBucketByName("whatever");
        verify(bucketRepository, times(1)).deleteByName(anyString());
    }

    @Test
    public void testIsBucketExist_ReturnFalse() {
        boolean exists = bucketOperation.isBucketExist("whatever");
        verify(bucketRepository, times(1)).findByName(anyString());
        assertFalse(exists);
    }

    @Test
    public void testIsBucketExist_ReturnTrue() {
        when(bucketRepository.findByName(anyString())).thenReturn(Optional.of(new BucketEntity()));
        boolean exists = bucketOperation.isBucketExist("whatever");
        verify(bucketRepository, times(1)).findByName(anyString());
        assertTrue(exists);
    }

}
