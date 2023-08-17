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
package com.oceanbase.odc.service.rollbackplan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.service.rollbackplan.model.RollbackPlan;
import com.oceanbase.odc.service.rollbackplan.model.RollbackProperties;

/**
 * @author jingtian
 * @date 2023/5/16
 * @since ODC_release_4.2.0
 */
public class OBOracleUpdateRollbackGeneratorTest {
    private final static String BASE_PATH = "src/test/resources/rollbackplan/oboracle/";
    private final static String ddl = TestUtils.loadAsString(BASE_PATH + "tableDdl.sql");
    private final static String dropTables = TestUtils.loadAsString(BASE_PATH + "drop.sql");
    private static ConnectionSession session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
    private static SyncJdbcExecutor jdbcExecutor =
            session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
    private static String SCHEMA_NAME = ConnectionSessionUtil.getCurrentSchema(session);

    @BeforeClass
    public static void setUp() {
        batchExcuteSql(dropTables);
        jdbcExecutor.execute(ddl);
    }

    @AfterClass
    public static void clear() {
        batchExcuteSql(dropTables);
    }

    private static void batchExcuteSql(String str) {
        for (String ddl : str.split("/")) {
            jdbcExecutor.execute(ddl);
        }
    }

    @Test
    public void update_singleTable_success() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        properties.setDefaultTimeZone("Asia/Shanghai");
        String sql = "UPDATE ROLLBACK_TAB2 PARTITION(p1,p2) SET ROLLBACK_TAB2.C2 = 100 WHERE ROLLBACK_TAB2.C1 = 5;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        List<String> rollbackSql = new ArrayList<>();
        rollbackSql.add("DELETE FROM ROLLBACK_TAB2 PARTITION(p1,p2) WHERE ROLLBACK_TAB2.C1 = 5;");
        rollbackSql.add("INSERT INTO \"" + SCHEMA_NAME + "\".\"ROLLBACK_TAB2\" VALUES (5,5);");
        RollbackPlan expect = new RollbackPlan(sql, DialectType.OB_ORACLE,
                Arrays.asList(
                        "SELECT ROLLBACK_TAB2.* FROM ROLLBACK_TAB2 PARTITION(p1,p2) WHERE ROLLBACK_TAB2.C1 = 5;"),
                rollbackSql, null, 1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void update_singleTableWithSubquery_success() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        properties.setDefaultTimeZone("Asia/Shanghai");
        String sql = "UPDATE (SELECT * FROM ROLLBACK_TAB1) V SET V.C2 = 10 WHERE V.C1 = 1;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        List<String> rollbackSql = new ArrayList<>();
        rollbackSql.add("DELETE FROM (SELECT * FROM ROLLBACK_TAB1) V WHERE V.C1 = 1;");
        rollbackSql.add("INSERT INTO (SELECT * FROM ROLLBACK_TAB1) V VALUES (1,1);");
        RollbackPlan expect = new RollbackPlan(sql, DialectType.OB_ORACLE,
                Arrays.asList("SELECT V.* FROM (SELECT * FROM ROLLBACK_TAB1) V WHERE V.C1 = 1;"), rollbackSql,
                null, 1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void update_singleTableWithAlias_success() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        properties.setDefaultTimeZone("Asia/Shanghai");
        String sql = "UPDATE ROLLBACK_TAB1 V SET V.C2 = 10 WHERE V.C1 = 1;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        List<String> rollbackSql = new ArrayList<>();
        rollbackSql.add("DELETE FROM ROLLBACK_TAB1 V WHERE V.C1 = 1;");
        rollbackSql.add("INSERT INTO \"" + SCHEMA_NAME + "\".\"ROLLBACK_TAB1\" VALUES (1,1);");
        RollbackPlan expect = new RollbackPlan(sql, DialectType.OB_ORACLE,
                Arrays.asList("SELECT V.* FROM ROLLBACK_TAB1 V WHERE V.C1 = 1;"), rollbackSql, null, 1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void update_updateAssignWithColumnList_success() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        properties.setDefaultTimeZone("Asia/Shanghai");
        String sql =
                "UPDATE ROLLBACK_TAB1 SET (C1,C2) = (SELECT ROLLBACK_TAB2.C1 from ROLLBACK_TAB2 where ROLLBACK_TAB2.C2=5);";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        List<String> rollbackSql = new ArrayList<>();
        rollbackSql.add("DELETE FROM ROLLBACK_TAB1;");
        rollbackSql.add("INSERT INTO \"" + SCHEMA_NAME + "\".\"ROLLBACK_TAB1\" VALUES (1,1);");
        rollbackSql.add("INSERT INTO \"" + SCHEMA_NAME + "\".\"ROLLBACK_TAB1\" VALUES (2,2);");
        rollbackSql.add("INSERT INTO \"" + SCHEMA_NAME + "\".\"ROLLBACK_TAB1\" VALUES (3,3);");
        rollbackSql.add("INSERT INTO \"" + SCHEMA_NAME + "\".\"ROLLBACK_TAB1\" VALUES (4,4);");
        rollbackSql.add("INSERT INTO \"" + SCHEMA_NAME + "\".\"ROLLBACK_TAB1\" VALUES (5,5);");
        RollbackPlan expect =
                new RollbackPlan(sql, DialectType.OB_ORACLE,
                        Arrays.asList("SELECT ROLLBACK_TAB1.* FROM ROLLBACK_TAB1;"), rollbackSql,
                        null, 5);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void update_testUnSupportedStmt_returnErrMessage() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        properties.setDefaultTimeZone("Asia/Shanghai");
        String sql = "UPDATE ROLLBACK_TAB1 SET ROW=VALUE_A;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        RollbackPlan expect = new RollbackPlan(sql, DialectType.OB_ORACLE, null, null,
                "Unsupported update statement, cannot generate rollback sql",
                0);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void update_test_default_value_is_null_success() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        properties.setDefaultTimeZone("Asia/Shanghai");
        String rowid =
                jdbcExecutor.queryForObject("select rowid from ROLLBACK_TEST_TIME_TYPE where ID=1;", String.class);
        String sql = "UPDATE ROLLBACK_TEST_TIME_TYPE SET COL_DATE=to_date('2023-06-04','yyyy-mm-dd') where \"ROWID\"='"
                + rowid + "'";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        String querySql =
                "SELECT ROLLBACK_TEST_TIME_TYPE.* FROM ROLLBACK_TEST_TIME_TYPE " + "WHERE \"ROWID\"='" + rowid + "';";
        List<String> rollbackSql = new ArrayList<>();
        rollbackSql.add("DELETE FROM ROLLBACK_TEST_TIME_TYPE WHERE \"ROWID\"='" + rowid + "';");
        rollbackSql.add("INSERT INTO \"" + SCHEMA_NAME
                + "\".\"ROLLBACK_TEST_TIME_TYPE\" VALUES (1,to_date('1970-01-28 11:01:01', 'YYYY-MM-DD HH24:MI:SS'),to_timestamp('1970-01-08 08:03:03', 'YYYY-MM-DD HH24:MI:SS'),to_timestamp_tz('1970-01-22 05:00:05 +00:00', 'YYYY-MM-DD HH24:MI:SS TZH:TZM'),NULL);");
        RollbackPlan expect =
                new RollbackPlan(sql, DialectType.OB_ORACLE, Collections.singletonList(querySql), rollbackSql, null, 1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void test_changed_lines_exceeds_max_value_returnErrMessage() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(3);
        properties.setQueryDataBatchSize(2);
        properties.setDefaultTimeZone("Asia/Shanghai");
        String sql = "UPDATE ROLLBACK_TAB1 SET C1=100;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        RollbackPlan expect = new RollbackPlan(sql, DialectType.OB_ORACLE,
                Arrays.asList("SELECT ROLLBACK_TAB1.* FROM ROLLBACK_TAB1;"), null,
                "The number of changed lines exceeds 3", 0);
        Assert.assertEquals(expect, actual);
    }
}
