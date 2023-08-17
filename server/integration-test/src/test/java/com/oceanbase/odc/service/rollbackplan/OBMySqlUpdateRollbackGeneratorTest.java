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
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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
public class OBMySqlUpdateRollbackGeneratorTest {
    private static final String BASE_PATH = "src/test/resources/rollbackplan/obmysql/";
    private static final String ddl = TestUtils.loadAsString(BASE_PATH + "tableDdl.sql");
    private static final String dropTables = TestUtils.loadAsString(BASE_PATH + "drop.sql");
    private static ConnectionSession session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
    private static SyncJdbcExecutor jdbcExecutor =
            session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
    private static String SCHEMA_NAME = ConnectionSessionUtil.getCurrentSchema(session);
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @BeforeClass
    public static void setUp() {
        jdbcExecutor.execute(dropTables);
        jdbcExecutor.execute(ddl);
    }

    @AfterClass
    public static void clear() {
        jdbcExecutor.execute(dropTables);
    }

    @Test
    public void testUnsupportedSqlType_returnNull() {
        expectedException.expect(UnsupportedSqlTypeForRollbackPlanException.class);
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        String sql = "SELECT * FROM rollback_tab3;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);
    }

    @Test
    public void test_timeout() {
        expectedException.expect(UnsupportedSqlTypeForRollbackPlanException.class);
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        String sql = "SELECT * FROM rollback_tab3;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, 1L);
    }

