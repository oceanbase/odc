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
package com.oceanbase.odc.service.structurecompare.oboracle;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
 * @date 2024/1/26
 * @since
 */
public class DefaultDBStructureComparatorTest extends PluginTestEnv {
    private static final String BASE_PATH =
            "src/test/resources/structurecompare/oboracle/";
    private static String sourceSchemaDdl = FileUtil.loadAsString(BASE_PATH + "source_schema_ddl.sql");
    private static String targetSchemaDdl = FileUtil.loadAsString(BASE_PATH + "target_schema_ddl.sql");
    private static TestDBConfiguration configuration =
            TestDBConfigurations.getInstance().getTestOBOracleConfiguration();
    private static JdbcTemplate jdbcTemplate = new JdbcTemplate(configuration.getDataSource());
    private final static String sourceSchemaName = generateSchemaName() + "_SOURCE";
    private final static String targetSchemaName = generateSchemaName() + "_TARGET";
    private static TestDBConfiguration srcConfiguration = new TestDBConfiguration();
    private static TestDBConfiguration tgtConfiguration = new TestDBConfiguration();
    private static DefaultDBStructureComparator comparator = new DefaultDBStructureComparator();
    private static List<DBObjectComparisonResult> results;
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void setUp() throws SQLException {
        jdbcTemplate.execute("CREATE USER " + sourceSchemaName + " IDENTIFIED BY \"\"");
        jdbcTemplate.execute("CREATE USER " + targetSchemaName + " IDENTIFIED BY \"\"");

        BeanUtils.copyProperties(configuration, srcConfiguration);
        srcConfiguration.setDefaultDBName(sourceSchemaName);
        srcConfiguration.initDataSource();

        BeanUtils.copyProperties(configuration, tgtConfiguration);
        tgtConfiguration.setDefaultDBName(targetSchemaName);
        tgtConfiguration.initDataSource();

        jdbcTemplate.execute("alter session set current_schema=\"" + sourceSchemaName + "\"");
        jdbcTemplate.execute(sourceSchemaDdl);
        jdbcTemplate.execute("alter session set current_schema=\"" + targetSchemaName + "\"");
        jdbcTemplate.execute(targetSchemaDdl);
        jdbcTemplate.execute("alter session set current_schema=\"" + configuration.getDefaultDBName() + "\"");

        results = comparator.compare(getSourceConfig(srcConfiguration),
                getTargetConfig(tgtConfiguration));
    }

    @AfterClass
    public static void clear() {
        jdbcTemplate.execute("drop user " + sourceSchemaName + " cascade");
        jdbcTemplate.execute("drop user " + targetSchemaName + " cascade");
    }

