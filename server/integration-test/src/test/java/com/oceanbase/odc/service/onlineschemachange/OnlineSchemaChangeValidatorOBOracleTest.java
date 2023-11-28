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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeSqlType;
import com.oceanbase.odc.service.onlineschemachange.validator.OnlineSchemaChangeValidator;

public class OnlineSchemaChangeValidatorOBOracleTest extends ServiceTestEnv {

    @Autowired
    private OnlineSchemaChangeValidator validService;
    @MockBean
    private ConnectionService connectionService;
    private ConnectionConfig config;
    private SyncJdbcExecutor obMySqlSyncJdbcExecutor;


    @Before
    public void setUp() {

        config = TestConnectionUtil.getTestConnectionConfig(ConnectType.OB_ORACLE);
        ConnectionSession session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        Mockito.when(connectionService.getForConnectionSkipPermissionCheck(Mockito.anyLong())).thenReturn(config);
        obMySqlSyncJdbcExecutor = session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
    }


    @Test
    @Ignore("TODO: fix this test")
    public void TestUniqueNotNullOBOracle_Successfully() {
        String createSql = "CREATE TABLE NOT_NULL_UNIQUE_KEY (\n"
                + "col number NOT NULL,\n"
                + "col1 number DEFAULT NULL,\n"
                + "CONSTRAINT u1 UNIQUE (col)\n"
                + ")";
        obMySqlSyncJdbcExecutor.execute(createSql);
        try {
            validService.validate(getCreateRequest(
                    createSql,
                    OnlineSchemaChangeSqlType.CREATE));
        } finally {
            obMySqlSyncJdbcExecutor.execute("DROP TABLE NOT_NULL_UNIQUE_KEY");
        }

    }

    @Test(expected = UnsupportedException.class)
    @Ignore("TODO: fix this test")
    public void TestUniqueContainNotNullOBOracle_Failed() {
        String createSql = "CREATE TABLE NOT_NULL_UNIQUE_KEY2 (\n"
                + "col number NOT NULL,\n"
                + "col1 number DEFAULT NULL,\n"
                + "CONSTRAINT u1 UNIQUE (col,col1)\n"
                + ")";
        obMySqlSyncJdbcExecutor.execute(createSql);
        try {
            validService.validate(getCreateRequest(
                    createSql,
                    OnlineSchemaChangeSqlType.CREATE));
        } finally {
            obMySqlSyncJdbcExecutor.execute("DROP TABLE NOT_NULL_UNIQUE_KEY2");
        }

    }

    @Test(expected = UnsupportedException.class)
    @Ignore("TODO: fix this test")
    public void TestUniqueColumnNotNullOBOracle_Failed() {
        String createSql = "CREATE TABLE NOT_NULL_UNIQUE_KEY3 (\n"
                + "col number NOT NULL,\n"
                + "col1 number DEFAULT NULL,\n"
                + "CONSTRAINT u1 UNIQUE (col1)\n"
                + ")";
        obMySqlSyncJdbcExecutor.execute(createSql);
        try {
            validService.validate(getCreateRequest(
                    createSql,
                    OnlineSchemaChangeSqlType.CREATE));
        } finally {
            obMySqlSyncJdbcExecutor.execute("DROP TABLE NOT_NULL_UNIQUE_KEY3");
        }

    }


    private CreateFlowInstanceReq getCreateRequest(String sql, OnlineSchemaChangeSqlType sqlType) {
        OnlineSchemaChangeParameters parameter = new OnlineSchemaChangeParameters();
        parameter.setSqlType(sqlType);
        parameter.setSqlContent(sql);
        parameter.setDelimiter(";");
        CreateFlowInstanceReq req = new CreateFlowInstanceReq();
        req.setParameters(parameter);
        req.setConnectionId(1L);
        req.setDatabaseName(config.getDefaultSchema());
        return req;
    }

}
