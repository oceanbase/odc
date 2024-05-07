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
package com.oceanbase.odc.plugin.task.oboracle.partitionplan;

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

import com.oceanbase.odc.plugin.schema.oboracle.OBOracleTableExtension;
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

/**
 * Test cases for {@link OBOracleAutoPartitionExtensionPoint}
 *
 * @author yh263208
 * @date 2024-01-29 17:16
 * @since ODC_release_4.2.4
 */
public class OBOracleAutoPartitionExtensionPointTest {

    public static final String RANGE_TABLE_NAME = "RANGE_PARTI_TIME_TBL100";

    @BeforeClass
    public static void setUp() throws IOException {
        JdbcTemplate oracle = new JdbcTemplate(TestDBConfigurations.getInstance()
                .getTestOBOracleConfiguration().getDataSource());
        getDdlContent().forEach(oracle::execute);
    }

    @AfterClass
    public static void clear() {
        JdbcTemplate oracle = new JdbcTemplate(TestDBConfigurations.getInstance()
                .getTestOBOracleConfiguration().getDataSource());
        oracle.execute("DROP TABLE " + RANGE_TABLE_NAME);
    }

    @Test
    public void getPartitionKeyDataTypes_rangeParti_getSucceed() throws SQLException, IOException {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBOracleConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            DBTable dbTable = new OBOracleTableExtension().getDetail(connection,
                    configuration.getDefaultDBName(), RANGE_TABLE_NAME);
            AutoPartitionExtensionPoint extensionPoint = new OBOracleAutoPartitionExtensionPoint();
            List<DataType> actuals = extensionPoint.getPartitionKeyDataTypes(connection, dbTable);
            Assert.assertEquals(Arrays.asList(new TimeDataType("DATE", TimeDataType.SECOND),
                    new TimeDataType("TIMESTAMP", TimeDataType.SECOND)), actuals);
        }
    }

    @Test
    public void generateCreatePartitionDdls_normal_succeed() throws Exception {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBOracleConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            DBTable dbTable = new OBOracleTableExtension().getDetail(connection,
                    configuration.getDefaultDBName(), RANGE_TABLE_NAME);
            int generateCount = 5;
            AutoPartitionExtensionPoint extensionPoint = new OBOracleAutoPartitionExtensionPoint();
            PartitionExprGenerator pk1 = extensionPoint
                    .getPartitionExpressionGeneratorByName("CUSTOM_GENERATOR");
            SqlExprBasedGeneratorConfig config = new SqlExprBasedGeneratorConfig();
            config.setIntervalGenerateExpr("NUMTOYMINTERVAL(1, 'YEAR')");
            config.setGenerateExpr(PartitionPlanVariableKey.LAST_PARTITION_VALUE.getVariable()
                    + " + " + PartitionPlanVariableKey.INTERVAL.getVariable());
            List<String> pk1Values = pk1.invoke(connection, dbTable,
                    getSqlExprBasedGeneratorParameters(config, generateCount, "c1"));

            PartitionExprGenerator pk2 = extensionPoint
                    .getPartitionExpressionGeneratorByName("TIME_INCREASING_GENERATOR");
            TimeIncreaseGeneratorConfig config1 = new TimeIncreaseGeneratorConfig();
            long current = 1706180200490L;// 2024-01-25 18:57
            config1.setBaseTimestampMillis(current);
            config1.setInterval(1);
            config1.setIntervalPrecision(TimeDataType.DAY);
            List<String> pk2Values = pk2.invoke(connection, dbTable,
                    getTimeIncreaseGeneratorParameters(config1, generateCount, "c2"));

            PartitionNameGenerator nameGen = extensionPoint
                    .getPartitionNameGeneratorGeneratorByName("CUSTOM_PARTITION_NAME_GENERATOR");
            DBTablePartition partition = new DBTablePartition();
            partition.setPartitionDefinitions(new ArrayList<>());
            for (int i = 0; i < generateCount; i++) {
                DBTablePartitionDefinition definition = new DBTablePartitionDefinition();
                definition.setMaxValues(Arrays.asList(pk1Values.get(i), pk2Values.get(i)));
                SqlExprBasedGeneratorConfig config2 = new SqlExprBasedGeneratorConfig();
                config2.setGenerateExpr("CONCAT('P', TO_CHAR(TO_DATE('20240125', 'YYYYMMDD') + "
                        + PartitionPlanVariableKey.INTERVAL.getVariable() + ", 'YYYYMMDD'))");
                config2.setIntervalGenerateExpr("NUMTOYMINTERVAL(1, 'MONTH')");
                definition.setName(nameGen.invoke(connection, dbTable,
                        getSqlExprBasedNameGeneratorParameters(i, config2, definition)));
                partition.getPartitionDefinitions().add(definition);
            }
            partition.setPartitionOption(dbTable.getPartition().getPartitionOption());
            partition.setTableName(dbTable.getName());
            partition.setSchemaName(dbTable.getSchemaName());
            List<String> actuals = extensionPoint.generateCreatePartitionDdls(connection, partition);
            List<String> expects = Collections.singletonList(String.format("ALTER TABLE \"%s\".\"%s\" ADD \n"
                    + "\tPARTITION \"P20240225\" VALUES LESS THAN (TO_DATE(' 2024-12-31 23:59:59', "
                    + "'SYYYY-MM-DD HH24:MI:SS', 'NLS_CALENDAR=GREGORIAN'),Timestamp '2024-01-26 00:00:00'),\n"
                    + "\tPARTITION \"P20240325\" VALUES LESS THAN (TO_DATE(' 2025-12-31 23:59:59', "
                    + "'SYYYY-MM-DD HH24:MI:SS', 'NLS_CALENDAR=GREGORIAN'),Timestamp '2024-01-27 00:00:00'),\n"
                    + "\tPARTITION \"P20240425\" VALUES LESS THAN (TO_DATE(' 2026-12-31 23:59:59', "
                    + "'SYYYY-MM-DD HH24:MI:SS', 'NLS_CALENDAR=GREGORIAN'),Timestamp '2024-01-28 00:00:00'),\n"
                    + "\tPARTITION \"P20240525\" VALUES LESS THAN (TO_DATE(' 2027-12-31 23:59:59', "
                    + "'SYYYY-MM-DD HH24:MI:SS', 'NLS_CALENDAR=GREGORIAN'),Timestamp '2024-01-29 00:00:00'),\n"
                    + "\tPARTITION \"P20240625\" VALUES LESS THAN (TO_DATE(' 2028-12-31 23:59:59', "
                    + "'SYYYY-MM-DD HH24:MI:SS', 'NLS_CALENDAR=GREGORIAN'),Timestamp '2024-01-30 00:00:00');\n",
                    configuration.getDefaultDBName(), RANGE_TABLE_NAME));
            Assert.assertEquals(expects, actuals);
        }
    }

    @Test
    public void unquoteIdentifier_wrappedStr_succeed() {
        List<String> inputs = Arrays.asList("\"abcd\"", "abcde");
        AutoPartitionExtensionPoint extensionPoint = new OBOracleAutoPartitionExtensionPoint();
        List<String> actuals = inputs.stream().map(extensionPoint::unquoteIdentifier).collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList("abcd", "ABCDE"), actuals);
    }

    @Test
    public void generateDropPartitionDdls_reloadIndexes_succeed() throws Exception {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBOracleConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            DBTable dbTable = new OBOracleTableExtension().getDetail(connection,
                    configuration.getDefaultDBName(), RANGE_TABLE_NAME);
            AutoPartitionExtensionPoint extensionPoint = new OBOracleAutoPartitionExtensionPoint();
            DropPartitionGenerator generator = extensionPoint
                    .getDropPartitionGeneratorByName("KEEP_MOST_LATEST_GENERATOR");
            List<DBTablePartitionDefinition> toDelete = generator.invoke(connection,
                    dbTable, getDropPartitionParameters(1));
            DBTablePartition dbTablePartition = new DBTablePartition();
            dbTablePartition.setPartitionDefinitions(toDelete);
            dbTablePartition.setTableName(dbTable.getName());
            dbTablePartition.setSchemaName(dbTable.getSchemaName());
            List<String> actual = extensionPoint.generateDropPartitionDdls(connection, dbTablePartition, true);
            List<String> expects = Collections.singletonList(String.format(
                    "ALTER TABLE \"%s\".\"%s\" DROP PARTITION (\"P0\") UPDATE GLOBAL INDEXES;",
                    configuration.getDefaultDBName(), RANGE_TABLE_NAME));
            Assert.assertEquals(expects, actual);
        }
    }

    @Test
    public void generateDropPartitionDdls_dontReloadIndexes_succeed() throws Exception {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBOracleConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            DBTable dbTable = new OBOracleTableExtension().getDetail(connection,
                    configuration.getDefaultDBName(), RANGE_TABLE_NAME);
            AutoPartitionExtensionPoint extensionPoint = new OBOracleAutoPartitionExtensionPoint();
            DropPartitionGenerator generator = extensionPoint
                    .getDropPartitionGeneratorByName("KEEP_MOST_LATEST_GENERATOR");
            List<DBTablePartitionDefinition> toDelete = generator.invoke(connection,
                    dbTable, getDropPartitionParameters(1));
            DBTablePartition dbTablePartition = new DBTablePartition();
            dbTablePartition.setPartitionDefinitions(toDelete);
            dbTablePartition.setTableName(dbTable.getName());
            dbTablePartition.setSchemaName(dbTable.getSchemaName());
            List<String> actual = extensionPoint.generateDropPartitionDdls(connection, dbTablePartition, false);
            List<String> expects = Collections.singletonList(
                    String.format("ALTER TABLE \"%s\".\"%s\" DROP PARTITION (\"P0\");\n",
                            configuration.getDefaultDBName(), RANGE_TABLE_NAME));
            Assert.assertEquals(expects, actual);
        }
    }

    @Test
    public void listAllPartitionedTables_listAll_listSucceed() throws SQLException {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBOracleConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            AutoPartitionExtensionPoint extensionPoint = new OBOracleAutoPartitionExtensionPoint();
            List<DBTable> actual = extensionPoint.listAllPartitionedTables(connection, null,
                    configuration.getDefaultDBName(), Collections.singletonList(RANGE_TABLE_NAME));
            Assert.assertEquals(1, actual.size());
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
        try (InputStream input = OBOracleAutoPartitionExtensionPointTest.class.getClassLoader()
                .getResourceAsStream("partitionplan/extension_point_create_table_partition.sql")) {
            byte[] buffer = new byte[input.available()];
            IOUtils.readFully(input, buffer);
            StringSubstitutor substitutor = StringSubstitutor.createInterpolator();
            return new ArrayList<>(Arrays.asList(substitutor.replace(new String(buffer)).split(delimiter)));
        }
    }

}