    @Test
    public void testNoPriOrUqKey_returnErrMessage() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        String sql = "UPDATE rollback_noPriOrUqKey SET c2=2 WHERE c1=1;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);
        RollbackPlan actual = rollbackPlan.generate();
        RollbackPlan expect = new RollbackPlan(sql, DialectType.OB_MYSQL, null, null,
                "It is not supported to generate rollback plan for tables without primary key or unique key",
                0);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void testMultiUniqueKey_success() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(3);
        String sql = "UPDATE rollback_pri SET name = \"test\" WHERE id1 > 1;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        List<String> rollbackSql = new ArrayList<>();
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_pri` VALUES (2,21,2,2,2,2,'bb');");
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_pri` VALUES (3,31,3,3,3,3,'cc');");
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_pri` VALUES (4,41,4,4,4,4,'dd');");
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_pri` VALUES (5,51,5,5,5,5,'ee');");
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_pri` VALUES (6,61,6,6,6,6,'ff');");
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_pri` VALUES (7,71,7,7,7,7,'gg');");
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_pri` VALUES (8,81,8,8,8,8,'hh');");
        RollbackPlan expect =
                new RollbackPlan(sql, DialectType.OB_MYSQL,
                        Arrays.asList("SELECT rollback_pri.* FROM rollback_pri WHERE id1 > 1;"),
                        rollbackSql, null, 7);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void testChangeLineIsZero_returnErrMessage() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        String sql = "UPDATE rollback_tab2 SET c2 = 100 WHERE c1 > 100;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        RollbackPlan expect = new RollbackPlan(sql, DialectType.OB_MYSQL,
                Arrays.asList("SELECT rollback_tab2.* FROM rollback_tab2 WHERE c1 > 100;"), null,
                "The number of data change rows is 0", 0);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void testUnsupportedStatement_returnErrMessage() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        String sql = "UPDATE rollback_tab1, rollback_tab2 SET c2 = 100 WHERE rollback_tab1.c1 > 100;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        RollbackPlan expect = new RollbackPlan(sql, DialectType.OB_MYSQL, null, null,
                "Unsupported sql statement, missing table relation:c2 = 100",
                0);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void update_singleTableWithNameReference_success() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        String sql = "UPDATE rollback_tab2 PARTITION(p2) SET rollback_tab2.c2 = 100 WHERE rollback_tab2.c1 > 2;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        RollbackPlan expect = new RollbackPlan(sql, DialectType.OB_MYSQL,
                Arrays.asList("SELECT rollback_tab2.* FROM rollback_tab2 PARTITION(p2) WHERE rollback_tab2.c1 > 2;"),
                Arrays.asList("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_tab2` VALUES (3,3);"), null, 1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void update_singleTableUpdateAssignColumnWithoutTableName_success() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        String sql = "UPDATE rollback_tab2 PARTITION(p2) SET c2 = 100 WHERE c1 > 2;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);
        RollbackPlan actual = rollbackPlan.generate();
        RollbackPlan expect = new RollbackPlan(sql, DialectType.OB_MYSQL,
                Arrays.asList("SELECT rollback_tab2.* FROM rollback_tab2 PARTITION(p2) WHERE c1 > 2;"),
                Arrays.asList("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_tab2` VALUES (3,3);"), null, 1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void update_multiTables_success() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        String sql =
                "UPDATE rollback_tab1,rollback_tab2 PARTITION(p2) SET rollback_tab1.c2 = 100, rollback_tab2.c2 = 200 WHERE rollback_tab1.c2 = rollback_tab2.c2;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        List<String> rollbackSql = new ArrayList<>();
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_tab2` VALUES (1,1);");
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_tab2` VALUES (2,2);");
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_tab2` VALUES (3,3);");
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_tab1` VALUES (1,1);");
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_tab1` VALUES (2,2);");
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_tab1` VALUES (3,3);");
        RollbackPlan expect = new RollbackPlan(sql, DialectType.OB_MYSQL, Arrays.asList(
                "SELECT rollback_tab2.* FROM rollback_tab1,rollback_tab2 PARTITION(p2) WHERE rollback_tab1.c2 = rollback_tab2.c2;",
                "SELECT rollback_tab1.* FROM rollback_tab1,rollback_tab2 PARTITION(p2) WHERE rollback_tab1.c2 = rollback_tab2.c2;"),
                rollbackSql, null, 6);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void update_multiTablesWithRepeatedUpdateAssignTables_success() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        String sql =
                "UPDATE rollback_tab1,rollback_tab2 SET rollback_tab1.c2 = 100, rollback_tab2.c2 = 200, rollback_tab1.c1 = 200 WHERE rollback_tab1.c2 = rollback_tab2.c2;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        List<String> rollbackSql = new ArrayList<>();
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_tab2` VALUES (1,1);");
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_tab2` VALUES (2,2);");
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_tab2` VALUES (3,3);");
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_tab1` VALUES (1,1);");
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_tab1` VALUES (2,2);");
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_tab1` VALUES (3,3);");
        RollbackPlan expect = new RollbackPlan(sql, DialectType.OB_MYSQL, Arrays.asList(
                "SELECT rollback_tab2.* FROM rollback_tab1,rollback_tab2 WHERE rollback_tab1.c2 = rollback_tab2.c2;",
                "SELECT rollback_tab1.* FROM rollback_tab1,rollback_tab2 WHERE rollback_tab1.c2 = rollback_tab2.c2;"),
                rollbackSql, null, 6);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void update_multiWithAlias_success() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        String sql =
                "UPDATE rollback_tab1 as a, rollback_tab2 as b SET a.c2 = 100, b.c2 = 200, a.c1 = 200 WHERE a.c2 = b.c2;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        List<String> rollbackSql = new ArrayList<>();
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_tab2` VALUES (1,1);");
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_tab2` VALUES (2,2);");
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_tab2` VALUES (3,3);");
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_tab1` VALUES (1,1);");
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_tab1` VALUES (2,2);");
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_tab1` VALUES (3,3);");
        RollbackPlan expect = new RollbackPlan(sql, DialectType.OB_MYSQL, Arrays.asList(
                "SELECT b.* FROM rollback_tab1 as a,rollback_tab2 as b WHERE a.c2 = b.c2;",
                "SELECT a.* FROM rollback_tab1 as a,rollback_tab2 as b WHERE a.c2 = b.c2;"),
                rollbackSql, null, 6);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void update_WithJoinReferenceAndAlias_success() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        String sql =
                "UPDATE rollback_tab1 v1 inner join rollback_tab2 v2 inner join rollback_tab4 v3 on v1.c1 = v2.c1 and v1.c1=v3.c1 SET v1.c2=200;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        List<String> rollbackSql = new ArrayList<>();
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_tab1` VALUES (1,1);");
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_tab1` VALUES (2,2);");
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_tab1` VALUES (3,3);");
        RollbackPlan expect = new RollbackPlan(sql, DialectType.OB_MYSQL, Arrays.asList(
                "SELECT v1.* FROM rollback_tab1 v1 inner join rollback_tab2 v2 inner join rollback_tab4 v3 on v1.c1 = v2.c1 and v1.c1=v3.c1;"),
                rollbackSql, null, 3);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void update_test_default_value_is_null_success() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(3);
        String sql =
                "UPDATE rollback_use_default_value set c3=100 where c1<3;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        List<String> rollbackSql = new ArrayList<>();
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_use_default_value` VALUES (1,null,0);");
        rollbackSql.add("REPLACE INTO `" + SCHEMA_NAME + "`.`rollback_use_default_value` VALUES (2,2,3);");
        RollbackPlan expect = new RollbackPlan(sql, DialectType.OB_MYSQL, Arrays.asList(
                "SELECT rollback_use_default_value.* FROM rollback_use_default_value WHERE c1<3;"),
                rollbackSql, null, 2);
        Assert.assertEquals(expect, actual);
    }
}
