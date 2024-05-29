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
package com.oceanbase.odc.service.structurecompare.obmysql;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.jts.util.Assert;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.PluginTestEnv;
import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.db.browser.DBTableEditorFactory;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;
import com.oceanbase.odc.service.structurecompare.comparedbobject.DBTableStructureComparator;
import com.oceanbase.odc.service.structurecompare.model.ComparisonResult;
import com.oceanbase.odc.service.structurecompare.model.DBObjectComparisonResult;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

/**
 * @Author：tinker
 * @Date: 2024/5/29 11:35
 * @Descripition:
 */
public class DBTableStructureComparatorTest extends PluginTestEnv {
    private static TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
    private static JdbcTemplate jdbcTemplate = new JdbcTemplate(configuration.getDataSource());
    private final static String sourceSchemaName = generateSchemaName() + "_source";
    private final static String targetSchemaName = generateSchemaName() + "_target";
    private final static String srcTableName = "table_structure_compare_test_source";
    private final static String tgtTableName = "table_structure_compare_test_target";
    private static TestDBConfiguration tgtConfig = new TestDBConfiguration();
    private static TestDBConfiguration srcConfig = new TestDBConfiguration();
    private static String srcCreateDdl = "CREATE TABLE `" + srcTableName + "` ("
            + "`id` bigint NOT NULL,`val` varchar(120) NULL,"
            + "`create_time` datetime(0) ON UPDATE CURRENT_TIMESTAMP(0) NOT NULL,"
            + "INDEX `test_idx1` USING BTREE (`id`) LOCAL,"
            + "INDEX `test_idx2` USING BTREE (`id`) GLOBAL,"
            + "CONSTRAINT `test_pk` PRIMARY KEY (`id`, `create_time`)"
            + ") PARTITION BY RANGE COLUMNS(create_time) ("
            + "PARTITION `p1` VALUES LESS THAN ('2023-01-01'),"
            + "PARTITION `p2` VALUES LESS THAN ('2024-01-01')"
            + ");";
    private static String srcDropDdl = "drop table if exists `" + srcTableName + "`";
    private static String tgtCreateDdl = "CREATE TABLE `" + tgtTableName + "` ("
            + "`id` bigint NOT NULL,"
            + "`create_time` datetime(0) ON UPDATE CURRENT_TIMESTAMP(0) NOT NULL,"
            + "INDEX `test_idx1` USING BTREE (`id`) LOCAL,"
            + "CONSTRAINT `test_pk` PRIMARY KEY (`id`, `create_time`)"
            + ")PARTITION BY RANGE COLUMNS(create_time) ("
            + "PARTITION `p1` VALUES LESS THAN ('2023-01-01'));";
    private static String tgtDropDdl = "drop table if exists `" + tgtTableName + "`";



    @BeforeClass
    public static void setUp() {
        jdbcTemplate.execute("create database `" + sourceSchemaName + "`");
        jdbcTemplate.execute("create database `" + targetSchemaName + "`");
        BeanUtils.copyProperties(configuration, srcConfig);
        srcConfig.setDefaultDBName(sourceSchemaName);
        srcConfig.initDataSource();
        BeanUtils.copyProperties(configuration, tgtConfig);
        tgtConfig.setDefaultDBName(targetSchemaName);
        tgtConfig.initDataSource();

        jdbcTemplate.execute("use `" + sourceSchemaName + "`");
        jdbcTemplate.execute(srcDropDdl);
        jdbcTemplate.execute(srcCreateDdl);
        jdbcTemplate.execute("use `" + targetSchemaName + "`");
        jdbcTemplate.execute(tgtDropDdl);
        jdbcTemplate.execute(tgtCreateDdl);
        jdbcTemplate.execute("use `" + configuration.getDefaultDBName() + "`");
    }

    @AfterClass
    public static void clear() {
        jdbcTemplate.execute("drop database `" + sourceSchemaName + "`");
        jdbcTemplate.execute("drop database `" + targetSchemaName + "`");
    }


