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
package com.oceanbase.odc.service.structurecompare;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import com.oceanbase.odc.PluginTestEnv;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.service.structurecompare.model.ComparisonResult;
import com.oceanbase.odc.service.structurecompare.model.DBObjectComparisonResult;
import com.oceanbase.odc.service.structurecompare.model.DBStructureComparisonConfig;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.odc.test.util.FileUtil;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

/**
 * @author jingtian
 * @date 2024/1/8
 * @since ODC_release_4.2.4
 */
public class DefaultDBStructureComparatorTest extends PluginTestEnv {
    private static final String BASE_PATH = "src/test/resources/structurecompare/";
    private static String sourceSchemaDdl = FileUtil.loadAsString(BASE_PATH + "source_schema_ddl.sql");
    private static String targetSchemaDdl = FileUtil.loadAsString(BASE_PATH + "target_schema_ddl.sql");
    private static final String sourceDrop = FileUtil.loadAsString(BASE_PATH + "source_drop.sql");
    private static final String targetDrop = FileUtil.loadAsString(BASE_PATH + "target_drop.sql");
    private static TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
    private static JdbcTemplate jdbcTemplate = new JdbcTemplate(configuration.getDataSource());
    private final static String sourceSchemaName = generateSchemaName() + "_source";
    private final static String targetSchemaName = generateSchemaName() + "_target";
    private static TestDBConfiguration srcConfiguration = new TestDBConfiguration();
    private static TestDBConfiguration tgtConfiguration = new TestDBConfiguration();
    private static DefaultDBStructureComparator comparator = new DefaultDBStructureComparator();
    private static List<DBObjectComparisonResult> results;
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void setUp() throws SQLException {
        jdbcTemplate.execute("create database `" + sourceSchemaName + "`");
        jdbcTemplate.execute("create database `" + targetSchemaName + "`");

        BeanUtils.copyProperties(configuration, srcConfiguration);
        srcConfiguration.setDefaultDBName(sourceSchemaName);
        srcConfiguration.initDataSource();

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

        results = comparator.compare(getSourceConfig(srcConfiguration),
                getTargetConfig(tgtConfiguration));
    }

    @AfterClass
    public static void clear() {
        jdbcTemplate.execute("drop database `" + sourceSchemaName + "`");
        jdbcTemplate.execute("drop database `" + targetSchemaName + "`");
    }

