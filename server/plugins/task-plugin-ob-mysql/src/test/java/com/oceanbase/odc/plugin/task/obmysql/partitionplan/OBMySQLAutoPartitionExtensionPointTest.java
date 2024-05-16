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
package com.oceanbase.odc.plugin.task.obmysql.partitionplan;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.plugin.schema.obmysql.OBMySQLTableExtension;
import com.oceanbase.odc.plugin.task.api.partitionplan.AutoPartitionExtensionPoint;
import com.oceanbase.odc.plugin.task.api.partitionplan.datatype.TimeDataType;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.create.PartitionExprGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.drop.DropPartitionGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.drop.KeepMostLatestPartitionGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.partitionname.PartitionNameGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.PartitionPlanVariableKey;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.SqlExprBasedGeneratorConfig;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.TimeIncreaseGeneratorConfig;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;
import com.oceanbase.tools.dbbrowser.model.datatype.GeneralDataType;

/**
 * Test cases for {@link OBMySQLAutoPartitionExtensionPoint}
 *
 * @author yh263208
 * @date 2024-01-24 14:30
 * @since ODC_release_4.2.4
 */
public class OBMySQLAutoPartitionExtensionPointTest {

    public static final String LIST_PARTI_TABLE_NAME = "list_parti_tbl";
    public static final String NO_PARTI_TABLE_NAME = "no_parti_tbl";
    public static final String RANGE_TABLE_NAME = "range_parti_ext_tbl";
    public static final String RANGE_MAX_VALUE_TABLE_NAME = "range_maxvalue_parti_ext_tbl";
    public static final String RANGE_COLUMNS_TABLE_NAME = "range_col_parti_ext_tbl";
    public static final String REAL_RANGE_TABLE_NAME = "real_range_parti_ext_tbl";
    public static final String REAL_RANGE_COL_TABLE_NAME = "real_range_col_parti_ext_tbl";

    @BeforeClass
    public static void setUp() throws IOException {
        JdbcTemplate oracle = new JdbcTemplate(TestDBConfigurations.getInstance()
                .getTestOBMysqlConfiguration().getDataSource());
        getDdlContent().forEach(oracle::execute);
    }

    @AfterClass
    public static void clear() {
        JdbcTemplate oracle = new JdbcTemplate(TestDBConfigurations.getInstance()
                .getTestOBMysqlConfiguration().getDataSource());
        oracle.execute("DROP TABLE " + RANGE_COLUMNS_TABLE_NAME);
        oracle.execute("DROP TABLE " + LIST_PARTI_TABLE_NAME);
        oracle.execute("DROP TABLE " + NO_PARTI_TABLE_NAME);
        oracle.execute("DROP TABLE " + RANGE_TABLE_NAME);
        oracle.execute("DROP TABLE " + REAL_RANGE_TABLE_NAME);
        oracle.execute("DROP TABLE " + RANGE_MAX_VALUE_TABLE_NAME);
        oracle.execute("DROP TABLE " + REAL_RANGE_COL_TABLE_NAME);
    }

