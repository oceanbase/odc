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

package com.oceanbase.odc.service.connection;

import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.metadb.connection.ConnectionHistoryEntity;
import com.oceanbase.odc.metadb.connection.ConnectionHistoryRepository;

/**
 * @author gaoda.xy
 * @date 2023/12/5 13:50
 */
public class ConnectionHistoryRepositoryTest extends ServiceTestEnv {

    @Autowired
    private ConnectionHistoryRepository connectionHistoryRepository;

    private static final long USER_ID = 11L;

    @Before
    public void setUp() {
        connectionHistoryRepository.deleteAll();
    }

    @Test
    public void test_findByLastAccessTimeAfter() {
        Date now = new Date();
        connectionHistoryRepository.updateOrInsert(1L, USER_ID, new Date(now.getTime() + 10 * 1000L));
        connectionHistoryRepository.updateOrInsert(2L, USER_ID, now);
        List<ConnectionHistoryEntity> entities = connectionHistoryRepository.findByLastAccessTimeAfter(now);
        Assert.assertEquals(1, entities.size());
    }

    @Test
    public void test_updateOrInsert_no_duplicate_key() {
        Date now = new Date();
        connectionHistoryRepository.updateOrInsert(1L, 1L, now);
        connectionHistoryRepository.updateOrInsert(1L, 2L, now);
        connectionHistoryRepository.updateOrInsert(2L, 1L, now);
        Assert.assertEquals(3, connectionHistoryRepository.findAll().size());
    }

    @Test
    public void test_updateOrInsert_with_duplicate_key() {
        Date now = new Date();
        connectionHistoryRepository.updateOrInsert(1L, 1L, new Date(now.getTime() - 1000));
        connectionHistoryRepository.updateOrInsert(1L, 1L, now);
        connectionHistoryRepository.updateOrInsert(2L, 1L, now);
        Assert.assertEquals(2, connectionHistoryRepository.findAll().size());
    }

    @Test
    public void test_deleteByConnectionId() {
        connectionHistoryRepository.updateOrInsert(1L, USER_ID, new Date());
        connectionHistoryRepository.updateOrInsert(2L, USER_ID, new Date());
        Assert.assertEquals(2, connectionHistoryRepository.findAll().size());
        connectionHistoryRepository.deleteByConnectionId(1L);
        Assert.assertEquals(1, connectionHistoryRepository.findAll().size());
    }

}
