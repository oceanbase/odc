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

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.service.connection.model.ConnectionLabel;
import com.oceanbase.odc.test.tool.TestRandom;

public class ConnectionLabelDAOTest extends ServiceTestEnv {

    @Autowired
    private ConnectionLabelDAO labelDAO;

    @Test
    public void insert_insertLabel_insertSucceed() {
        ConnectionLabel label = TestRandom.nextObject(ConnectionLabel.class);
        long actual = labelDAO.insert(label);
        Assert.assertEquals(1, actual);
    }

    @Test
    public void list_listLabels_listSucceed() {
        ConnectionLabel label = TestRandom.nextObject(ConnectionLabel.class);
        labelDAO.insert(label);
        List<ConnectionLabel> actual = labelDAO.list(label.getUserId());
        List<ConnectionLabel> expect = Collections.singletonList(label);
        actual.forEach(connectionLabel -> {
            connectionLabel.setGmtCreated(null);
            connectionLabel.setGmtModified(null);
        });
        expect.forEach(connectionLabel -> {
            connectionLabel.setGmtCreated(null);
            connectionLabel.setGmtModified(null);
        });
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void update_updateLabel_updateSucceed() {
        ConnectionLabel label = TestRandom.nextObject(ConnectionLabel.class);
        labelDAO.insert(label);
        label.setLabelName("new name");
        label.setLabelColor("new color");
        labelDAO.update(label);
        ConnectionLabel actual = labelDAO.get(label.getId());
        label.setGmtCreated(null);
        label.setGmtModified(null);
        actual.setGmtCreated(null);
        actual.setGmtModified(null);
        Assert.assertEquals(label, actual);
    }

    @Test
    public void delete_deleteLabel_deleteSucceed() {
        ConnectionLabel label = TestRandom.nextObject(ConnectionLabel.class);
        labelDAO.insert(label);
        labelDAO.delete(label.getId());
        Assert.assertNull(labelDAO.get(label.getId()));
    }

    @Test
    public void get_getLabel_getSucceed() {
        ConnectionLabel label = TestRandom.nextObject(ConnectionLabel.class);
        labelDAO.insert(label);
        ConnectionLabel actual = labelDAO.get(label.getId());
        label.setGmtCreated(null);
        label.setGmtModified(null);
        actual.setGmtCreated(null);
        actual.setGmtModified(null);
        Assert.assertEquals(label, actual);
    }

}
