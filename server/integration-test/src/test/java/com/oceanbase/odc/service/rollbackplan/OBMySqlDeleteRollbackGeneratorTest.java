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
public class OBMySqlDeleteRollbackGeneratorTest {
    private static final String BASE_PATH = "src/test/resources/rollbackplan/obmysql/";
    private static final String ddl = TestUtils.loadAsString(BASE_PATH + "tableDdl.sql");
    private static final String dropTables = TestUtils.loadAsString(BASE_PATH + "drop.sql");
    private static ConnectionSession session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
    private static SyncJdbcExecutor jdbcExecutor =
            session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
    private static String SCHEMA_NAME = ConnectionSessionUtil.getCurrentSchema(session);

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
    public void delete_singleTable_success() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        String sql = "DELETE FROM rollback_tab2 PARTITION(p2) WHERE c1>1 ORDER BY c2 LIMIT 2;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        List<String> rollbackSql = new ArrayList<>();
        rollbackSql.add("INSERT INTO `" + SCHEMA_NAME + "`.`rollback_tab2` VALUES (2,2);");
        rollbackSql.add("INSERT INTO `" + SCHEMA_NAME + "`.`rollback_tab2` VALUES (3,3);");
        RollbackPlan expect = new RollbackPlan(sql, DialectType.OB_MYSQL,
                Arrays.asList(
                        "SELECT rollback_tab2.* FROM rollback_tab2 PARTITION(p2) WHERE c1>1 ORDER BY c2 LIMIT 2;"),
                rollbackSql, null, 2);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void delete_multiTables_success() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        String sql =
                "DELETE rollback_tab1, rollback_tab2 FROM rollback_tab1, rollback_tab2 WHERE rollback_tab1.c1 = rollback_tab2.c1;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        List<String> rollbackSql = new ArrayList<>();
        rollbackSql.add("INSERT INTO `" + SCHEMA_NAME + "`.`rollback_tab2` VALUES (1,1);");
        rollbackSql.add("INSERT INTO `" + SCHEMA_NAME + "`.`rollback_tab2` VALUES (2,2);");
        rollbackSql.add("INSERT INTO `" + SCHEMA_NAME + "`.`rollback_tab2` VALUES (3,3);");
        rollbackSql.add("INSERT INTO `" + SCHEMA_NAME + "`.`rollback_tab1` VALUES (1,1);");
        rollbackSql.add("INSERT INTO `" + SCHEMA_NAME + "`.`rollback_tab1` VALUES (2,2);");
        rollbackSql.add("INSERT INTO `" + SCHEMA_NAME + "`.`rollback_tab1` VALUES (3,3);");

        RollbackPlan expect = new RollbackPlan(sql, DialectType.OB_MYSQL, Arrays.asList(
                "SELECT rollback_tab2.* FROM rollback_tab1, rollback_tab2 WHERE rollback_tab1.c1 = rollback_tab2.c1;",
                "SELECT rollback_tab1.* FROM rollback_tab1, rollback_tab2 WHERE rollback_tab1.c1 = rollback_tab2.c1;"),
                rollbackSql, null, 6);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void delete_multiTablesWithJoinReferenceAndAlias_success() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(3);
        String sql =
                "DELETE v1, v2 FROM rollback_tab1 v1 INNER JOIN rollback_tab2 v2 on v1.c1 = v2.c1 and v1.c2=v2.c2;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        List<String> rollbackSql = new ArrayList<>();
        rollbackSql.add("INSERT INTO `" + SCHEMA_NAME + "`.`rollback_tab2` VALUES (1,1);");
        rollbackSql.add("INSERT INTO `" + SCHEMA_NAME + "`.`rollback_tab2` VALUES (2,2);");
        rollbackSql.add("INSERT INTO `" + SCHEMA_NAME + "`.`rollback_tab2` VALUES (3,3);");
        rollbackSql.add("INSERT INTO `" + SCHEMA_NAME + "`.`rollback_tab1` VALUES (1,1);");
        rollbackSql.add("INSERT INTO `" + SCHEMA_NAME + "`.`rollback_tab1` VALUES (2,2);");
        rollbackSql.add("INSERT INTO `" + SCHEMA_NAME + "`.`rollback_tab1` VALUES (3,3);");