    @Test
    public void test_compareTablesWithDifferentTableName() throws SQLException {
        String tgtDbVersion = getDBVersion(ConnectType.OB_MYSQL, tgtConfig.getDataSource());
        String srcDbVersion = getDBVersion(ConnectType.OB_MYSQL, srcConfig.getDataSource());
        DBTableEditor tgtTableEditor = getDBTableEditor(ConnectType.OB_MYSQL, tgtDbVersion);
        DBSchemaAccessor srcAccessor =
                getDBSchemaAccessor(ConnectType.OB_MYSQL, srcConfig.getDataSource(), srcDbVersion);
        DBSchemaAccessor tgtAccessor =
                getDBSchemaAccessor(ConnectType.OB_MYSQL, tgtConfig.getDataSource(), tgtDbVersion);
        DBTable srcTable = srcAccessor.getTables(sourceSchemaName,
                Collections.singletonList(srcTableName)).get(srcTableName);
        DBTable tgtTable = tgtAccessor.getTables(targetSchemaName,
                Collections.singletonList(tgtTableName)).get(tgtTableName);
        DBTableStructureComparator comparator = new DBTableStructureComparator(tgtTableEditor, DialectType.OB_MYSQL,
                sourceSchemaName, targetSchemaName);
        DBObjectComparisonResult compare = comparator.compare(srcTable, tgtTable);
        Assert.isTrue(compare.getComparisonResult() == ComparisonResult.INCONSISTENT);
        List<DBObjectComparisonResult> onlyInSourceColumns = compare.getSubDBObjectComparisonResult().stream().filter(
                o -> o.getDbObjectType() == DBObjectType.COLUMN
                        && o.getComparisonResult() == ComparisonResult.ONLY_IN_SOURCE)
                .collect(
                        Collectors.toList());
        Assert.equals(1, onlyInSourceColumns.size());
        String expectAddColumn = String.format(
                "ALTER TABLE `%s`.`%s` ADD COLUMN `val` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL ;",
                targetSchemaName, tgtTableName);
        Assert.equals(expectAddColumn, onlyInSourceColumns.get(0).getChangeScript().trim());
        List<DBObjectComparisonResult> onlyInSourceIndexes = compare.getSubDBObjectComparisonResult().stream().filter(
                o -> o.getDbObjectType() == DBObjectType.INDEX
                        && o.getComparisonResult() == ComparisonResult.ONLY_IN_SOURCE)
                .collect(
                        Collectors.toList());
        Assert.equals(1, onlyInSourceIndexes.size());
        String expectAddIndex = String.format("CREATE  INDEX `test_idx2` USING BTREE ON `%s`.`%s` (`id`) GLOBAL;",
                targetSchemaName, tgtTableName);
        Assert.equals(expectAddIndex, onlyInSourceIndexes.get(0).getChangeScript().trim());
        List<DBObjectComparisonResult> inconsistentPartition = compare.getSubDBObjectComparisonResult().stream().filter(
                o -> o.getDbObjectType() == DBObjectType.PARTITION
                        && o.getComparisonResult() == ComparisonResult.INCONSISTENT)
                .collect(
                        Collectors.toList());
        Assert.equals(1, inconsistentPartition.size());
        String expectAddPartition = String.format(
                "ALTER TABLE `%s`.`%s` ADD PARTITION(PARTITION `p2` VALUES LESS THAN ('2024-01-01 00:00:00'));",
                targetSchemaName, tgtTableName);
        Assert.equals(expectAddPartition, inconsistentPartition.get(0).getChangeScript().trim());
    }

    private static String generateSchemaName() {
        return "odc_table_compare_test_"
                + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 15).toLowerCase();
    }


    private static String getDBVersion(ConnectType connectType, DataSource dataSource) throws SQLException {
        return ConnectionPluginUtil.getInformationExtension(connectType.getDialectType())
                .getDBVersion(dataSource.getConnection());
    }

    private static DBTableEditor getDBTableEditor(ConnectType connectType, String dbVersion) {
        return new DBTableEditorFactory(connectType, dbVersion).create();
    }

    private static DBSchemaAccessor getDBSchemaAccessor(ConnectType connectType, DataSource dataSource,
            String dbVersion)
            throws SQLException {
        return DBSchemaAccessors.create(JdbcOperationsUtil.getJdbcOperations(dataSource.getConnection()), null,
                connectType, dbVersion, null);
    }
}
