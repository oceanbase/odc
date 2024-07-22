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
package com.oceanbase.odc.metadb.connection;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.service.db.schema.model.DBObjectSyncStatus;
import com.oceanbase.odc.test.tool.TestRandom;

/**
 * Test cases for {@link DatabaseRepository}
 *
 * @author yh263208
 * @date 2024-07-08 14:29
 * @since ODC_release_4.3.1
 */
public class DatabaseRepositoryTest extends ServiceTestEnv {

    @Autowired
    private DatabaseRepository databaseRepository;

    @Before
    public void setUp() {
        this.databaseRepository.deleteAll();
    }

    @Test
    public void setObjectSyncStatusByObjectLastSyncTimeBefore_noObjectMatched_setNothing() {
        Date syncTime = new Date(System.currentTimeMillis() - 86400);
        DatabaseEntity d1 = TestRandom.nextObject(DatabaseEntity.class);
        d1.setObjectLastSyncTime(syncTime);
        d1.setObjectSyncStatus(DBObjectSyncStatus.SYNCED);
        DatabaseEntity d2 = TestRandom.nextObject(DatabaseEntity.class);
        d2.setObjectLastSyncTime(syncTime);
        d2.setObjectSyncStatus(DBObjectSyncStatus.SYNCED);
        this.databaseRepository.saveAll(Arrays.asList(d1, d2));
        int affectRows = this.databaseRepository.setObjectSyncStatusByObjectSyncStatusAndObjectLastSyncTimeBefore(
                DBObjectSyncStatus.SYNCING, DBObjectSyncStatus.SYNCING,
                new Date(System.currentTimeMillis() - 86400 * 2));
        Assert.assertEquals(0, affectRows);
    }

    @Test
    public void setObjectSyncStatusByObjectLastSyncTimeBefore_oneObjectMatched_setSucceed() {
        DatabaseEntity d1 = TestRandom.nextObject(DatabaseEntity.class);
        d1.setObjectLastSyncTime(new Date());
        d1.setObjectSyncStatus(DBObjectSyncStatus.SYNCED);
        DatabaseEntity d2 = TestRandom.nextObject(DatabaseEntity.class);
        d2.setObjectLastSyncTime(new Date(System.currentTimeMillis() - 86400));
        d2.setObjectSyncStatus(DBObjectSyncStatus.SYNCED);
        this.databaseRepository.saveAll(Arrays.asList(d1, d2));
        this.databaseRepository.setObjectSyncStatusByObjectSyncStatusAndObjectLastSyncTimeBefore(
                DBObjectSyncStatus.INITIALIZED, DBObjectSyncStatus.SYNCED,
                new Date(System.currentTimeMillis() - 86400 / 2));
        Set<DBObjectSyncStatus> actual = this.databaseRepository.findAll().stream()
                .map(DatabaseEntity::getObjectSyncStatus).collect(Collectors.toSet());
        Set<DBObjectSyncStatus> expect = new HashSet<>(
                Arrays.asList(DBObjectSyncStatus.SYNCED, DBObjectSyncStatus.INITIALIZED));
        Assert.assertEquals(expect, actual);
    }

}
