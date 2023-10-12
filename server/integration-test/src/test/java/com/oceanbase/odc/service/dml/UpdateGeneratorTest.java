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
import org.junit.Test;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.service.dml.model.DataModifyUnit;

/**
 * {@link UpdateGeneratorTest}
 *
 * @author yh263208
 * @date 2023-03-09 18:08
 * @since ODC_release_4.2.0
 */
public class UpdateGeneratorTest {

    @BeforeClass
    public static void setUp() {
        ConnectionSession connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        JdbcOperations jdbcOperations =
                connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        try {
            jdbcOperations.execute("drop table t_test_update_data_sql");
        } catch (Exception e) {
            // eat exp
        }
        jdbcOperations.execute(
                "create table t_test_update_data_sql(c1 int, c2 varchar(100), c3 int, c4 text, primary key (c1, c2))");
        jdbcOperations.execute("insert into t_test_update_data_sql values(1,'abc',10, null),(2,'bcd',10, null)");

        connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        jdbcOperations = connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        try {
            jdbcOperations.execute("drop table \"t_test_update_data_sql\";");
        } catch (Exception e) {
            // eat exp
        }
        jdbcOperations.execute(
                "create table \"t_test_update_data_sql\"(\"c1\" int, \"c2\" varchar(100), \"c3\" int, \"c4\" clob, C5 date);");
        jdbcOperations.execute("insert into \"t_test_update_data_sql\" values("
                + "1,'abc',10, null, to_date('2020-12-12', 'yyyy-mm-dd')), (1,'abc',10,null, to_date('2020-12-12', 'yyyy-mm-dd')) ;");

    }

    @AfterClass
    public static void clear() {
        ConnectionSession connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        JdbcOperations jdbcOperations =
                connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        jdbcOperations.execute("drop table t_test_update_data_sql");

        connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        jdbcOperations = connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        jdbcOperations.execute("drop table \"t_test_update_data_sql\";");
    }

    @Test
    public void generate_mysqlMode_generateSucceed() {
        ConnectionSession connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        List<DataModifyUnit> list = new ArrayList<>();
        DataModifyUnit unit = new DataModifyUnit();
        unit.setColumnName("c1");
        unit.setColumnType("int");
        unit.setOldData("1");
        unit.setNewData("100");
        unit.setTableName("t_test_update_data_sql");
        list.add(unit);

        unit = new DataModifyUnit();
        unit.setColumnName("c2");
        unit.setColumnType("varchar(100)");
        unit.setOldData("abc");
        unit.setNewData("test");
        unit.setTableName("t_test_update_data_sql");
        list.add(unit);

        // oldData == newData, expect skipped
        unit = new DataModifyUnit();
        unit.setColumnName("c3");
        unit.setColumnType("int");
        unit.setOldData("10");
        unit.setNewData("10");
        unit.setTableName("t_test_update_data_sql");
        list.add(unit);

        unit = new DataModifyUnit();
        unit.setColumnName("c4");
        unit.setColumnType("text");
        unit.setNewData("object@tmp_test.txt");
        unit.setTableName("t_test_update_data_sql");
        list.add(unit);
        String schema = ConnectionSessionUtil.getCurrentSchema(connectionSession);
        list.forEach(u -> u.setSchemaName(schema));

        MySQLDMLBuilder builder = new MySQLDMLBuilder(list, Collections.emptyList(), connectionSession, null);
        UpdateGenerator generator = new UpdateGenerator(builder, connectionSession);
        String expect = "update `" + schema
                + "`.`t_test_update_data_sql` set `c1` = 100, `c2` = 'test', `c4` = 'object@tmp_test.txt' where `c1`=1 and `c2`='abc';";
        Assert.assertEquals(expect, generator.generate());
    }

    @Test
    @Ignore("TODO: fix this test")
    public void generate_oracleMode_generateSucceed() {
        ConnectionSession connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        List<DataModifyUnit> list = new ArrayList<>();
        DataModifyUnit unit = new DataModifyUnit();
        unit.setColumnName("c1");
        unit.setColumnType("int");
        unit.setOldData("1");
        unit.setNewData("100");
        unit.setTableName("t_test_update_data_sql");
        list.add(unit);

        unit = new DataModifyUnit();
        unit.setColumnName("c2");
        unit.setColumnType("varchar(100)");
        unit.setOldData("abc");
        unit.setNewData("test");
        unit.setTableName("t_test_update_data_sql");
        list.add(unit);

        // newData was null, not skipped
        unit = new DataModifyUnit();
        unit.setColumnName("c3");
        unit.setColumnType("int");
        unit.setOldData("10");
        unit.setNewData(null);
        unit.setTableName("t_test_update_data_sql");
        list.add(unit);

        unit = new DataModifyUnit();
        unit.setColumnName("c4");
        unit.setColumnType("clob");
        unit.setNewData("object@tmp_test.txt");
        unit.setNewDataType("FILE");
        unit.setTableName("t_test_update_data_sql");
        list.add(unit);

        unit = new DataModifyUnit();
        unit.setColumnName("C5");
        unit.setColumnType("date");
        unit.setNewData("2022-07-11T20:04:31+08:00");
        unit.setOldData("2023-07-11T20:04:31+08:00");
        unit.setTableName("t_test_update_data_sql");
        list.add(unit);
        String schema = ConnectionSessionUtil.getCurrentSchema(connectionSession);
        list.forEach(u -> u.setSchemaName(schema));

        OracleDMLBuilder builder = new OracleDMLBuilder(list, Collections.emptyList(), connectionSession, null);
        UpdateGenerator generator = new UpdateGenerator(builder, connectionSession);
        String expect = "update \"" + schema
                + "\".\"t_test_update_data_sql\" set \"c1\" = 100, \"c2\" = 'test', \"c3\" = "
                + "null, \"c4\" = load_clob_file('object@tmp_test.txt'), "
                + "\"C5\" = to_date('2022-07-11 20:04:31', 'YYYY-MM-DD HH24:MI:SS') where "
                + "\"c1\"=1 and \"c2\"='abc' and \"c3\"=10 and "
                + "\"C5\"=to_date('2023-07-11 20:04:31', 'YYYY-MM-DD HH24:MI:SS');";
        Assert.assertEquals(expect, generator.generate());
        Assert.assertTrue(generator.isAffectMultiRows());
    }

}
