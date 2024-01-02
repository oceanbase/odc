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
package com.oceanbase.odc.plugin.task.obmysql.structurecompare;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.plugin.task.api.structurecompare.StructureComparator;
import com.oceanbase.odc.plugin.task.api.structurecompare.model.ComparisonResult;
import com.oceanbase.odc.plugin.task.api.structurecompare.model.DBObjectComparisonResult;
import com.oceanbase.odc.plugin.task.api.structurecompare.model.StructureCompareConfig;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.odc.test.util.FileUtil;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

/**
 * @author jingtian
 * @date 2024/1/2
 * @since ODC_release_4.2.4
 */
public class OBMySQLStructureCompareExtensionTest {
    private static final String BASE_PATH = "src/test/resources/structurecompare/";
    private static String sourceSchemaDdl = FileUtil.loadAsString(BASE_PATH + "source_schema_ddl.sql");
    private static String targetSchemaDdl = FileUtil.loadAsString(BASE_PATH + "target_schema_ddl.sql");
    private static final String sourceDrop = FileUtil.loadAsString(BASE_PATH + "source_drop.sql");
    private static final String targetDrop = FileUtil.loadAsString(BASE_PATH + "target_drop.sql");
    private static TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
    private static JdbcTemplate jdbcTemplate = new JdbcTemplate(configuration.getDataSource());
    private final static String sourceSchemaName = generateSchemaName() + "_source";
    private final static String targetSchemaName = generateSchemaName() + "_target";
    private static StructureComparator comparator;
    private static List<DBObjectComparisonResult> results;
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void setUp() throws SQLException {
        jdbcTemplate.execute("create database `" + sourceSchemaName + "`");
        jdbcTemplate.execute("create database `" + targetSchemaName + "`");

        TestDBConfiguration srcConfiguration = new TestDBConfiguration();
        BeanUtils.copyProperties(configuration, srcConfiguration);
        srcConfiguration.setDefaultDBName(sourceSchemaName);
        srcConfiguration.initDataSource();

        TestDBConfiguration tgtConfiguration = new TestDBConfiguration();
        BeanUtils.copyProperties(configuration, tgtConfiguration);
        tgtConfiguration.setDefaultDBName(targetSchemaName);
        tgtConfiguration.initDataSource();

        jdbcTemplate.execute("use `" + sourceSchemaName + "`");
        jdbcTemplate.execute(sourceDrop);
        jdbcTemplate.execute(sourceSchemaDdl);
        jdbcTemplate.execute("use `" + targetSchemaName + "`");
        jdbcTemplate.execute(targetDrop);
        jdbcTemplate.execute(targetSchemaDdl);
        jdbcTemplate.execute("use `" + configuration.getDefaultDBName() + "`");

        OBMySQLStructureCompareExtension extension = new OBMySQLStructureCompareExtension();
        StructureCompareConfig config = new StructureCompareConfig();
        config.setSourceSchemaName(sourceSchemaName);
        config.setTargetSchemaName(targetSchemaName);
        config.setSourceDialectType(DialectType.OB_MYSQL);
        config.setTargetDialectType(DialectType.OB_MYSQL);
        config.setSourceDataSource(srcConfiguration.getDataSource());
        config.setTargetDataSource(tgtConfiguration.getDataSource());
        comparator = extension.getStructureComparator(config);
        results = comparator.compareTables();
    }

    @AfterClass
    public static void clear() {
        jdbcTemplate.execute("drop database `" + sourceSchemaName + "`");
        jdbcTemplate.execute("drop database `" + targetSchemaName + "`");
    }

    private static String generateSchemaName() {
        return "odc_db_compare_test_" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 15).toLowerCase();
    }

    @Test
    public void test_not_supported_dialect() throws SQLException {
        StructureCompareConfig config = new StructureCompareConfig();
        config.setSourceSchemaName(sourceSchemaName);
        config.setTargetSchemaName(targetSchemaName);
        config.setSourceDialectType(DialectType.OB_MYSQL);
        config.setTargetDialectType(DialectType.OB_ORACLE);
        config.setSourceDataSource(configuration.getDataSource());
        config.setTargetDataSource(configuration.getDataSource());
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Source and target dialect type must be OB_MYSQL");
        StructureComparator structureComparator = new OBMySQLStructureCompareExtension().getStructureComparator(config);
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
        DBObjectComparisonResult result1 = results.stream().filter(
                item -> item.getDbObjectName().equals("update_partition1")).collect(Collectors.toList()).get(0);
        Assert.assertEquals(result1.getComparisonResult(), ComparisonResult.INCONSISTENT);
        Assert.assertEquals(4, result1.getSubDBObjectComparisonResult().size());

        result1.getSubDBObjectComparisonResult().forEach(item -> {
            if (item.getDbObjectType().equals(DBObjectType.PARTITION)) {
                Assert.assertEquals(item.getComparisonResult(), ComparisonResult.INCONSISTENT);
                Assert.assertNotNull(item.getChangeScript());
            }
        });

        DBObjectComparisonResult result2 = results.stream().filter(
                item -> item.getDbObjectName().equals("update_partition2")).collect(Collectors.toList()).get(0);
        Assert.assertEquals(result2.getComparisonResult(), ComparisonResult.INCONSISTENT);

        result2.getSubDBObjectComparisonResult().forEach(item -> {
            if (item.getDbObjectType().equals(DBObjectType.PARTITION)) {
                Assert.assertEquals(item.getComparisonResult(), ComparisonResult.ONLY_IN_SOURCE);
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
        List<DBObjectComparisonResult> results =
                comparator.compareTables(Arrays.asList("update_column", "update_index", "fk_dependency_only_in_source1",
                        "fk_only_in_source"));
        Assert.assertEquals(results.size(), 4);
    }
}