        RollbackPlan expect = new RollbackPlan(sql, DialectType.OB_MYSQL, Arrays.asList(
                "SELECT v2.* FROM rollback_tab1 v1 INNER JOIN rollback_tab2 v2 on v1.c1 = v2.c1 and v1.c2=v2.c2;",
                "SELECT v1.* FROM rollback_tab1 v1 INNER JOIN rollback_tab2 v2 on v1.c1 = v2.c1 and v1.c2=v2.c2;"),
                rollbackSql, null, 6);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void delete_multiTablesWithUsing_success() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        String sql = "DELETE FROM v1, v2 USING rollback_tab1 AS v1 INNER JOIN rollback_tab2 AS v2 WHERE v1.c1=v2.c1;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        List<String> rollbackSql = new ArrayList<>();
        rollbackSql.add("INSERT INTO `" + SCHEMA_NAME + "`.`rollback_tab2` VALUES (1,1);");
        rollbackSql.add("INSERT INTO `" + SCHEMA_NAME + "`.`rollback_tab2` VALUES (2,2);");
        rollbackSql.add("INSERT INTO `" + SCHEMA_NAME + "`.`rollback_tab2` VALUES (3,3);");
        rollbackSql.add("INSERT INTO `" + SCHEMA_NAME + "`.`rollback_tab1` VALUES (1,1);");
        rollbackSql.add("INSERT INTO `" + SCHEMA_NAME + "`.`rollback_tab1` VALUES (2,2);");
        rollbackSql.add("INSERT INTO `" + SCHEMA_NAME + "`.`rollback_tab1` VALUES (3,3);");

        RollbackPlan expect = new RollbackPlan(sql, DialectType.OB_MYSQL,
                Arrays.asList(
                        "SELECT v2.* FROM rollback_tab1 AS v1 INNER JOIN rollback_tab2 AS v2 WHERE v1.c1=v2.c1;",
                        "SELECT v1.* FROM rollback_tab1 AS v1 INNER JOIN rollback_tab2 AS v2 WHERE v1.c1=v2.c1;"),
                rollbackSql, null, 6);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void delete_differentTypesOfFields_success() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        String sql = "DELETE FROM rollback_tab3;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        List<String> rollbackSql = new ArrayList<>();
        rollbackSql.add("INSERT INTO `" + SCHEMA_NAME + "`.`rollback_tab3` VALUES (1,23,'aa','2000-01-09');");
        rollbackSql.add("INSERT INTO `" + SCHEMA_NAME + "`.`rollback_tab3` VALUES (2,24,'bb','1999-09-09');");
        rollbackSql.add("INSERT INTO `" + SCHEMA_NAME + "`.`rollback_tab3` VALUES (3,10,'cc','2013-06-25');");
        rollbackSql.add("INSERT INTO `" + SCHEMA_NAME + "`.`rollback_tab3` VALUES (4,30,'dd','1993-03-15');");
        RollbackPlan expect =
                new RollbackPlan(sql, DialectType.OB_MYSQL, Arrays.asList("SELECT rollback_tab3.* FROM rollback_tab3;"),
                        rollbackSql, null, 4);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void testUnsupportedColumnType_returnErrMessage() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        String sql = "DELETE FROM rollback_unsupportedType;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        RollbackPlan expect = new RollbackPlan(sql, DialectType.OB_MYSQL,
                Arrays.asList("SELECT rollback_unsupportedType.* FROM rollback_unsupportedType;"),
                null,
                "Failed to get rollback sql, error message = java.lang.UnsupportedOperationException: Unsupported column type for generating rollback sql, column type : LONGBLOB",
                0);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void test_changedLine_exceeds_max_value_returnErrMessage() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(5);
        properties.setQueryDataBatchSize(2);
        String sql = "delete a, b from rollback_tab1 as a inner join rollback_tab2 as b where a.c1=b.c1;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);
        RollbackPlan actual = rollbackPlan.generate();
        RollbackPlan expect = new RollbackPlan(sql, DialectType.OB_MYSQL,
                Arrays.asList("SELECT b.* FROM rollback_tab1 as a inner join rollback_tab2 as b WHERE a.c1=b.c1;",
                        "SELECT a.* FROM rollback_tab1 as a inner join rollback_tab2 as b WHERE a.c1=b.c1;"),
                null,
                "The number of changed lines exceeds 5",
                0);
        Assert.assertEquals(expect, actual);
    }
}