    @Test
    public void supports_rangeParti_returnTrue() throws SQLException {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            DBTable dbTable = new OBMySQLTableExtension().getDetail(connection,
                    configuration.getDefaultDBName(), RANGE_COLUMNS_TABLE_NAME);
            AutoPartitionExtensionPoint extensionPoint = new OBMySQLAutoPartitionExtensionPoint();
            Assert.assertTrue(extensionPoint.supports(dbTable.getPartition()));
        }
    }

    @Test
    public void supports_rangePartiMaxValue_returnFalse() throws SQLException {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        Connection connection = configuration.getDataSource().getConnection();
        DBTable dbTable = new OBMySQLTableExtension().getDetail(connection,
                configuration.getDefaultDBName(), RANGE_MAX_VALUE_TABLE_NAME);
        AutoPartitionExtensionPoint extensionPoint = new OBMySQLAutoPartitionExtensionPoint();
        Assert.assertFalse(extensionPoint.supports(dbTable.getPartition()));
    }

    @Test
    public void supports_nonParti_returnFalse() throws SQLException {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            DBTable dbTable = new OBMySQLTableExtension().getDetail(connection,
                    configuration.getDefaultDBName(), NO_PARTI_TABLE_NAME);
            AutoPartitionExtensionPoint extensionPoint = new OBMySQLAutoPartitionExtensionPoint();
            Assert.assertFalse(extensionPoint.supports(dbTable.getPartition()));
        }
    }

    @Test
    public void supports_listParti_returnFalse() throws SQLException {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            DBTable dbTable = new OBMySQLTableExtension().getDetail(connection,
                    configuration.getDefaultDBName(), LIST_PARTI_TABLE_NAME);
            AutoPartitionExtensionPoint extensionPoint = new OBMySQLAutoPartitionExtensionPoint();
            Assert.assertFalse(extensionPoint.supports(dbTable.getPartition()));
        }
    }

    @Test
    public void getPartitionKeyDataTypes_rangeParti_getSucceed() throws SQLException, IOException {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            DBTable dbTable = new OBMySQLTableExtension().getDetail(connection,
                    configuration.getDefaultDBName(), RANGE_COLUMNS_TABLE_NAME);
            AutoPartitionExtensionPoint extensionPoint = new OBMySQLAutoPartitionExtensionPoint();
            List<DataType> actuals = extensionPoint.getPartitionKeyDataTypes(connection, dbTable);
            Assert.assertEquals(Arrays.asList(new GeneralDataType(0, 0, "varchar"),
                    new TimeDataType("date", TimeDataType.DAY)), actuals);
        }
    }

    @Test
    public void getPartitionKeyDataTypes_rangeExprParti_getSucceed() throws SQLException, IOException {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            DBTable dbTable = new OBMySQLTableExtension().getDetail(connection,
                    configuration.getDefaultDBName(), RANGE_TABLE_NAME);
            AutoPartitionExtensionPoint extensionPoint = new OBMySQLAutoPartitionExtensionPoint();
            List<DataType> actuals = extensionPoint.getPartitionKeyDataTypes(connection, dbTable);
            Assert.assertEquals(Collections.singletonList(new GeneralDataType(0, 0, "BIGINT")), actuals);
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getPartitionKeyDataTypes_listParti_exptThrown() throws SQLException, IOException {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            DBTable dbTable = new OBMySQLTableExtension().getDetail(connection,
                    configuration.getDefaultDBName(), LIST_PARTI_TABLE_NAME);
            AutoPartitionExtensionPoint extensionPoint = new OBMySQLAutoPartitionExtensionPoint();
            extensionPoint.getPartitionKeyDataTypes(connection, dbTable);
        }
    }

    @Test
    public void unquoteIdentifier_wrappedStr_succeed() {
        List<String> inputs = Arrays.asList("`abcd`", "abcde");
        AutoPartitionExtensionPoint extensionPoint = new OBMySQLAutoPartitionExtensionPoint();
        List<String> actuals = inputs.stream().map(extensionPoint::unquoteIdentifier).collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList("abcd", "abcde"), actuals);
    }

    @Test
    public void generateCreatePartitionDdls_normal_succeed() throws Exception {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            DBTable dbTable = new OBMySQLTableExtension().getDetail(connection,
                    configuration.getDefaultDBName(), REAL_RANGE_TABLE_NAME);
            int generateCount = 5;
            AutoPartitionExtensionPoint extensionPoint = new OBMySQLAutoPartitionExtensionPoint();
            PartitionExprGenerator pk1 = extensionPoint
                    .getPartitionExpressionGeneratorByName("CUSTOM_GENERATOR");
            SqlExprBasedGeneratorConfig config = new SqlExprBasedGeneratorConfig();
            config.setIntervalGenerateExpr("86400");
            config.setGenerateExpr("cast(date_format(from_unixtime(unix_timestamp(STR_TO_DATE("
                    + PartitionPlanVariableKey.LAST_PARTITION_VALUE.getVariable()
                    + ", '%Y%m%d')) + " + PartitionPlanVariableKey.INTERVAL.getVariable()
                    + "), '%Y%m%d') as signed)");
            List<String> pk1Values = pk1.invoke(connection, dbTable,
                    getSqlExprBasedGeneratorParameters(config, generateCount, "datekey"));

            PartitionExprGenerator pk2 = extensionPoint
                    .getPartitionExpressionGeneratorByName("TIME_INCREASING_GENERATOR");
            TimeIncreaseGeneratorConfig config1 = new TimeIncreaseGeneratorConfig();
            long current = 1706180200490L;// 2024-01-25 18:57
            config1.setBaseTimestampMillis(current);
            config1.setInterval(1);
            config1.setIntervalPrecision(TimeDataType.DAY);
            List<String> pk2Values = pk2.invoke(connection, dbTable,
                    getTimeIncreaseGeneratorParameters(config1, generateCount, "c3"));

            PartitionNameGenerator nameGen = extensionPoint
                    .getPartitionNameGeneratorGeneratorByName("CUSTOM_PARTITION_NAME_GENERATOR");
            DBTablePartition partition = new DBTablePartition();
            partition.setPartitionDefinitions(new ArrayList<>());
            for (int i = 0; i < generateCount; i++) {
                DBTablePartitionDefinition definition = new DBTablePartitionDefinition();
                definition.setMaxValues(Arrays.asList(pk1Values.get(i), pk2Values.get(i)));
                SqlExprBasedGeneratorConfig config2 = new SqlExprBasedGeneratorConfig();
                config2.setGenerateExpr("concat('p', date_format(from_unixtime(unix_timestamp("
                        + "STR_TO_DATE(20240125, '%Y%m%d')) + "
                        + PartitionPlanVariableKey.INTERVAL.getVariable() + "), '%Y%m%d'))");
                config2.setIntervalGenerateExpr("86400");
                definition.setName(nameGen.invoke(connection, dbTable,
                        getSqlExprBasedNameGeneratorParameters(i, config2, definition)));
                partition.getPartitionDefinitions().add(definition);
            }
            partition.setPartitionOption(dbTable.getPartition().getPartitionOption());
            partition.setTableName(dbTable.getName());
            partition.setSchemaName(dbTable.getSchemaName());
            List<String> actuals = extensionPoint.generateCreatePartitionDdls(connection, partition);
            List<String> expects = Collections.singletonList(String.format("ALTER TABLE `%s`.`%s` ADD PARTITION (\n"
                    + "\tPARTITION `p20240126` VALUES LESS THAN (20220801,'2024-01-26'),\n"
                    + "\tPARTITION `p20240127` VALUES LESS THAN (20220802,'2024-01-27'),\n"
                    + "\tPARTITION `p20240128` VALUES LESS THAN (20220803,'2024-01-28'),\n"
                    + "\tPARTITION `p20240129` VALUES LESS THAN (20220804,'2024-01-29'),\n"
                    + "\tPARTITION `p20240130` VALUES LESS THAN (20220805,'2024-01-30'));\n",
                    configuration.getDefaultDBName(), REAL_RANGE_TABLE_NAME));
            Assert.assertEquals(expects, actuals);
        }
    }

    @Test
    public void generateDropPartitionDdls_reloadIndexes_succeed() throws Exception {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            DBTable dbTable = new OBMySQLTableExtension().getDetail(connection,
                    configuration.getDefaultDBName(), REAL_RANGE_TABLE_NAME);
            AutoPartitionExtensionPoint extensionPoint = new OBMySQLAutoPartitionExtensionPoint();
            DropPartitionGenerator generator = extensionPoint
                    .getDropPartitionGeneratorByName("KEEP_MOST_LATEST_GENERATOR");
            List<DBTablePartitionDefinition> toDelete = generator.invoke(connection,
                    dbTable, getDropPartitionParameters(1));
            DBTablePartition dbTablePartition = new DBTablePartition();
            dbTablePartition.setPartitionDefinitions(toDelete);
            dbTablePartition.setTableName(dbTable.getName());
            dbTablePartition.setSchemaName(dbTable.getSchemaName());
            List<String> actual = extensionPoint.generateDropPartitionDdls(connection, dbTablePartition, true);
            List<String> expects = Collections.singletonList(String.format("ALTER TABLE `%s`.`%s` DROP PARTITION "
                    + "(`p20220830`, `p20220829`);\n", configuration.getDefaultDBName(), REAL_RANGE_TABLE_NAME));
            Assert.assertEquals(expects, actual);
        }
    }

    @Test
    public void listAllPartitionedTables_listAll_listSucceed() throws SQLException {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            AutoPartitionExtensionPoint extensionPoint = new OBMySQLAutoPartitionExtensionPoint();
            List<DBTable> actual = extensionPoint.listAllPartitionedTables(connection, null,
                    configuration.getDefaultDBName(), null);
            Assert.assertEquals(4, actual.size());
        }
    }

    private Map<String, Object> getDropPartitionParameters(int keepCount) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(KeepMostLatestPartitionGenerator.KEEP_LATEST_COUNT_KEY, keepCount);
        return parameters;
    }

    private Map<String, Object> getSqlExprBasedNameGeneratorParameters(int index, SqlExprBasedGeneratorConfig config,
            DBTablePartitionDefinition definition) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(PartitionNameGenerator.TARGET_PARTITION_DEF_KEY, definition);
        parameters.put(PartitionNameGenerator.TARGET_PARTITION_DEF_INDEX_KEY, index);
        parameters.put(PartitionNameGenerator.PARTITION_NAME_GENERATOR_KEY, config);
        return parameters;
    }

    private Map<String, Object> getSqlExprBasedGeneratorParameters(SqlExprBasedGeneratorConfig config,
            Integer count, String partitionKey) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(PartitionExprGenerator.GENERATOR_PARAMETER_KEY, config);
        parameters.put(PartitionExprGenerator.GENERATE_COUNT_KEY, count);
        parameters.put(PartitionExprGenerator.GENERATOR_PARTITION_KEY, partitionKey);
        return parameters;
    }

    private Map<String, Object> getTimeIncreaseGeneratorParameters(TimeIncreaseGeneratorConfig config,
            Integer count, String partitionKey) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(PartitionExprGenerator.GENERATOR_PARAMETER_KEY, config);
        parameters.put(PartitionExprGenerator.GENERATE_COUNT_KEY, count);
        parameters.put(PartitionExprGenerator.GENERATOR_PARTITION_KEY, partitionKey);
        return parameters;
    }

    private static List<String> getDdlContent() throws IOException {
        String delimiter = "\\$\\$\\s*";
        try (InputStream input = OBMySQLPartitionKeyDataTypeFactoryTest.class.getClassLoader()
                .getResourceAsStream("partitionplan/extension_point_create_table_partition.sql")) {
            byte[] buffer = new byte[input.available()];
            IOUtils.readFully(input, buffer);
            StringSubstitutor substitutor = StringSubstitutor.createInterpolator();
            return new ArrayList<>(Arrays.asList(substitutor.replace(new String(buffer)).split(delimiter)));
        }
    }

}
