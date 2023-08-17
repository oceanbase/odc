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
public class OBOracleDeleteRollbackGeneratorTest {
    private static final String BASE_PATH = "src/test/resources/rollbackplan/oboracle/";
    private static final String ddl = TestUtils.loadAsString(BASE_PATH + "tableDdl.sql");
    private static final String dropTables = TestUtils.loadAsString(BASE_PATH + "drop.sql");
    private static ConnectionSession session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
    private static SyncJdbcExecutor jdbcExecutor =
            session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
    private static String SCHEMA_NAME = ConnectionSessionUtil.getCurrentSchema(session);
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
    public void delete_singleTable_success() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        properties.setDefaultTimeZone("Asia/Shanghai");
        String sql = "DELETE FROM \"ROLLBACK_TAB1\" WHERE C1=1;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        List<String> rollbackSql = new ArrayList<>();
        rollbackSql.add("INSERT INTO \"" + SCHEMA_NAME + "\".\"ROLLBACK_TAB1\" VALUES (1,1);");
        RollbackPlan expect =
                new RollbackPlan(sql, DialectType.OB_ORACLE,
                        Arrays.asList("SELECT \"ROLLBACK_TAB1\".* FROM \"ROLLBACK_TAB1\" WHERE C1=1;"),
                        rollbackSql, null, 1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void delete_WithSubqueryFromSingleTable_success() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        properties.setDefaultTimeZone("Asia/Shanghai");
        String sql = "DELETE FROM (SELECT * FROM ROLLBACK_TAB1 WHERE ROLLBACK_TAB1.C1>0) V WHERE V.C2>1;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        List<String> rollbackSql = new ArrayList<>();
        rollbackSql.add("INSERT INTO (SELECT * FROM ROLLBACK_TAB1 WHERE ROLLBACK_TAB1.C1>0) V VALUES (2,2);");
        rollbackSql.add("INSERT INTO (SELECT * FROM ROLLBACK_TAB1 WHERE ROLLBACK_TAB1.C1>0) V VALUES (3,3);");
        rollbackSql.add("INSERT INTO (SELECT * FROM ROLLBACK_TAB1 WHERE ROLLBACK_TAB1.C1>0) V VALUES (4,4);");
        rollbackSql.add("INSERT INTO (SELECT * FROM ROLLBACK_TAB1 WHERE ROLLBACK_TAB1.C1>0) V VALUES (5,5);");
        RollbackPlan expect = new RollbackPlan(sql, DialectType.OB_ORACLE,
                Arrays.asList(
                        "SELECT V.* FROM (SELECT * FROM ROLLBACK_TAB1 WHERE ROLLBACK_TAB1.C1>0) V WHERE V.C2>1;"),
                rollbackSql, null, 4);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void delete_WithSubqueryFromMultiTable_returnErrMessage() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        properties.setDefaultTimeZone("Asia/Shanghai");
        String sql =
                "DELETE FROM (SELECT \"ROLLBACK_TAB1\".* FROM ROLLBACK_TAB1, ROLLBACK_TAB1 WHERE ROLLBACK_TAB1.C1=ROLLBACK_TAB1.C1) V WHERE V.C2=4;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        RollbackPlan expect = new RollbackPlan(sql, DialectType.OB_ORACLE,
                null, null,
                "Does not support generating rollback plan for subquery SQL statements involving multi-table queries",
                0);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void delete_singleTableWithReturning_success() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        properties.setDefaultTimeZone("Asia/Shanghai");
        String sql = "DELETE FROM ROLLBACK_TAB1 WHERE C1=1 RETURNING C2;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        List<String> rollbackSql = new ArrayList<>();
        rollbackSql.add("INSERT INTO \"" + SCHEMA_NAME + "\".\"ROLLBACK_TAB1\" VALUES (1,1);");
        RollbackPlan expect =
                new RollbackPlan(sql, DialectType.OB_ORACLE,
                        Arrays.asList("SELECT ROLLBACK_TAB1.* FROM ROLLBACK_TAB1 WHERE C1=1;"),
                        rollbackSql, null, 1);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void testUnsupportedSqlType_returnErrMessage() {
        expectedException.expect(UnsupportedSqlTypeForRollbackPlanException.class);
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        properties.setDefaultTimeZone("Asia/Shanghai");
        String sql = "SELECT * FROM ROLLBACK_TAB1;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);
    }

    @Test
    public void testUnsupportedColumnType_returnErrMessage() {
        RollbackProperties properties = new RollbackProperties();
        properties.setEachSqlMaxChangeLines(100000);
        properties.setQueryDataBatchSize(2);
        properties.setDefaultTimeZone("Asia/Shanghai");
        String sql = "DELETE FROM ROLLBACK_UNSUPPORTED_TYPE;";
        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, properties, session, null);

        RollbackPlan actual = rollbackPlan.generate();
        RollbackPlan expect = new RollbackPlan(sql, DialectType.OB_ORACLE,
                Arrays.asList("SELECT ROLLBACK_UNSUPPORTED_TYPE.* FROM ROLLBACK_UNSUPPORTED_TYPE;"),
                null,
                "Failed to get rollback sql, error message = java.lang.UnsupportedOperationException: Unsupported column type for generating rollback sql, column type : RAW",
                0);
        Assert.assertEquals(expect, actual);
    }
}
