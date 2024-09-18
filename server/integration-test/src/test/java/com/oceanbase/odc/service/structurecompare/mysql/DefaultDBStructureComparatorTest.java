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
package com.oceanbase.odc.service.structurecompare.mysql;

import java.sql.SQLException;
import java.util.Collections;
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
import com.oceanbase.odc.service.structurecompare.DefaultDBStructureComparator;
import com.oceanbase.odc.service.structurecompare.model.ComparisonResult;
import com.oceanbase.odc.service.structurecompare.model.DBObjectComparisonResult;
import com.oceanbase.odc.service.structurecompare.model.DBStructureComparisonConfig;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.odc.test.util.FileUtil;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

/**
 * @author jingtian
 * @date 2024/9/14
 */
public class DefaultDBStructureComparatorTest extends PluginTestEnv {
    private static final String BASE_PATH =
            "src/test/resources/structurecompare/mysql/";
    private static String sourceSchemaDdl = FileUtil.loadAsString(BASE_PATH + "source_schema_ddl.sql");
    private static String targetSchemaDdl = FileUtil.loadAsString(BASE_PATH + "target_schema_ddl.sql");
    private static final String sourceDrop = FileUtil.loadAsString(BASE_PATH + "source_drop.sql");
    private static final String targetDrop = FileUtil.loadAsString(BASE_PATH + "target_drop.sql");
    private static TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestMysqlConfiguration();
    private static JdbcTemplate jdbcTemplate = new JdbcTemplate(configuration.getDataSource());
    private final static String sourceSchemaName = generateSchemaName() + "_source";
    private final static String targetSchemaName = generateSchemaName() + "_target";
    private static TestDBConfiguration srcConfiguration = new TestDBConfiguration();
    private static TestDBConfiguration tgtConfiguration = new TestDBConfiguration();
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
        srcConfig.setConnectType(ConnectType.MYSQL);
        srcConfig.setDataSource(configuration.getDataSource());
        srcConfig.setToComparedObjectTypes(Collections.singleton(DBObjectType.TABLE));
        return srcConfig;
    }

    private static DBStructureComparisonConfig getTargetConfig(TestDBConfiguration configuration) {
        DBStructureComparisonConfig srcConfig = new DBStructureComparisonConfig();
        srcConfig.setSchemaName(targetSchemaName);
        srcConfig.setConnectType(ConnectType.MYSQL);
        srcConfig.setDataSource(configuration.getDataSource());
        srcConfig.setToComparedObjectTypes(Collections.singleton(DBObjectType.TABLE));
        return srcConfig;
    }

    @Test
    public void test_updateTableColumn_tinyint() throws SQLException {
        DBObjectComparisonResult result = getCompareResult("tinyint_test");

        Assert.assertEquals(result.getComparisonResult(), ComparisonResult.INCONSISTENT);
        DBObjectComparisonResult actual = result.getSubDBObjectComparisonResult()
                .stream().filter(item -> item.getComparisonResult() == ComparisonResult.INCONSISTENT).collect(
                        Collectors.toList())
                .get(0);
        DBObjectComparisonResult expect =
                new DBObjectComparisonResult(DBObjectType.COLUMN, sourceSchemaName, targetSchemaName);
        expect.setComparisonResult(ComparisonResult.INCONSISTENT);
        expect.setChangeScript("ALTER TABLE `" + targetSchemaName
                + "`.`tinyint_test` MODIFY COLUMN `c1` tinyint(1) UNSIGNED  ZEROFILL  DEFAULT '0' NOT NULL;\n");
        expect.setDbObjectName("c1");
        Assert.assertEquals(expect, actual);
    }

    private DBObjectComparisonResult getCompareResult(String compareTableName) throws SQLException {
        DBStructureComparisonConfig srcConfig = getSourceConfig(srcConfiguration);
        srcConfig
                .setBlackListMap(Collections.singletonMap(DBObjectType.TABLE, Collections.singleton(compareTableName)));
        DBStructureComparisonConfig tgtConfig = getTargetConfig(tgtConfiguration);
        DefaultDBStructureComparator comparator = new DefaultDBStructureComparator();
        return comparator.compare(srcConfig, tgtConfig).get(0);
    }
}
