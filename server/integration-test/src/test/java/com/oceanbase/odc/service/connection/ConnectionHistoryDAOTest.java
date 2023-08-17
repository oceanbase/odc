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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.metadb.connection.ConnectionHistoryDAO;
import com.oceanbase.odc.metadb.connection.ConnectionHistoryEntity;

/**
 * @Author: Lebie
 * @Date: 2022/1/6 下午8:15
 * @Description: []
 */
public class ConnectionHistoryDAOTest extends ServiceTestEnv {
    @Autowired
    private ConnectionHistoryDAO connectionHistoryDAO;

    private final Date oldDate = new Date(1000L);

    private final Date newDate = new Date(2000L);


    @Before
    public void setUp() {
        connectionHistoryDAO.deleteAll();
    }

    @After
    public void clear() {
        connectionHistoryDAO.deleteAll();
    }

    @Test
    public void testUpdateOrInsert_InsertNewRecord() {
        connectionHistoryDAO.updateOrInsert(ConnectionHistoryEntity.of(1L, 1L, oldDate));
        List<ConnectionHistoryEntity> entitiesAfterInsert = connectionHistoryDAO.listAll();
        connectionHistoryDAO.deleteAll();
        Assert.assertTrue(
                entitiesAfterInsert.size() == 1 && oldDate.equals(entitiesAfterInsert.get(0).getLastAccessTime()));
    }

    @Test
    public void testUpdateOrInsert_UpdateRecord() {
        connectionHistoryDAO.updateOrInsert(ConnectionHistoryEntity.of(1L, 1L, oldDate));
        List<ConnectionHistoryEntity> entitiesAfterInsert = connectionHistoryDAO.listAll();

        connectionHistoryDAO.updateOrInsert(ConnectionHistoryEntity.of(1L, 1L, newDate));
        List<ConnectionHistoryEntity> entitiesAfterUpdate = connectionHistoryDAO.listAll();

        connectionHistoryDAO.deleteAll();
        Assert.assertTrue(entitiesAfterUpdate.size() == 1);
        Assert.assertNotEquals(entitiesAfterInsert.get(0).getLastAccessTime().getTime(),
                entitiesAfterUpdate.get(0).getLastAccessTime().getTime());
    }
}
