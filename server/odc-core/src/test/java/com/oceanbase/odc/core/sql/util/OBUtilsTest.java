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
package com.oceanbase.odc.core.sql.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.model.SqlExecDetail;
import com.oceanbase.odc.core.sql.execute.model.SqlExecTime;
import com.oceanbase.odc.test.database.TestDBConfigurations;

public class OBUtilsTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private final DataSource dataSource =
            TestDBConfigurations.getInstance().getTestOBMysqlConfiguration().getDataSource();

    @Test
    public void extractTraceIdFromKeyValue_OB2277_Matched() {
        String keyValue = "trace_id:YB426451985D-0005D3191FDEEE9D-0-0";
        String traceId = OBUtils.extractTraceIdFromKeyValue(keyValue);
        Assert.assertEquals("YB426451985D-0005D3191FDEEE9D-0-0", traceId);
    }

    @Test
    public void extractTraceIdFromKeyValue_OB321_Matched() {
        String keyValue =
                "addr:{ip:\"127.0.0.1\", port:1000}, enqueue_ts:123892389329, trace_id:YB426451985D-0005D3191FDEEE9D-0-0";
        String traceId = OBUtils.extractTraceIdFromKeyValue(keyValue);
        Assert.assertEquals("YB426451985D-0005D3191FDEEE9D-0-0", traceId);
    }

    @Test
    public void extractTraceIdFromKeyValue_NoTraceId_Null() {
        String keyValue = "addr:{ip:\"127.0.0.1\", port:1000}, enqueue_ts:123892389329";
        String traceId = OBUtils.extractTraceIdFromKeyValue(keyValue);
        Assert.assertNull(traceId);
    }


    @Test
    public void parseObVersionComment_GetVersion_ReturnTrue1() {
        String keyValue =
                "OceanBase 1.4.79 (r196340-local-4f889a9d2386a3d7dbc3c14c3d67f34bdb584b05) (Built Apr 15 2021 05:10:33)";
        String version = OBUtils.parseObVersionComment(keyValue);
        Assert.assertEquals("1.4.79", version);
    }

    @Test
    public void parseObVersionComment_GetVersion_ReturnTrue2() throws Exception {
        String keyValue = " ";
        thrown.expect(java.lang.IllegalArgumentException.class);
        thrown.expectMessage("The validated character sequence is blank");
        String version = OBUtils.parseObVersionComment(keyValue);
    }

    @Test
    public void parseObVersionComment_GetVersion_ReturnTrue3() throws Exception {
        String keyValue = "abc";
        thrown.expect(java.lang.IllegalArgumentException.class);
        thrown.expectMessage("version_comment get failed");
        String version = OBUtils.parseObVersionComment(keyValue);
    }

    @Test
    public void parseObVersionComment_GetVersion_ReturnTrue4() throws Exception {
        String keyValue = "Oceanbase 123 234";
        thrown.expect(java.lang.IllegalArgumentException.class);
        thrown.expectMessage("version_comment get failed");
        String version = OBUtils.parseObVersionComment(keyValue);
    }

    @Test
    public void test_QueryExecuteDetailByTraceId() throws Exception {
        SqlExecDetail sqlExecTime;
        String sql = "select 'test' from dual";
        try (Connection connection = this.dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("set session ob_enable_trace_log='ON';");
                statement.execute(sql);
                try (ResultSet resultSet = statement.getResultSet()) {
                    String traceId = OBUtils.getLastTraceIdBefore400(statement);
                    sqlExecTime =
                            OBUtils.queryExecuteDetailByTraceId(statement, traceId, ConnectType.OB_MYSQL, "2.2.79");
                }

                try (ResultSet resultSet = statement.executeQuery(
                        "SELECT QUERY_SQL FROM OCEANBASE.GV$SQL_AUDIT WHERE TRACE_ID='" + sqlExecTime.getTraceId()
                                + "'")) {
                    Assert.assertTrue(resultSet.next());
                    Assert.assertEquals(sql, resultSet.getString("QUERY_SQL"));
                }
            }
        }
    }

    @Test
    public void test_GetLastExecuteDetails() throws Exception {
        SqlExecDetail sqlExecTime;
        String sql = "select 'test' from dual";
        try (Connection connection = this.dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("set session ob_enable_trace_log='ON';");
                statement.execute(sql);
                try (ResultSet resultSet = statement.getResultSet()) {
                    sqlExecTime = OBUtils.getLastExecuteDetailsAfter2277(statement, ConnectType.OB_MYSQL, "2.2.79");
                }
                try (ResultSet resultSet = statement.executeQuery(
                        "SELECT QUERY_SQL FROM OCEANBASE.GV$SQL_AUDIT WHERE TRACE_ID='" + sqlExecTime.getTraceId()
                                + "'")) {
                    Assert.assertTrue(resultSet.next());
                    Assert.assertEquals(sql, resultSet.getString("QUERY_SQL"));
                }
            }
        }
    }

    @Test
    public void test_getLastExecuteDetailsBefore400() throws Exception {
        SqlExecTime sqlExecTime;
        String sql = "select 'test' from dual";
        try (Connection connection = this.dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("set session ob_enable_trace_log='ON';");
                statement.execute(sql);
                try (ResultSet resultSet = statement.getResultSet()) {
                    sqlExecTime = OBUtils.getLastExecuteDetailsBefore400(statement, "3.2.3.0");
                }
                try (ResultSet resultSet = statement.executeQuery(
                        "SELECT QUERY_SQL FROM OCEANBASE.GV$SQL_AUDIT WHERE TRACE_ID='" + sqlExecTime.getTraceId()
                                + "'")) {
                    Assert.assertTrue(resultSet.next());
                    Assert.assertEquals(sql, resultSet.getString("QUERY_SQL"));
                }
            }
        }
    }

    @Test
    public void queryDBMSOutput_putSomething_querySucceed() throws SQLException {
        DataSource dataSource = TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource();
        String word = "Hello,world";
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("call dbms_output.enable(1000000)");
                statement.execute("declare begin dbms_output.put_line('" + word + "'); end;");
            }
            Assert.assertEquals(word, OBUtils.queryDBMSOutput(connection, null));
        }
    }

    @Test
    public void queryDBMSOutput_putNothing_queryNull() throws SQLException {
        DataSource dataSource = TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource();
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("call dbms_output.enable(1000000)");
                statement.execute("declare\nvar varchar2(64);\nbegin\nselect '1' into var from dual;end;");
            }
            Assert.assertNull(OBUtils.queryDBMSOutput(connection, null));
        }
    }

    @Test
    public void queryDBMSOutput_putLinesQueryFirstLine_querySucceed() throws SQLException {
        DataSource dataSource = TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource();
        String word = "Hello,world";
        String word1 = "Hello,world1";
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("call dbms_output.enable(1000000)");
                statement.execute("declare begin "
                        + "dbms_output.put_line('" + word + "');"
                        + "dbms_output.put_line('" + word1 + "');end;");
            }
            Assert.assertEquals(word, OBUtils.queryDBMSOutput(connection, 1));
        }
    }

    @Test
    public void queryDBMSOutput_putLinesQueryAllLines_querySucceed() throws SQLException {
        DataSource dataSource = TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource();
        String word = "Hello,world";
        String word1 = "Hello,world1";
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("call dbms_output.enable(1000000)");
                statement.execute("declare begin "
                        + "dbms_output.put_line('" + word + "');"
                        + "dbms_output.put_line('" + word1 + "');end;");
            }
            Assert.assertEquals(word + "\n" + word1, OBUtils.queryDBMSOutput(connection, 2));
        }
    }

}
