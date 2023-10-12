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
package com.oceanbase.odc.service.onlineschemachange.subtask;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.onlineschemachange.ddl.DdlConstants;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeSqlType;

public class SubTaskParameterGeneratorTest {
    private static final String CREATE_SQL_CONTENT = "CREATE TABLE T1 (ID INT); CREATE TABLE T2 (ID INT);";
    private static final String ALTER_SQL_CONTENT = "ALTER TABLE T1 ADD CONSTRAINT CONSTRAINT_T1_ID UNIQUE (ID);"
            + "ALTER TABLE T1 ADD CONSTRAINT CONSTRAINT_T1_NAME UNIQUE (NAME);";
    private ConnectionSession session;
    private JdbcOperations jdbcTemplate;

    @Before
    public void setUp() {
        session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        jdbcTemplate = session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        jdbcTemplate.execute(CREATE_SQL_CONTENT);
    }

    @After
    public void tearDown() {
        jdbcTemplate.execute("DROP TABLE T1");
        jdbcTemplate.execute("DROP TABLE T2");
    }

    @Test
    public void test_CreateTable() {
        List<OnlineSchemaChangeScheduleTaskParameters> subTaskParameters =
                mockOSCParameter(CREATE_SQL_CONTENT, OnlineSchemaChangeSqlType.CREATE).generateSubTaskParameters(
                        getTestConnectionConfig(), ConnectionSessionUtil.getCurrentSchema(session));
        Assert.assertEquals(2, subTaskParameters.size());
        subTaskParameters.forEach(
                param -> Assert
                        .assertTrue(param.getNewTableName().endsWith(DdlConstants.NEW_TABLE_NAME_SUFFIX_OB_ORACLE)));
    }

    @Test
    public void test_AlterTable() {
        List<OnlineSchemaChangeScheduleTaskParameters> subTaskParameters =
                mockOSCParameter(ALTER_SQL_CONTENT, OnlineSchemaChangeSqlType.ALTER).generateSubTaskParameters(
                        getTestConnectionConfig(), ConnectionSessionUtil.getCurrentSchema(session));
        Assert.assertEquals(1, subTaskParameters.size());
        Assert.assertEquals(2, subTaskParameters.get(0).getSqlsToBeExecuted().size());
    }

    private ConnectionConfig getTestConnectionConfig() {
        return (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(session);
    }

    private OnlineSchemaChangeParameters mockOSCParameter(String sqlContent, OnlineSchemaChangeSqlType ddlType) {
        OnlineSchemaChangeParameters parameter = new OnlineSchemaChangeParameters();
        parameter.setSqlContent(sqlContent);
        parameter.setDelimiter(";");
        parameter.setSqlType(ddlType);
        return parameter;
    }

}