    private static String generateSchemaName() {
        return "odc_db_compare_test_" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 15).toLowerCase();
    }

    private static DBStructureComparisonConfig getSourceConfig(TestDBConfiguration configuration) {
        DBStructureComparisonConfig srcConfig = new DBStructureComparisonConfig();
        srcConfig.setSchemaName(sourceSchemaName);
        srcConfig.setConnectType(ConnectType.OB_MYSQL);
        srcConfig.setDataSource(configuration.getDataSource());
        srcConfig.setToComparedObjectTypes(Collections.singleton(DBObjectType.TABLE));
        return srcConfig;
    }

    private static DBStructureComparisonConfig getTargetConfig(TestDBConfiguration configuration) {
        DBStructureComparisonConfig srcConfig = new DBStructureComparisonConfig();
        srcConfig.setSchemaName(targetSchemaName);
        srcConfig.setConnectType(ConnectType.OB_MYSQL);
        srcConfig.setDataSource(configuration.getDataSource());
        srcConfig.setToComparedObjectTypes(Collections.singleton(DBObjectType.TABLE));
        return srcConfig;
    }

    @Test
    public void test_srcDialectTypeNotEqualsToTgtDialectType_throwException() throws SQLException {
        DBStructureComparisonConfig targetConfig = getTargetConfig(tgtConfiguration);
        targetConfig.setConnectType(ConnectType.OB_ORACLE);
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("The dialect type of source and target schema must be equal");
        comparator.compare(getSourceConfig(srcConfiguration), targetConfig);
    }

    @Test
    public void test_notSupportedDialect_throwException() throws SQLException {
        DBStructureComparisonConfig sourceConfig = getSourceConfig(srcConfiguration);
        sourceConfig.setConnectType(ConnectType.ORACLE);
        DBStructureComparisonConfig targetConfig = getTargetConfig(tgtConfiguration);
        targetConfig.setConnectType(ConnectType.ORACLE);
        thrown.expect(UnsupportedOperationException.class);
        thrown.expectMessage("Unsupported dialect type for schema structure comparison: ORACLE");
        comparator.compare(sourceConfig, targetConfig);
    }

    @Test
    public void test_notSupportedDBObjectType_throwException() throws SQLException {
        DBStructureComparisonConfig sourceConfig = getSourceConfig(srcConfiguration);
        sourceConfig.setToComparedObjectTypes(Collections.singleton(DBObjectType.VIEW));
        thrown.expectMessage("Unsupported database object type for schema structure comparison: VIEW");
        comparator.compare(sourceConfig, getTargetConfig(tgtConfiguration));
    }

    @Test
    public void test_GetCreateTableResultsByDependencyOrder() {
        int dependentTableOrder1 = 0;
        int dependentTableOrder2 = 0;
        int foreignKeyTableOrder = 0;
        List<DBObjectComparisonResult> toCreate = results.stream().filter(
                item -> item.getComparisonResult().equals(ComparisonResult.ONLY_IN_SOURCE)).collect(
                        Collectors.toList());
        for (int i = 0; i < toCreate.size(); i++) {
            if (toCreate.get(i).getDbObjectName().equals("fk_dependency_only_in_source1")) {
                dependentTableOrder1 = i;
            } else if (toCreate.get(i).getDbObjectName().equals("fk_dependency_only_in_source2")) {
                dependentTableOrder2 = i;
            } else if (toCreate.get(i).getDbObjectName().equals("fk_only_in_source")) {
                foreignKeyTableOrder = i;
            }
        }
        Assert.assertTrue(dependentTableOrder1 < foreignKeyTableOrder && dependentTableOrder2 < foreignKeyTableOrder);
    }

    @Test
    public void test_GetDroppedTableResultsByDependencyOrder() {
        int dependentTableOrder1 = 0;
        int dependentTableOrder2 = 0;
        int foreignKeyTableOrder = 0;
        List<DBObjectComparisonResult> toDrop = results.stream().filter(
                item -> item.getComparisonResult().equals(ComparisonResult.ONLY_IN_TARGET)).collect(
                        Collectors.toList());
        for (int i = 0; i < toDrop.size(); i++) {
            if (toDrop.get(i).getDbObjectName().equals("fk_dependency_only_in_target1")) {
                dependentTableOrder1 = i;
            } else if (toDrop.get(i).getDbObjectName().equals("fk_dependency_only_in_target2")) {
                dependentTableOrder2 = i;
            } else if (toDrop.get(i).getDbObjectName().equals("fk_only_in_target")) {
                foreignKeyTableOrder = i;
            }
        }
        Assert.assertTrue(dependentTableOrder1 > foreignKeyTableOrder && dependentTableOrder2 > foreignKeyTableOrder);
    }

    @Test
    public void test_updatePrimaryKey() {
        DBObjectComparisonResult updatePk = results.stream().filter(
                result -> result.getDbObjectName().equals("primary_key_test")).collect(Collectors.toList()).get(0);

        DBObjectComparisonResult actual = updatePk.getSubDBObjectComparisonResult().stream().filter(
                item -> item.getDbObjectType().equals(DBObjectType.CONSTRAINT)).collect(Collectors.toList()).get(0);
        DBObjectComparisonResult expect =
                new DBObjectComparisonResult(DBObjectType.CONSTRAINT, "PRIMARY", sourceSchemaName, targetSchemaName);
        expect.setChangeScript("ALTER TABLE `" + targetSchemaName + "`.`primary_key_test` DROP PRIMARY KEY;\n"
                + "ALTER TABLE `" + targetSchemaName + "`.`primary_key_test` ADD CONSTRAINT `PRIMARY` "
                + "PRIMARY KEY (`c1`);\n");
        expect.setComparisonResult(ComparisonResult.INCONSISTENT);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void test_updateColumns() {
        DBObjectComparisonResult result = results.stream().filter(
                item -> item.getDbObjectName().equals("update_column")).collect(Collectors.toList()).get(0);

        DBObjectComparisonResult id =
                new DBObjectComparisonResult(DBObjectType.COLUMN, "id", sourceSchemaName, targetSchemaName);
        id.setComparisonResult(ComparisonResult.CONSISTENT);

        DBObjectComparisonResult c1 =
                new DBObjectComparisonResult(DBObjectType.COLUMN, "c1", sourceSchemaName, targetSchemaName);
        c1.setComparisonResult(ComparisonResult.INCONSISTENT);
        c1.setChangeScript("ALTER TABLE `" + targetSchemaName
                + "`.`update_column` MODIFY COLUMN `c1` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL;\n");

        DBObjectComparisonResult c2 =
                new DBObjectComparisonResult(DBObjectType.COLUMN, "c2", sourceSchemaName, targetSchemaName);
        c2.setComparisonResult(ComparisonResult.INCONSISTENT);
        c2.setChangeScript(
                "ALTER TABLE `" + targetSchemaName + "`.`update_column` MODIFY COLUMN `c2` date NOT NULL;\n");

        DBObjectComparisonResult c3 =
                new DBObjectComparisonResult(DBObjectType.COLUMN, "c3", sourceSchemaName, targetSchemaName);
        c3.setComparisonResult(ComparisonResult.INCONSISTENT);
        c3.setChangeScript(
                "ALTER TABLE `" + targetSchemaName + "`.`update_column` MODIFY COLUMN `c3` decimal(10, 2) NOT NULL;\n");

        DBObjectComparisonResult only_in_target_col = new DBObjectComparisonResult(DBObjectType.COLUMN,
                "only_in_target_col", sourceSchemaName, targetSchemaName);
        only_in_target_col.setComparisonResult(ComparisonResult.ONLY_IN_TARGET);
        only_in_target_col.setChangeScript(
                "ALTER TABLE `" + targetSchemaName + "`.`update_column` DROP COLUMN `only_in_target_col`;\n");

        DBObjectComparisonResult only_in_source_col = new DBObjectComparisonResult(DBObjectType.COLUMN,
                "only_in_source_col", sourceSchemaName, targetSchemaName);
        only_in_source_col.setComparisonResult(ComparisonResult.ONLY_IN_SOURCE);
        only_in_source_col.setChangeScript("ALTER TABLE `" + targetSchemaName
                + "`.`update_column` ADD COLUMN `only_in_source_col` int(11) NULL ;\n");

        List<DBObjectComparisonResult> expected = new ArrayList<>();
        expected.addAll(Arrays.asList(only_in_target_col, id, c1, c2, c3, only_in_source_col));

        List<DBObjectComparisonResult> actual = result.getSubDBObjectComparisonResult().stream().filter(
                item -> item.getDbObjectType().equals(DBObjectType.COLUMN)).collect(Collectors.toList());

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void test_updateIndex() {
        DBObjectComparisonResult result = results.stream().filter(
                item -> item.getDbObjectName().equals("update_index")).collect(Collectors.toList()).get(0);

        List<DBObjectComparisonResult> actual = result.getSubDBObjectComparisonResult().stream().filter(
                item -> item.getDbObjectType().equals(DBObjectType.INDEX)).collect(Collectors.toList());

        DBObjectComparisonResult idx1 =
                new DBObjectComparisonResult(DBObjectType.INDEX, "idx1", sourceSchemaName, targetSchemaName);
        idx1.setComparisonResult(ComparisonResult.INCONSISTENT);
        idx1.setChangeScript("ALTER TABLE `" + targetSchemaName + "`.`update_index` DROP INDEX "
                + "`idx1`;\n"
                + "CREATE  INDEX `idx1` USING BTREE ON `" + targetSchemaName + "`"
                + ".`update_index` (`c1`) LOCAL;\n");

        DBObjectComparisonResult idx_only_in_source = new DBObjectComparisonResult(DBObjectType.INDEX,
                "idx_only_in_source", sourceSchemaName, targetSchemaName);
        idx_only_in_source.setComparisonResult(ComparisonResult.ONLY_IN_SOURCE);
        idx_only_in_source.setChangeScript("CREATE  INDEX `idx_only_in_source` USING BTREE ON `" + targetSchemaName
                + "`.`update_index` (`c2`) LOCAL;\n");

        DBObjectComparisonResult idx_only_in_target = new DBObjectComparisonResult(DBObjectType.INDEX,
                "idx_only_in_target", sourceSchemaName, targetSchemaName);
        idx_only_in_target.setComparisonResult(ComparisonResult.ONLY_IN_TARGET);
        idx_only_in_target.setChangeScript(
                "ALTER TABLE `" + targetSchemaName + "`.`update_index` DROP INDEX `idx_only_in_target`;\n");

        List<DBObjectComparisonResult> expected = new ArrayList<>();
        expected.addAll(Arrays.asList(idx1, idx_only_in_target, idx_only_in_source));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void test_updateConstraint() {
        DBObjectComparisonResult actual1 = results.stream().filter(
                item -> item.getDbObjectName().equals("add_foreign_key_constraint")).collect(Collectors.toList()).get(0)
                .getSubDBObjectComparisonResult().stream().filter(
                        item -> item.getDbObjectType().equals(DBObjectType.CONSTRAINT)
                                && !item.getDbObjectName().equals("PRIMARY"))
                .collect(Collectors.toList()).get(0);

        DBObjectComparisonResult actual2 = results.stream().filter(
                item -> item.getDbObjectName().equals("drop_foreign_key_constraint")).collect(Collectors.toList())
                .get(0).getSubDBObjectComparisonResult().stream().filter(
                        item -> item.getDbObjectType().equals(DBObjectType.CONSTRAINT)
                                && !item.getDbObjectName().equals("PRIMARY"))
                .collect(Collectors.toList()).get(0);

        DBObjectComparisonResult excepted1 = new DBObjectComparisonResult(DBObjectType.CONSTRAINT,
                actual1.getDbObjectName(), sourceSchemaName, targetSchemaName);
        excepted1.setComparisonResult(ComparisonResult.ONLY_IN_SOURCE);
        excepted1.setChangeScript("ALTER TABLE `" + targetSchemaName + "`.`add_foreign_key_constraint` ADD CONSTRAINT `"
                + actual1.getDbObjectName() + "` FOREIGN KEY (`product_id`) REFERENCES `" + targetSchemaName
                + "`.`fk_dependency` (`product_id`);\n");

        DBObjectComparisonResult excepted2 = new DBObjectComparisonResult(DBObjectType.CONSTRAINT,
                actual2.getDbObjectName(), sourceSchemaName, targetSchemaName);
        excepted2.setComparisonResult(ComparisonResult.ONLY_IN_TARGET);
        excepted2.setChangeScript("ALTER TABLE `" + targetSchemaName
                + "`.`drop_foreign_key_constraint` DROP FOREIGN KEY `" + actual2.getDbObjectName() + "`;\n");

        Assert.assertEquals(excepted1, actual1);
        Assert.assertEquals(excepted2, actual2);
    }

    @Test
    public void test_updatePartition() {
        DBObjectComparisonResult actual = results.stream().filter(
                item -> item.getDbObjectName().equals("update_partition")).collect(Collectors.toList()).get(0)
                .getSubDBObjectComparisonResult().stream().filter(
                        item -> item.getDbObjectType().equals(DBObjectType.PARTITION))
                .collect(Collectors.toList()).get(0);

        DBObjectComparisonResult excepted =
                new DBObjectComparisonResult(DBObjectType.PARTITION, sourceSchemaName, targetSchemaName);
        excepted.setComparisonResult(ComparisonResult.INCONSISTENT);
        excepted.setChangeScript("ALTER TABLE `" + targetSchemaName + "`.`update_partition` DROP PARTITION (p4);\n");
        Assert.assertEquals(excepted, actual);
    }

    @Test
    public void test_converseToPartitionedTable() {
        DBObjectComparisonResult actual = results.stream().filter(
                item -> item.getDbObjectName().equals("converse_to_partition_table")).collect(Collectors.toList())
                .get(0).getSubDBObjectComparisonResult().stream().filter(
                        item -> item.getDbObjectType().equals(DBObjectType.PARTITION))
                .collect(Collectors.toList()).get(0);

        DBObjectComparisonResult excepted =
                new DBObjectComparisonResult(DBObjectType.PARTITION, sourceSchemaName, targetSchemaName);
        excepted.setComparisonResult(ComparisonResult.ONLY_IN_SOURCE);
        excepted.setChangeScript("ALTER TABLE `" + targetSchemaName + "`.`converse_to_partition_table`  PARTITION"
                + " BY KEY(`id`) \n"
                + "PARTITIONS 4(\n"
                + "PARTITION `p0`,\n"
                + "PARTITION `p1`,\n"
                + "PARTITION `p2`,\n"
                + "PARTITION `p3`\n"
                + ");\n");
        Assert.assertEquals(excepted, actual);
    }

    @Test
    public void test_converseToNonPartitionedTable_notSupported() {
        DBObjectComparisonResult actual = results.stream().filter(
                item -> item.getDbObjectName().equals("converse_to_non_partition_table")).collect(Collectors.toList())
                .get(0).getSubDBObjectComparisonResult().stream().filter(
                        item -> item.getDbObjectType().equals(DBObjectType.PARTITION))
                .collect(Collectors.toList()).get(0);

        DBObjectComparisonResult excepted =
                new DBObjectComparisonResult(DBObjectType.PARTITION, sourceSchemaName, targetSchemaName);
        excepted.setComparisonResult(ComparisonResult.UNSUPPORTED);
        excepted.setChangeScript("/* Unsupported operation: Convert partitioned table to non-partitioned table */\n");
        Assert.assertEquals(excepted, actual);
    }

    @Test
    public void test_updateTableOption() {
        DBObjectComparisonResult result = results.stream().filter(
                item -> item.getDbObjectName().equals("update_options")).collect(Collectors.toList()).get(0);
        Assert.assertEquals(result.getComparisonResult(), ComparisonResult.INCONSISTENT);
        Assert.assertEquals(result.getChangeScript(), "ALTER TABLE `" + targetSchemaName + "`"
                + ".`update_options` COMMENT = 'comment1';\n"
                + "ALTER TABLE `" + targetSchemaName + "`"
                + ".`update_options` CHARACTER SET = utf8mb4;\n"
                + "ALTER TABLE `" + targetSchemaName + "`"
                + ".`update_options` COLLATE = utf8mb4_bin;\n");
    }

    @Test
    public void test_compareSpecifiedTables() throws SQLException {
        DBStructureComparisonConfig sourceConfig = getSourceConfig(srcConfiguration);
        Map<DBObjectType, Set<String>> blackListMap = new HashMap<>();
        Set<String> set = new HashSet<>();
        set.add("update_column");
        set.add("update_index");
        set.add("fk_dependency_only_in_source1");
        set.add("fk_only_in_source");
        set.add("fk_dependency_only_in_target1");
        blackListMap.put(DBObjectType.TABLE, set);
        sourceConfig.setBlackListMap(blackListMap);

        List<DBObjectComparisonResult> result = comparator.compare(sourceConfig, getTargetConfig(tgtConfiguration));
        Assert.assertEquals(result.size(), 5);
    }
}
