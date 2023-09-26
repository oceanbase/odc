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
package com.oceanbase.odc.service.onlineschemachange;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeSqlType;
import com.oceanbase.odc.service.onlineschemachange.validator.OnlineSchemaChangeValidator;

public class OnlineSchemaChangeValidatorTest extends ServiceTestEnv {
    public String CREATE_STMT;
    public String ALTER_STMT;
    public String DROP_STMT;

    @Autowired
    private OnlineSchemaChangeValidator validService;
    @MockBean
    private ConnectionService connectionService;
    private ConnectionSession session;
    private ConnectionConfig config;


    @Before
    public void setUp() {
        String t1 = "T1_" + StringUtils.uuidNoHyphen();
        String t2 = "T2_" + StringUtils.uuidNoHyphen();

        CREATE_STMT = String.format("CREATE TABLE %s (ID INT PRIMARY KEY); CREATE TABLE %s (ID INT UNIQUE);", t1, t2);
        ALTER_STMT = String.format("ALTER TABLE %s ADD COLUMN C1 VARCHAR(20);", t1);
        DROP_STMT = String.format("DROP TABLE %s; DROP TABLE %s;", t1, t2);

        config = TestConnectionUtil.getTestConnectionConfig(ConnectType.OB_MYSQL);
        session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        Mockito.when(connectionService.getForConnectionSkipPermissionCheck(Mockito.anyLong())).thenReturn(config);

        session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY).execute(CREATE_STMT);
    }

    @After
    public void tearDown() {
        session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY).execute(DROP_STMT);
    }

    @Test
    @Ignore("TODO: fix this test")
    public void test_Validate_Create_Successfully() {
        validService.validate(getCreateRequest(
                CREATE_STMT,
                OnlineSchemaChangeSqlType.CREATE));
    }

    @Test(expected = BadArgumentException.class)
    public void test_Validate_Create_Failed() {
        validService.validate(getCreateRequest(
                CREATE_STMT,
                OnlineSchemaChangeSqlType.ALTER));
    }

    @Test
    @Ignore("TODO: fix this test")
    public void test_Validate_Alter_Successfully() {
        validService.validate(getCreateRequest(
                ALTER_STMT,
                OnlineSchemaChangeSqlType.ALTER));
    }

    @Test(expected = BadArgumentException.class)
    public void test_Validate_Alter_Failed() {
        validService.validate(getCreateRequest(
                ALTER_STMT,
                OnlineSchemaChangeSqlType.CREATE));
    }

    private CreateFlowInstanceReq getCreateRequest(String sql, OnlineSchemaChangeSqlType sqlType) {
        OnlineSchemaChangeParameters parameter = new OnlineSchemaChangeParameters();
        parameter.setSqlType(sqlType);
        parameter.setSqlContent(sql);
        parameter.setDelimiter(";");
        CreateFlowInstanceReq req = new CreateFlowInstanceReq();
        req.setParameters(parameter);
        req.setConnectionId(1L);
        req.setDatabaseName(config.defaultSchema());
        return req;
    }

}