    private static String generateSchemaName() {
        return "ODC_DB_COMPARE_TEST_" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 15).toUpperCase();
    }

    private static DBStructureComparisonConfig getSourceConfig(TestDBConfiguration configuration) {
        DBStructureComparisonConfig srcConfig = new DBStructureComparisonConfig();
        srcConfig.setSchemaName(sourceSchemaName);
        srcConfig.setConnectType(ConnectType.OB_ORACLE);
        srcConfig.setDataSource(configuration.getDataSource());
        srcConfig.setToComparedObjectTypes(Collections.singleton(DBObjectType.TABLE));
        return srcConfig;
    }

    private static DBStructureComparisonConfig getTargetConfig(TestDBConfiguration configuration) {
        DBStructureComparisonConfig srcConfig = new DBStructureComparisonConfig();
        srcConfig.setSchemaName(targetSchemaName);
        srcConfig.setConnectType(ConnectType.OB_ORACLE);
        srcConfig.setDataSource(configuration.getDataSource());
        srcConfig.setToComparedObjectTypes(Collections.singleton(DBObjectType.TABLE));
        return srcConfig;
    }

    @Test
    public void test_updatePrimaryKey() {
        DBObjectComparisonResult updatePk = results.stream().filter(
                result -> result.getDbObjectName().equalsIgnoreCase("PRIMARY_KEY_TEST")).collect(Collectors.toList())
                .get(0);

        List<DBObjectComparisonResult> actual = updatePk.getSubDBObjectComparisonResult().stream().filter(
                item -> item.getDbObjectType().equals(DBObjectType.CONSTRAINT)).collect(Collectors.toList());

        List<DBObjectComparisonResult> expect = new ArrayList<>();
        DBObjectComparisonResult result1 =
                new DBObjectComparisonResult(DBObjectType.CONSTRAINT, actual.get(0).getDbObjectName(), sourceSchemaName,
                        targetSchemaName);
        result1.setComparisonResult(ComparisonResult.ONLY_IN_TARGET);
        result1.setChangeScript("-- Unsupported operation to drop primary key constraint\n");

        DBObjectComparisonResult result2 =
                new DBObjectComparisonResult(DBObjectType.CONSTRAINT, actual.get(1).getDbObjectName(), sourceSchemaName,
                        targetSchemaName);
        result2.setComparisonResult(ComparisonResult.ONLY_IN_SOURCE);
        result2.setChangeScript("-- Unsupported operation to add primary key constraint\n");
        expect.addAll(Arrays.asList(result1, result2));
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void test_onlyInSourceTableChangeScript() {
        String actual = results.stream().filter(
                result -> result.getComparisonResult() == ComparisonResult.ONLY_IN_SOURCE
                        && result.getDbObjectName().equalsIgnoreCase("ONLY_IN_SOURCE_CHILD"))
                .collect(Collectors.toList()).get(0).getChangeScript();

        String expect = "CREATE TABLE \"" + targetSchemaName + "\".\"ONLY_IN_SOURCE_CHILD\" "
                + "(\n"
                + "\"SALE_ID\" NUMBER NOT NULL DEFAULT NULL,\n"
                + "\"PRODUCT_ID\" NUMBER NOT NULL DEFAULT NULL,\n"
                + "\"SALE_DATE\" DATE NOT NULL DEFAULT NULL,\n"
                + "\"CUSTOMER_ID\" NUMBER NOT NULL DEFAULT NULL,\n"
                + "\"SALESPERSON_ID\" NUMBER NOT NULL DEFAULT NULL,\n"
                + "CONSTRAINT \"CHK_SALES_DATE\" CHECK ((\"SALE_DATE\" > TO_DATE('2020-01-01',"
                + "'YYYY-MM-DD'))),\n"
                + "CONSTRAINT \"FK_SALES_PRODUCT\" FOREIGN KEY (\"PRODUCT_ID\") REFERENCES \""
                + targetSchemaName + "\".\"ONLY_IN_SOURCE_PARENT\" "
                + "(\"PRODUCT_ID\"),\n"
                + "CONSTRAINT \"PK_ONLY_IN_SOURCE_CHILD\" PRIMARY KEY (\"SALE_ID\")\n"
                + ")  PARTITION BY HASH(sale_id) \n"
                + "PARTITIONS 3;\n"
                + "CREATE  INDEX \"IDX_SALES_CUSTOMER\" USING BTREE ON \""
                + targetSchemaName + "\".\"ONLY_IN_SOURCE_CHILD\" "
                + "(\"CUSTOMER_ID\") LOCAL;\n"
                + "CREATE  INDEX \"IDX_SALES_SALESPERSON\" USING BTREE ON \""
                + targetSchemaName + "\".\"ONLY_IN_SOURCE_CHILD\" "
                + "(\"SALESPERSON_ID\") GLOBAL;\n"
                + "CREATE UNIQUE INDEX \"UK_SALES\" USING BTREE ON \""
                + targetSchemaName + "\".\"ONLY_IN_SOURCE_CHILD\" "
                + "(\"SALE_ID\", \"SALE_DATE\") GLOBAL;\n";
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void test_updateColumns() {
        DBObjectComparisonResult result = results.stream().filter(
                item -> item.getDbObjectName().equalsIgnoreCase("update_column")).collect(Collectors.toList()).get(0);

        DBObjectComparisonResult id =
                new DBObjectComparisonResult(DBObjectType.COLUMN, "ID", sourceSchemaName, targetSchemaName);
        id.setComparisonResult(ComparisonResult.CONSISTENT);

        DBObjectComparisonResult c1 =
                new DBObjectComparisonResult(DBObjectType.COLUMN, "C1", sourceSchemaName, targetSchemaName);
        c1.setComparisonResult(ComparisonResult.INCONSISTENT);
        c1.setChangeScript("ALTER TABLE \"" + targetSchemaName
                + "\".\"UPDATE_COLUMN\" MODIFY \"C1\" VARCHAR2(100) DEFAULT NULL;\n");

        DBObjectComparisonResult c2 =
                new DBObjectComparisonResult(DBObjectType.COLUMN, "C2", sourceSchemaName, targetSchemaName);
        c2.setComparisonResult(ComparisonResult.INCONSISTENT);
        c2.setChangeScript(
                "ALTER TABLE \"" + targetSchemaName
                        + "\".\"UPDATE_COLUMN\" MODIFY \"C2\" DATE NOT NULL DEFAULT NULL;\n");

        DBObjectComparisonResult c3 =
                new DBObjectComparisonResult(DBObjectType.COLUMN, "C3", sourceSchemaName, targetSchemaName);
        c3.setComparisonResult(ComparisonResult.INCONSISTENT);
        c3.setChangeScript(
                "ALTER TABLE \"" + targetSchemaName
                        + "\".\"UPDATE_COLUMN\" MODIFY \"C3\" NUMBER(10, 2) DEFAULT NULL;\n");

        DBObjectComparisonResult only_in_target_col = new DBObjectComparisonResult(DBObjectType.COLUMN,
                "ONLY_IN_TARGET_COL", sourceSchemaName, targetSchemaName);
        only_in_target_col.setComparisonResult(ComparisonResult.ONLY_IN_TARGET);
        only_in_target_col.setChangeScript(
                "ALTER TABLE \"" + targetSchemaName + "\".\"UPDATE_COLUMN\" DROP COLUMN \"ONLY_IN_TARGET_COL\";\n");

        DBObjectComparisonResult only_in_source_col = new DBObjectComparisonResult(DBObjectType.COLUMN,
                "ONLY_IN_SOURCE_COL", sourceSchemaName, targetSchemaName);
        only_in_source_col.setComparisonResult(ComparisonResult.ONLY_IN_SOURCE);
        only_in_source_col.setChangeScript("ALTER TABLE \"" + targetSchemaName
                + "\".\"UPDATE_COLUMN\" ADD \"ONLY_IN_SOURCE_COL\" NUMBER(38, 0) NULL  "
                + "DEFAULT NULL;\n");

        List<DBObjectComparisonResult> expected = new ArrayList<>();
        expected.addAll(Arrays.asList(only_in_target_col, id, c1, c2, c3, only_in_source_col));

        List<DBObjectComparisonResult> actual = result.getSubDBObjectComparisonResult().stream().filter(
                item -> item.getDbObjectType().equals(DBObjectType.COLUMN)).collect(Collectors.toList());

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void test_updateIndex() {
        DBObjectComparisonResult result = results.stream().filter(
                item -> item.getDbObjectName().equalsIgnoreCase("update_index")).collect(Collectors.toList()).get(0);

        List<DBObjectComparisonResult> actual = result.getSubDBObjectComparisonResult().stream().filter(
                item -> item.getDbObjectType().equals(DBObjectType.INDEX)).collect(Collectors.toList());

        DBObjectComparisonResult idx1 =
                new DBObjectComparisonResult(DBObjectType.INDEX, "IDX1", sourceSchemaName, targetSchemaName);
        idx1.setComparisonResult(ComparisonResult.INCONSISTENT);
        idx1.setChangeScript("DROP INDEX \"IDX1\";\n"
                + "CREATE  INDEX \"IDX1\" USING BTREE ON \"" + targetSchemaName
                + "\".\"UPDATE_INDEX\" (\"C1\") GLOBAL;\n");

        DBObjectComparisonResult idx_only_in_source = new DBObjectComparisonResult(DBObjectType.INDEX,
                "IDX_ONLY_IN_SOURCE", sourceSchemaName, targetSchemaName);
        idx_only_in_source.setComparisonResult(ComparisonResult.ONLY_IN_SOURCE);
        idx_only_in_source.setChangeScript("CREATE  INDEX \"IDX_ONLY_IN_SOURCE\" USING BTREE ON \""
                + targetSchemaName + "\".\"UPDATE_INDEX\" "
                + "(\"C2\") GLOBAL;\n");

        DBObjectComparisonResult idx_only_in_target = new DBObjectComparisonResult(DBObjectType.INDEX,
                "IDX_ONLY_IN_TARGET", sourceSchemaName, targetSchemaName);
        idx_only_in_target.setComparisonResult(ComparisonResult.ONLY_IN_TARGET);
        idx_only_in_target.setChangeScript(
                "DROP INDEX \"IDX_ONLY_IN_TARGET\";\n");

        DBObjectComparisonResult idx_c1_c2_c3 = new DBObjectComparisonResult(DBObjectType.INDEX,
                "IDX_C1_C2_C3", sourceSchemaName, targetSchemaName);
        idx_c1_c2_c3.setComparisonResult(ComparisonResult.CONSISTENT);

        List<DBObjectComparisonResult> expected = new ArrayList<>();
        expected.addAll(Arrays.asList(idx1, idx_c1_c2_c3, idx_only_in_target, idx_only_in_source));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void test_updateConstraint() {
        List<DBObjectComparisonResult> actual = results.stream().filter(
                item -> item.getDbObjectName().equalsIgnoreCase("update_constraint")).collect(Collectors.toList())
                .get(0)
                .getSubDBObjectComparisonResult().stream().filter(
                        item -> item.getDbObjectType().equals(DBObjectType.CONSTRAINT))
                .collect(Collectors.toList());

        DBObjectComparisonResult checkOnlyInTarget = new DBObjectComparisonResult(DBObjectType.CONSTRAINT,
                "CHECK_ONLY_IN_TARGET", sourceSchemaName, targetSchemaName);
        checkOnlyInTarget.setComparisonResult(ComparisonResult.ONLY_IN_TARGET);
        checkOnlyInTarget.setChangeScript("ALTER TABLE \"" + targetSchemaName
                + "\".\"UPDATE_CONSTRAINT\" DROP CONSTRAINT \"CHECK_ONLY_IN_TARGET\";\n");

        DBObjectComparisonResult pKUpdateConstraint = new DBObjectComparisonResult(DBObjectType.CONSTRAINT,
                "PK_UPDATE_CONSTRAINT", sourceSchemaName, targetSchemaName);
        pKUpdateConstraint.setComparisonResult(ComparisonResult.CONSISTENT);

        DBObjectComparisonResult checkOnlyInSource = new DBObjectComparisonResult(DBObjectType.CONSTRAINT,
                "CHECK_ONLY_IN_SOURCE", sourceSchemaName, targetSchemaName);
        checkOnlyInSource.setComparisonResult(ComparisonResult.ONLY_IN_SOURCE);
        checkOnlyInSource.setChangeScript("ALTER TABLE \"" + targetSchemaName
                + "\".\"UPDATE_CONSTRAINT\" ADD CONSTRAINT \"CHECK_ONLY_IN_SOURCE\" CHECK "
                + "((\"GENDER\" in ('Male','Female','Other')));\n");

        DBObjectComparisonResult fkOnluyInSource = new DBObjectComparisonResult(DBObjectType.CONSTRAINT,
                "FK_ONLY_IN_SOURCE", sourceSchemaName, targetSchemaName);
        fkOnluyInSource.setComparisonResult(ComparisonResult.ONLY_IN_SOURCE);
        fkOnluyInSource.setChangeScript("ALTER TABLE \"" + targetSchemaName
                + "\".\"UPDATE_CONSTRAINT\" ADD CONSTRAINT \"FK_ONLY_IN_SOURCE\" FOREIGN KEY "
                + "(\"ID2\", \"ID1\") REFERENCES \"" + targetSchemaName + "\".\"FK_PARENT\" (\"PARENT_ID1\", "
                + "\"PARENT_ID2\");\n");

        List<DBObjectComparisonResult> expected = new ArrayList<>();
        expected.addAll(Arrays.asList(checkOnlyInTarget, pKUpdateConstraint, checkOnlyInSource, fkOnluyInSource));
        Assert.assertEquals(expected, actual);
    }
}
