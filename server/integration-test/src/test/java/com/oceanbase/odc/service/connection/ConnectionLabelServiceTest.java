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

import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.service.connection.model.ConnectionLabel;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

public class ConnectionLabelServiceTest extends ServiceTestEnv {
    private static final long USER_ID = 1L;

    @Autowired
    private ConnectionLabelService connectionLabelService;
    @MockBean
    private AuthenticationFacade authenticationFacade;

    @Before
    public void setUp() throws Exception {
        when(authenticationFacade.currentUserId()).thenReturn(USER_ID);
    }

    @Test
    public void testCreateSessionLabel() {
        ConnectionLabel connectionLabel = new ConnectionLabel();
        connectionLabel.setUserId(100);
        connectionLabel.setLabelName("test");
        connectionLabel.setLabelColor("red");
        ConnectionLabel result = this.connectionLabelService.create(connectionLabel);
        this.connectionLabelService.delete(result.getId());
    }

    @Test
    public void testQuerySessionLabel() {
        List<ConnectionLabel> resultList = this.connectionLabelService.list();
        Assert.assertEquals(0, resultList.size());

        ConnectionLabel connectionLabel = new ConnectionLabel();
        connectionLabel.setLabelName("test");
        connectionLabel.setLabelColor("red");
        this.connectionLabelService.create(connectionLabel);

        resultList = this.connectionLabelService.list();
        Assert.assertEquals(1, resultList.size());
        this.connectionLabelService.delete(resultList.get(0).getId());
    }

    @Test
    public void testUpdateSessionLabel() {
        ConnectionLabel connectionLabel = new ConnectionLabel();
        connectionLabel.setLabelName("test");
        connectionLabel.setLabelColor("red");
        this.connectionLabelService.create(connectionLabel);

        List<ConnectionLabel> resultList = this.connectionLabelService.list();
        Assert.assertEquals(1, resultList.size());

        connectionLabel = resultList.get(0);
        connectionLabel.setLabelName("test1");
        connectionLabel.setLabelColor("green");
        this.connectionLabelService.update(connectionLabel);

        resultList = this.connectionLabelService.list();
        Assert.assertEquals(1, resultList.size());
        Assert.assertEquals("test1", resultList.get(0).getLabelName());
        this.connectionLabelService.delete(resultList.get(0).getId());
    }

    @Test
    public void testDeleteSessionLabel() {
        ConnectionLabel connectionLabel = new ConnectionLabel();
        connectionLabel.setLabelName("test");
        connectionLabel.setLabelColor("red");
        this.connectionLabelService.create(connectionLabel);

        List<ConnectionLabel> resultList = this.connectionLabelService.list();
        Assert.assertEquals(1, resultList.size());

        this.connectionLabelService.delete(resultList.get(0).getId());

        resultList = this.connectionLabelService.list();
        Assert.assertEquals(0, resultList.size());
    }

}
