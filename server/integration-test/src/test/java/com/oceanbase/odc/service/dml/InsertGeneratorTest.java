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
package com.oceanbase.odc.service.dml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.service.dml.model.DataModifyUnit;

/**
 * {@link InsertGeneratorTest}
 *
 * @author yh263208
 * @date 2023-03-09 19:43
 * @since ODC_release_4.2.0
 */
public class InsertGeneratorTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void setUp() {
        ConnectionSession connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        JdbcOperations jdbcOperations =
                connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        try {
            jdbcOperations.execute("drop table t_test_insert_data_sql");
        } catch (Exception e) {
            // eat exp
        }
        jdbcOperations.execute("create table t_test_insert_data_sql(c1 int, c2 varchar(100), c3 blob)");

        connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        jdbcOperations = connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        try {
            jdbcOperations.execute("drop table \"t_test_insert_data_sql\"");
        } catch (Exception e) {
            // eat exp
        }
        jdbcOperations.execute(
                "create table \"t_test_insert_data_sql\"(\"c1\" int, \"c2\" varchar(100), \"c3\" blob, \"c4\" date, \"c5\""
                        + " timestamp)");
    }

    @AfterClass
    public static void clear() {
        ConnectionSession connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        JdbcOperations jdbcOperations =
                connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        jdbcOperations.execute("drop table t_test_insert_data_sql");

        connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        jdbcOperations = connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        jdbcOperations.execute("drop table \"t_test_insert_data_sql\"");
    }

    @Test
    public void generate_mysqlMode_generateSucceed() {
        ConnectionSession connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        List<DataModifyUnit> list = new ArrayList<>();
        DataModifyUnit unit = new DataModifyUnit();
        unit.setColumnName("c1");
        unit.setColumnType("int");
        unit.setNewData("1");
        unit.setTableName("t_test_insert_data_sql");
        list.add(unit);

        unit = new DataModifyUnit();
        unit.setColumnName("c2");
        unit.setColumnType("varchar(100)");
        unit.setNewData("abc");
        unit.setTableName("t_test_insert_data_sql");
        list.add(unit);

        unit = new DataModifyUnit();
        unit.setColumnName("c3");
        unit.setColumnType("blob");
        unit.setNewData("object@tmp_test.jpg");
        unit.setNewDataType("FILE");
        unit.setTableName("t_test_insert_data_sql");
        list.add(unit);
        String schema = ConnectionSessionUtil.getCurrentSchema(connectionSession);
        list.forEach(u -> u.setSchemaName(schema));

        MySQLDMLBuilder builder = new MySQLDMLBuilder(list, Collections.emptyList(), connectionSession, null);
        InsertGenerator generator = new InsertGenerator(builder);
        String expect = "insert into `" + schema + "`.`t_test_insert_data_sql`(`c1`,`c2`,`c3`) values(1,'abc',load_file"
                + "('object@tmp_test.jpg'));";
        Assert.assertEquals(expect, generator.generate());
    }

    @Test
    public void generate_mysqlModel1_generateSucceed() {
        ConnectionSession connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        List<DataModifyUnit> list = new ArrayList<>();
        DataModifyUnit unit = new DataModifyUnit();
        unit.setColumnName("c1");
        unit.setColumnType("int");
        unit.setNewData("1");
        unit.setTableName("t_test_insert_data_sql");
        list.add(unit);

        // null value, skip in insert statement
        unit = new DataModifyUnit();
        unit.setColumnName("c2");
        unit.setColumnType("varchar(100)");
        unit.setNewData(null);
        unit.setTableName("t_test_insert_data_sql");
        list.add(unit);

        // empty value, include in insert statement
        unit = new DataModifyUnit();
        unit.setColumnName("c3");
        unit.setColumnType("timestamp");
        unit.setNewData("");
        unit.setTableName("t_test_insert_data_sql");
        list.add(unit);
        String schema = ConnectionSessionUtil.getCurrentSchema(connectionSession);
        list.forEach(u -> u.setSchemaName(schema));

        MySQLDMLBuilder builder = new MySQLDMLBuilder(list, Collections.emptyList(), connectionSession, null);
        InsertGenerator generator = new InsertGenerator(builder);
        String expect = "insert into `" + schema + "`.`t_test_insert_data_sql`(`c1`,`c2`,`c3`) values(1,NULL,'');";
        Assert.assertEquals(expect, generator.generate());
    }

    @Test
    @Ignore("TODO: fix this test")
    public void generate_oracleModel_generateSucceed() {
        ConnectionSession connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        List<DataModifyUnit> list = new ArrayList<>();
        DataModifyUnit unit = new DataModifyUnit();
        unit.setColumnName("c1");
        unit.setColumnType("int");
        unit.setNewData("1");
        unit.setTableName("t_test_insert_data_sql");
        list.add(unit);

        unit = new DataModifyUnit();
        unit.setColumnName("c2");
        unit.setColumnType("varchar(100)");
        unit.setNewData("abc");
        unit.setTableName("t_test_insert_data_sql");
        list.add(unit);

        unit = new DataModifyUnit();
        unit.setColumnName("c3");
        unit.setColumnType("blob");
        unit.setNewData("object@tmp_test.jpg");
        unit.setNewDataType("FILE");
        unit.setTableName("t_test_insert_data_sql");
        list.add(unit);

        unit = new DataModifyUnit();
        unit.setColumnName("c4");
        unit.setColumnType("date");
        unit.setNewData("2020-12-12T12:12:23+08:00");
        unit.setTableName("t_test_insert_data_sql");
        list.add(unit);

        unit = new DataModifyUnit();
        unit.setColumnName("c5");
        unit.setColumnType("timestamp");
        unit.setNewData("2023-07-11T20:04:31.008891234+08:00");
        unit.setTableName("t_test_insert_data_sql");
        list.add(unit);
        String schema = ConnectionSessionUtil.getCurrentSchema(connectionSession);
        list.forEach(u -> u.setSchemaName(schema));

        OracleDMLBuilder builder = new OracleDMLBuilder(list, Collections.emptyList(), connectionSession, null);
        InsertGenerator generator = new InsertGenerator(builder);
        String expect = "insert into \"" + schema
                + "\".\"t_test_insert_data_sql\"(\"c1\",\"c2\",\"c3\",\"c4\",\"c5\") values"
                + "(1,'abc',load_file('object@tmp_test.jpg'),"
                + "to_date('2020-12-12 12:12:23', 'YYYY-MM-DD HH24:MI:SS'),"
                + "to_timestamp('2023-07-11 20:04:31.008891234', 'YYYY-MM-DD HH24:MI:SS.FF'));";
        Assert.assertEquals(expect, generator.generate());
    }

    @Test
    @Ignore("TODO: fix this test")
    public void generate_oracleModelWithTimestampLTZ_generateSucceed() throws Exception {
        ConnectionSession connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        List<DataModifyUnit> list = new ArrayList<>();
        DataModifyUnit unit = new DataModifyUnit();
        unit.setColumnName("c1");
        unit.setColumnType("timestamp(6) with time zone");
        unit.setNewData("2023-07-11T20:04:31.008891234+08:00");
        unit.setTableName("t_test_insert_data_sql");
        list.add(unit);

        unit = new DataModifyUnit();
        unit.setColumnName("c2");
        unit.setColumnType("timestamp(6) with local time zone");
        unit.setNewData("2023-07-11T20:04:31.008891234+08:00");
        unit.setTableName("t_test_insert_data_sql");
        list.add(unit);

        unit = new DataModifyUnit();
        unit.setColumnName("c3");
        unit.setColumnType("timestamp(5)");
        unit.setNewData("2023-07-11T20:04:31.008891234+08:00");
        unit.setTableName("t_test_insert_data_sql");
        list.add(unit);
        String schema = ConnectionSessionUtil.getCurrentSchema(connectionSession);
        list.forEach(u -> u.setSchemaName(schema));

        OracleDMLBuilder builder = new OracleDMLBuilder(list, Collections.emptyList(), connectionSession, null);
        InsertGenerator generator = new InsertGenerator(builder);
        String expect = "insert into \"" + schema + "\".\"t_test_insert_data_sql\"(\"c1\",\"c2\",\"c3\") values"
                + "(to_timestamp_tz('2023-07-11 20:04:31.008891234 +08:00', 'YYYY-MM-DD HH24:MI:SS.FF TZH:TZM'),"
                + "to_timestamp('2023-07-11 20:04:31.008891234', 'YYYY-MM-DD HH24:MI:SS.FF'),"
                + "to_timestamp('2023-07-11 20:04:31.008891234', 'YYYY-MM-DD HH24:MI:SS.FF'));";
        Assert.assertEquals(expect, generator.generate());
    }

}
