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

package com.oceanbase.odc.service.dbstructurecompare.obmysql;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.dbstructurecompare.OdcSchemaStructureComparator;
import com.oceanbase.odc.service.dbstructurecompare.model.ComparisonResult;
import com.oceanbase.odc.service.dbstructurecompare.model.DBObjectComparisonResult;
import com.oceanbase.odc.service.rollbackplan.TestUtils;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

/**
 * @author jingtian
 * @date 2023/12/26
 * @since ODC_release_4.2.4
 */
public class OdcSchemaStructureComparatorTest {
    private static final String BASE_PATH = "src/test/resources/dbstructurecompare/";
    private static String sourceSchemaDdl = TestUtils.loadAsString(BASE_PATH + "source_schema_ddl.sql");
    private static String targetSchemaDdl = TestUtils.loadAsString(BASE_PATH + "target_schema_ddl.sql");
    private static final String sourceDrop = TestUtils.loadAsString(BASE_PATH + "source_drop.sql");
    private static final String targetDrop = TestUtils.loadAsString(BASE_PATH + "target_drop.sql");
    private static SyncJdbcExecutor jdbcExecutor =
            TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL)
                    .getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
    private final static String sourceSchemaName = generateSchemaName();
    private final static String targetSchemaName = generateSchemaName();
    private static ConnectionSession srcSession;
    private static ConnectionSession tgtSession;
    private static List<DBObjectComparisonResult> results;

    @BeforeClass
    public static void setUp() {
        jdbcExecutor.execute("create database `" + sourceSchemaName + "`");
        jdbcExecutor.execute("create database `" + targetSchemaName + "`");
        ConnectionConfig sourceConfig = TestConnectionUtil.getTestConnectionConfig(ConnectType.OB_MYSQL);
        sourceConfig.setDefaultSchema(sourceSchemaName);
        srcSession = new DefaultConnectSessionFactory(sourceConfig).generateSession();

        ConnectionConfig targetConfig = TestConnectionUtil.getTestConnectionConfig(ConnectType.OB_MYSQL);
        targetConfig.setDefaultSchema(targetSchemaName);
        tgtSession = new DefaultConnectSessionFactory(targetConfig).generateSession();

        SyncJdbcExecutor srcJdbcExecutor = srcSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
        SyncJdbcExecutor tgtJdbcExecutor = tgtSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
        srcJdbcExecutor.execute(sourceDrop);
        srcJdbcExecutor.execute(sourceSchemaDdl);
        tgtJdbcExecutor.execute(targetDrop);
        tgtJdbcExecutor.execute(targetSchemaDdl);

        OdcSchemaStructureComparator comparator = new OdcSchemaStructureComparator(srcSession, tgtSession);
        results = comparator.compareTables();
    }

    @AfterClass
    public static void clear() {
        jdbcExecutor.execute("drop database `" + sourceSchemaName + "`");
        jdbcExecutor.execute("drop database `" + targetSchemaName + "`");
        srcSession.expire();
        tgtSession.expire();
    }

    private static String generateSchemaName() {
        return "odc_db_compare_test_" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 15).toLowerCase();
    }

    @Test
    public void compareAllTables_create_by_dependency_order() {
        int dependentTableOrder1 = 0;
        int dependentTableOrder2 = 0;
        int foreignKeyTableOrder = 0;

        for (int i = 0; i < results.size(); i++) {
            if (results.get(i).getDbObjectName().equals("fk_dependency_only_in_source1")) {
                Assert.assertEquals(results.get(i).getComparisonResult(), ComparisonResult.ONLY_IN_SOURCE);
                Assert.assertNotNull(results.get(i).getSourceDdl());
                dependentTableOrder1 = i;
            } else if (results.get(i).getDbObjectName().equals("fk_dependency_only_in_source2")) {
                Assert.assertEquals(results.get(i).getComparisonResult(), ComparisonResult.ONLY_IN_SOURCE);
                Assert.assertNotNull(results.get(i).getSourceDdl());
                dependentTableOrder2 = i;
            } else if (results.get(i).getDbObjectName().equals("fk_only_in_source")) {
                Assert.assertEquals(results.get(i).getComparisonResult(), ComparisonResult.ONLY_IN_SOURCE);
                Assert.assertNotNull(results.get(i).getSourceDdl());
                foreignKeyTableOrder = i;
            }
        }
        Assert.assertTrue(dependentTableOrder1 < foreignKeyTableOrder);
        Assert.assertTrue(dependentTableOrder2 < foreignKeyTableOrder);
    }

    @Test
    public void compareAllTables_delete_by_dependency_order() {
        int dependentTableOrder1 = 0;
        int dependentTableOrder2 = 0;
        int foreignKeyTableOrder = 0;

        for (int i = 0; i < results.size(); i++) {
            if (results.get(i).getDbObjectName().equals("fk_dependency_only_in_target1")) {
                Assert.assertEquals(results.get(i).getComparisonResult(), ComparisonResult.ONLY_IN_TARGET);
                Assert.assertNotNull(results.get(i).getTargetDdl());
                dependentTableOrder1 = i;
            } else if (results.get(i).getDbObjectName().equals("fk_dependency_only_in_target2")) {
                Assert.assertEquals(results.get(i).getComparisonResult(), ComparisonResult.ONLY_IN_TARGET);
                Assert.assertNotNull(results.get(i).getTargetDdl());
                dependentTableOrder2 = i;
            } else if (results.get(i).getDbObjectName().equals("fk_only_in_target")) {
                Assert.assertEquals(results.get(i).getComparisonResult(), ComparisonResult.ONLY_IN_TARGET);
                Assert.assertNotNull(results.get(i).getTargetDdl());
                foreignKeyTableOrder = i;
            }
        }
        Assert.assertTrue(dependentTableOrder1 > foreignKeyTableOrder);
        Assert.assertTrue(dependentTableOrder2 > foreignKeyTableOrder);
    }

    @Test
    public void compareAllTables_update_primary_key() {
        DBObjectComparisonResult updatePk = results.stream().filter(
                result -> result.getDbObjectName().equals("primary_key_test")).collect(Collectors.toList()).get(0);
        Assert.assertEquals(updatePk.getComparisonResult(), ComparisonResult.INCONSISTENT);
        updatePk.getSubDBObjectComparisonResult().forEach(item -> {
            if (item.getDbObjectType().equals(DBObjectType.CONSTRAINT)) {
                Assert.assertEquals(item.getComparisonResult(), ComparisonResult.INCONSISTENT);
                Assert.assertNotNull(item.getSourceDdl());
                Assert.assertNotNull(item.getTargetDdl());
                Assert.assertNotNull(item.getChangeScript());
            }
        });
    }

    @Test
    public void compareAllTables_update_columns() {
        DBObjectComparisonResult result = results.stream().filter(
                item -> item.getDbObjectName().equals("update_column")).collect(Collectors.toList()).get(0);
        Assert.assertEquals(result.getComparisonResult(), ComparisonResult.INCONSISTENT);
        Assert.assertEquals(8, result.getSubDBObjectComparisonResult().size());

        result.getSubDBObjectComparisonResult().forEach(item -> {
            if (item.getDbObjectType().equals(DBObjectType.PARTITION)) {
                Assert.assertEquals(item.getComparisonResult(), ComparisonResult.CONSISTENT);
            } else if (item.getDbObjectName().equals("c1") || item.getDbObjectName().equals("c2")
                    || item.getDbObjectName().equals("c3")) {
                Assert.assertEquals(item.getComparisonResult(), ComparisonResult.INCONSISTENT);
                Assert.assertNotNull(item.getChangeScript());
            } else if (item.getDbObjectName().equals("only_in_source_col")) {
                Assert.assertEquals(item.getComparisonResult(), ComparisonResult.ONLY_IN_SOURCE);
                Assert.assertNotNull(item.getChangeScript());
            } else if (item.getDbObjectName().equals("only_in_target_col")) {
                Assert.assertEquals(item.getComparisonResult(), ComparisonResult.ONLY_IN_TARGET);
                Assert.assertNotNull(item.getChangeScript());
            }
        });
    }

    @Test
    public void compareAllTables_update_index() {
        DBObjectComparisonResult result = results.stream().filter(
                item -> item.getDbObjectName().equals("update_index")).collect(Collectors.toList()).get(0);
        Assert.assertEquals(result.getComparisonResult(), ComparisonResult.INCONSISTENT);
        Assert.assertEquals(7, result.getSubDBObjectComparisonResult().size());

        result.getSubDBObjectComparisonResult().forEach(item -> {
            if (item.getDbObjectType().equals(DBObjectType.PARTITION)) {
                Assert.assertEquals(item.getComparisonResult(), ComparisonResult.CONSISTENT);
            } else if (item.getDbObjectName().equals("idx1")) {
                Assert.assertEquals(item.getComparisonResult(), ComparisonResult.INCONSISTENT);
                Assert.assertNotNull(item.getChangeScript());
            } else if (item.getDbObjectName().equals("idx_only_in_source")) {
                Assert.assertEquals(item.getComparisonResult(), ComparisonResult.ONLY_IN_SOURCE);
                Assert.assertNotNull(item.getChangeScript());
            } else if (item.getDbObjectName().equals("idx_only_in_target")) {
                Assert.assertEquals(item.getComparisonResult(), ComparisonResult.ONLY_IN_TARGET);
                Assert.assertNotNull(item.getChangeScript());
            }
        });
    }

    @Test
    public void compareAllTables_update_constraint() {
        DBObjectComparisonResult result = results.stream().filter(
                item -> item.getDbObjectName().equals("update_constraint")).collect(Collectors.toList()).get(0);
        Assert.assertEquals(result.getComparisonResult(), ComparisonResult.INCONSISTENT);
        Assert.assertEquals(7, result.getSubDBObjectComparisonResult().size());

        result.getSubDBObjectComparisonResult().forEach(item -> {
            if (item.getDbObjectType().equals(DBObjectType.PARTITION)) {
                Assert.assertEquals(item.getComparisonResult(), ComparisonResult.CONSISTENT);
            } else if (item.getDbObjectName().equals("cons")) {
                Assert.assertEquals(item.getComparisonResult(), ComparisonResult.INCONSISTENT);
                Assert.assertNotNull(item.getChangeScript());
            } else if (item.getDbObjectName().equals("cons_only_in_source")) {
                Assert.assertEquals(item.getComparisonResult(), ComparisonResult.ONLY_IN_SOURCE);
                Assert.assertNotNull(item.getChangeScript());
            } else if (item.getDbObjectName().equals("cons_only_in_target")) {
                Assert.assertEquals(item.getComparisonResult(), ComparisonResult.ONLY_IN_TARGET);
                Assert.assertNotNull(item.getChangeScript());
            }
        });
    }

    @Test
    public void compareAllTables_update_partition() {
        DBObjectComparisonResult result = results.stream().filter(
                item -> item.getDbObjectName().equals("update_partition")).collect(Collectors.toList()).get(0);
        Assert.assertEquals(result.getComparisonResult(), ComparisonResult.INCONSISTENT);
        Assert.assertEquals(4, result.getSubDBObjectComparisonResult().size());

        result.getSubDBObjectComparisonResult().forEach(item -> {
            if (item.getDbObjectType().equals(DBObjectType.PARTITION)) {
                Assert.assertEquals(item.getComparisonResult(), ComparisonResult.INCONSISTENT);
                Assert.assertNotNull(item.getChangeScript());
            }
        });
    }

    @Test
    public void compareAllTables_update_table_option() {
        DBObjectComparisonResult result = results.stream().filter(
                item -> item.getDbObjectName().equals("update_options")).collect(Collectors.toList()).get(0);
        Assert.assertEquals(result.getComparisonResult(), ComparisonResult.INCONSISTENT);
        Assert.assertNotNull(result.getChangeScript());
    }

    @Test
    public void compare_specified_tables() {
        OdcSchemaStructureComparator comparator = new OdcSchemaStructureComparator(srcSession, tgtSession);
        List<DBObjectComparisonResult> results =
                comparator.compareTables(Arrays.asList("update_column", "update_index"));
        Assert.assertEquals(results.size(), 2);
    }

}
