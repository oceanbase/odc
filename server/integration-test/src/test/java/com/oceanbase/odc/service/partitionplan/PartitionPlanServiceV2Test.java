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
package com.oceanbase.odc.service.partitionplan;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.plugin.task.api.partitionplan.datatype.TimeDataType;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.create.PartitionExprGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.drop.KeepMostRecentPartitionGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.partitionname.SqlExprBasedPartitionNameGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.PartitionPlanVariableKey;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.SqlExprBasedGeneratorConfig;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.TimeIncreaseGeneratorConfig;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanKeyConfig;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanStrategy;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanTableConfig;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;

/**
 * Test cases for {@link PartitionPlanServiceV2}
 *
 * @author yh263208
 * @date 2024-01-25 20:33
 * @since ODC_release_4.2.4
 */
public class PartitionPlanServiceV2Test extends ServiceTestEnv {

    public static final String MYSQL_REAL_RANGE_TABLE_NAME = "range_svc_parti_tbl";
    public static final String MYSQL_OVERLAP_RANGE_TABLE_NAME = "range_svc_parti_overlap_tbl";
    public static final String ORACLE_RANGE_TABLE_NAME = "RANGE_SVC_PARTI_TBL";
    @Autowired
    private PartitionPlanServiceV2 partitionPlanService;

    @BeforeClass
    public static void setUp() throws IOException {
        JdbcTemplate mysql = new JdbcTemplate(TestDBConfigurations.getInstance()
                .getTestOBMysqlConfiguration().getDataSource());
        getOBMysqlDdlContent().forEach(mysql::execute);
        JdbcTemplate oracle = new JdbcTemplate(TestDBConfigurations.getInstance()
                .getTestOBOracleConfiguration().getDataSource());
        getOBOracleDdlContent().forEach(oracle::execute);
    }

    @AfterClass
    public static void clear() {
        JdbcTemplate mysql = new JdbcTemplate(TestDBConfigurations.getInstance()
                .getTestOBMysqlConfiguration().getDataSource());
        mysql.execute("DROP TABLE " + MYSQL_REAL_RANGE_TABLE_NAME);
        mysql.execute("DROP TABLE " + MYSQL_OVERLAP_RANGE_TABLE_NAME);
        JdbcTemplate oracle = new JdbcTemplate(TestDBConfigurations.getInstance()
                .getTestOBOracleConfiguration().getDataSource());
        oracle.execute("DROP TABLE " + ORACLE_RANGE_TABLE_NAME);
    }

    @Test
    public void generatePartitionDdl_mysqlOnlyCreate_generateSucceed() throws Exception {
        PartitionPlanTableConfig tableConfig = new PartitionPlanTableConfig();
        tableConfig.setTableName(MYSQL_REAL_RANGE_TABLE_NAME);
        tableConfig.setPartitionNameInvoker("CUSTOM_PARTITION_NAME_GENERATOR");
        SqlExprBasedGeneratorConfig config = new SqlExprBasedGeneratorConfig();
        config.setGenerateExpr("concat('p', date_format(from_unixtime(unix_timestamp("
                + "STR_TO_DATE(20240125, '%Y%m%d'))), '%Y%m%d'))");
        tableConfig.setPartitionNameInvokerParameters(getSqlExprBasedNameGeneratorParameters(config));
        int generateCount = 5;
        PartitionPlanKeyConfig c3Create = getMysqlc3CreateConfig(generateCount);
        PartitionPlanKeyConfig datekeyCreate = getMysqldatekeyCreateConfig(generateCount);
        tableConfig.setPartitionKeyConfigs(Arrays.asList(c3Create, datekeyCreate));

        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            Map<PartitionPlanStrategy, List<String>> actual = this.partitionPlanService.generatePartitionDdl(
                    connection, DialectType.OB_MYSQL, configuration.getDefaultDBName(), tableConfig);
            Map<PartitionPlanStrategy, List<String>> expect = new HashMap<>();
            expect.put(PartitionPlanStrategy.CREATE, Collections.singletonList(String.format(
                    "ALTER TABLE %s.%s ADD PARTITION (\n"
                            + "\tPARTITION `p20240125` VALUES LESS THAN (20220801,'2024-01-25'),\n"
                            + "\tPARTITION `p20240125` VALUES LESS THAN (20220802,'2024-01-26'),\n"
                            + "\tPARTITION `p20240125` VALUES LESS THAN (20220803,'2024-01-27'),\n"
                            + "\tPARTITION `p20240125` VALUES LESS THAN (20220804,'2024-01-28'),\n"
                            + "\tPARTITION `p20240125` VALUES LESS THAN (20220805,'2024-01-29'));\n",
                    configuration.getDefaultDBName(), MYSQL_REAL_RANGE_TABLE_NAME)));
            Assert.assertEquals(expect, actual);
        }
    }

    @Test
    public void generatePartitionDdl_mysqlOnlyDrop_generateSucceed() throws Exception {
        PartitionPlanTableConfig tableConfig = new PartitionPlanTableConfig();
        tableConfig.setTableName(MYSQL_REAL_RANGE_TABLE_NAME);
        PartitionPlanKeyConfig dropConfig = getDropConfig();
        tableConfig.setPartitionKeyConfigs(Collections.singletonList(dropConfig));

        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            Map<PartitionPlanStrategy, List<String>> actual = this.partitionPlanService.generatePartitionDdl(
                    connection, DialectType.OB_MYSQL, configuration.getDefaultDBName(), tableConfig);
            Map<PartitionPlanStrategy, List<String>> expect = new HashMap<>();
            expect.put(PartitionPlanStrategy.DROP, Collections.singletonList(String.format(
                    "ALTER TABLE %s.%s DROP PARTITION (p20220830, p20220829);\n",
                    configuration.getDefaultDBName(), MYSQL_REAL_RANGE_TABLE_NAME)));
            Assert.assertEquals(expect, actual);
        }
    }

    @Test
    public void generatePartitionDdl_mysqlBothCreateAndDrop_generateSucceed() throws Exception {
        PartitionPlanTableConfig tableConfig = new PartitionPlanTableConfig();
        tableConfig.setTableName(MYSQL_REAL_RANGE_TABLE_NAME);
        tableConfig.setPartitionNameInvoker("CUSTOM_PARTITION_NAME_GENERATOR");
        SqlExprBasedGeneratorConfig config = new SqlExprBasedGeneratorConfig();
        config.setGenerateExpr("concat('p', date_format(from_unixtime(unix_timestamp("
                + "STR_TO_DATE(20240125, '%Y%m%d')) + "
                + PartitionPlanVariableKey.INTERVAL.getVariable() + "), '%Y%m%d'))");
        config.setIntervalGenerateExpr("86400");
        tableConfig.setPartitionNameInvokerParameters(getSqlExprBasedNameGeneratorParameters(config));
        int generateCount = 5;
        PartitionPlanKeyConfig c3Create = getMysqlc3CreateConfig(generateCount);
        PartitionPlanKeyConfig datekeyCreate = getMysqldatekeyCreateConfig(generateCount);
        PartitionPlanKeyConfig dropConfig = getDropConfig();
        tableConfig.setPartitionKeyConfigs(Arrays.asList(c3Create, datekeyCreate, dropConfig));

        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            Map<PartitionPlanStrategy, List<String>> actual = this.partitionPlanService.generatePartitionDdl(
                    connection, DialectType.OB_MYSQL, configuration.getDefaultDBName(), tableConfig);
            Map<PartitionPlanStrategy, List<String>> expect = new HashMap<>();
            expect.put(PartitionPlanStrategy.DROP, Collections.singletonList(String.format(
                    "ALTER TABLE %s.%s DROP PARTITION (p20220830, p20220829);\n",
                    configuration.getDefaultDBName(), MYSQL_REAL_RANGE_TABLE_NAME)));
            expect.put(PartitionPlanStrategy.CREATE, Collections.singletonList(String.format(
                    "ALTER TABLE %s.%s ADD PARTITION (\n"
                            + "\tPARTITION `p20240126` VALUES LESS THAN (20220801,'2024-01-25'),\n"
                            + "\tPARTITION `p20240127` VALUES LESS THAN (20220802,'2024-01-26'),\n"
                            + "\tPARTITION `p20240128` VALUES LESS THAN (20220803,'2024-01-27'),\n"
                            + "\tPARTITION `p20240129` VALUES LESS THAN (20220804,'2024-01-28'),\n"
                            + "\tPARTITION `p20240130` VALUES LESS THAN (20220805,'2024-01-29'));\n",
                    configuration.getDefaultDBName(), MYSQL_REAL_RANGE_TABLE_NAME)));
            Assert.assertEquals(expect, actual);
        }
    }

    @Test
    public void generatePartitionDdl_oracleBothCreateAndDropWithOverlap_generateSucceed() throws Exception {
        PartitionPlanTableConfig tableConfig = new PartitionPlanTableConfig();
        tableConfig.setTableName(ORACLE_RANGE_TABLE_NAME);
        tableConfig.setPartitionNameInvoker("CUSTOM_PARTITION_NAME_GENERATOR");
        SqlExprBasedGeneratorConfig config = new SqlExprBasedGeneratorConfig();
        config.setGenerateExpr("CONCAT('P', TO_CHAR(TO_DATE('20240125', 'YYYYMMDD') + "
                + PartitionPlanVariableKey.INTERVAL.getVariable() + ", 'YYYYMMDD'))");
        config.setIntervalGenerateExpr("NUMTOYMINTERVAL(1, 'MONTH')");
        tableConfig.setPartitionNameInvokerParameters(getSqlExprBasedNameGeneratorParameters(config));

        int generateCount = 5;
        PartitionPlanKeyConfig c1Create = getOraclec1CreateConfig(generateCount);
        PartitionPlanKeyConfig c2Create = getOraclec2CreateConfig(generateCount);
        PartitionPlanKeyConfig dropConfig = getDropConfig();
        tableConfig.setPartitionKeyConfigs(Arrays.asList(c1Create, c2Create, dropConfig));

        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBOracleConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            Map<PartitionPlanStrategy, List<String>> actual = this.partitionPlanService.generatePartitionDdl(
                    connection, DialectType.OB_ORACLE, configuration.getDefaultDBName(), tableConfig);
            Map<PartitionPlanStrategy, List<String>> expect = new HashMap<>();
            expect.put(PartitionPlanStrategy.DROP, Collections.singletonList(String.format(
                    "ALTER TABLE %s.%s DROP PARTITION (P1, P0) UPDATE GLOBAL INDEXES;",
                    configuration.getDefaultDBName(), ORACLE_RANGE_TABLE_NAME)));
            expect.put(PartitionPlanStrategy.CREATE, Collections.singletonList(String.format("ALTER TABLE %s.%s ADD \n"
                    + "\tPARTITION \"P20240225\" VALUES LESS THAN (TO_DATE(' 2024-01-25 00:00:00', "
                    + "'SYYYY-MM-DD HH24:MI:SS', 'NLS_CALENDAR=GREGORIAN'),Timestamp '2025-12-31 23:59:59'),\n"
                    + "\tPARTITION \"P20240325\" VALUES LESS THAN (TO_DATE(' 2024-01-26 00:00:00', "
                    + "'SYYYY-MM-DD HH24:MI:SS', 'NLS_CALENDAR=GREGORIAN'),Timestamp '2026-12-31 23:59:59'),\n"
                    + "\tPARTITION \"P20240525\" VALUES LESS THAN (TO_DATE(' 2024-01-28 00:00:00', "
                    + "'SYYYY-MM-DD HH24:MI:SS', 'NLS_CALENDAR=GREGORIAN'),Timestamp '2028-12-31 23:59:59'),\n"
                    + "\tPARTITION \"P20240625\" VALUES LESS THAN (TO_DATE(' 2024-01-29 00:00:00', "
                    + "'SYYYY-MM-DD HH24:MI:SS', 'NLS_CALENDAR=GREGORIAN'),Timestamp '2029-12-31 23:59:59');\n",
                    configuration.getDefaultDBName(), ORACLE_RANGE_TABLE_NAME)));
            Assert.assertEquals(expect, actual);
        }
    }

    @Test
    public void generatePartitionDdl_mysqlBothCreateAndDropWithOverlap_generateSucceed() throws Exception {
        PartitionPlanTableConfig tableConfig = new PartitionPlanTableConfig();
        tableConfig.setTableName(MYSQL_OVERLAP_RANGE_TABLE_NAME);
        tableConfig.setPartitionNameInvoker("CUSTOM_PARTITION_NAME_GENERATOR");
        SqlExprBasedGeneratorConfig config = new SqlExprBasedGeneratorConfig();
        config.setGenerateExpr("concat('p', date_format(from_unixtime(unix_timestamp("
                + "STR_TO_DATE(20240125, '%Y%m%d')) + "
                + PartitionPlanVariableKey.INTERVAL.getVariable() + "), '%Y%m%d'))");
        config.setIntervalGenerateExpr("86400");
        tableConfig.setPartitionNameInvokerParameters(getSqlExprBasedNameGeneratorParameters(config));
        int generateCount = 5;
        PartitionPlanKeyConfig c3Create = getMysqlc3CreateConfig(generateCount);
        PartitionPlanKeyConfig datekeyCreate = getMysqldatekeyCreateConfig(generateCount);
        PartitionPlanKeyConfig dropConfig = getDropConfig();
        tableConfig.setPartitionKeyConfigs(Arrays.asList(c3Create, datekeyCreate, dropConfig));

        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            Map<PartitionPlanStrategy, List<String>> actual = this.partitionPlanService.generatePartitionDdl(
                    connection, DialectType.OB_MYSQL, configuration.getDefaultDBName(), tableConfig);
            Map<PartitionPlanStrategy, List<String>> expect = new HashMap<>();
            expect.put(PartitionPlanStrategy.DROP, Collections.singletonList(String.format(
                    "ALTER TABLE %s.%s DROP PARTITION (p20220830, p20220829);\n",
                    configuration.getDefaultDBName(), MYSQL_OVERLAP_RANGE_TABLE_NAME)));
            expect.put(PartitionPlanStrategy.CREATE, Collections.singletonList(String.format(
                    "ALTER TABLE %s.%s ADD PARTITION (\n"
                            + "\tPARTITION `p20240127` VALUES LESS THAN (20220802,'2024-01-26'),\n"
                            + "\tPARTITION `p20240128` VALUES LESS THAN (20220803,'2024-01-27'),\n"
                            + "\tPARTITION `p20240129` VALUES LESS THAN (20220804,'2024-01-28'));\n",
                    configuration.getDefaultDBName(), MYSQL_OVERLAP_RANGE_TABLE_NAME)));
            Assert.assertEquals(expect, actual);
        }
    }

    private PartitionPlanKeyConfig getDropConfig() {
        PartitionPlanKeyConfig dropConfig = new PartitionPlanKeyConfig();
        dropConfig.setPartitionKey(null);
        dropConfig.setStrategy(PartitionPlanStrategy.DROP);
        dropConfig.setPartitionKeyInvoker("KEEP_MOST_LATEST_GENERATOR");
        dropConfig.setPartitionKeyInvokerParameters(getDropPartitionParameters(1));
        return dropConfig;
    }

    private PartitionPlanKeyConfig getMysqlc3CreateConfig(int generateCount) {
        PartitionPlanKeyConfig c3Create = new PartitionPlanKeyConfig();
        c3Create.setPartitionKey("c3");
        c3Create.setStrategy(PartitionPlanStrategy.CREATE);
        c3Create.setPartitionKeyInvoker("TIME_INCREASING_GENERATOR");
        TimeIncreaseGeneratorConfig config1 = new TimeIncreaseGeneratorConfig();
        long current = 1706180200490L;// 2024-01-25 18:57
        config1.setFromTimestampMillis(current);
        config1.setInterval(1);
        config1.setIntervalPrecision(TimeDataType.DAY);
        c3Create.setPartitionKeyInvokerParameters(getTimeIncreaseGeneratorParameters(config1, generateCount, "c3"));
        return c3Create;
    }

    private PartitionPlanKeyConfig getOraclec1CreateConfig(int generateCount) {
        PartitionPlanKeyConfig c1Create = new PartitionPlanKeyConfig();
        c1Create.setPartitionKey("c1");
        c1Create.setStrategy(PartitionPlanStrategy.CREATE);
        c1Create.setPartitionKeyInvoker("TIME_INCREASING_GENERATOR");
        TimeIncreaseGeneratorConfig config1 = new TimeIncreaseGeneratorConfig();
        long current = 1706180200490L;// 2024-01-25 18:57
        config1.setFromTimestampMillis(current);
        config1.setInterval(1);
        config1.setIntervalPrecision(TimeDataType.DAY);
        c1Create.setPartitionKeyInvokerParameters(getTimeIncreaseGeneratorParameters(config1, generateCount, "c1"));
        return c1Create;
    }

    private PartitionPlanKeyConfig getMysqldatekeyCreateConfig(int generateCount) {
        PartitionPlanKeyConfig datekeyCreate = new PartitionPlanKeyConfig();
        datekeyCreate.setPartitionKey("`datekey`");
        datekeyCreate.setStrategy(PartitionPlanStrategy.CREATE);
        datekeyCreate.setPartitionKeyInvoker("CUSTOM_GENERATOR");
        SqlExprBasedGeneratorConfig config = new SqlExprBasedGeneratorConfig();
        config.setIntervalGenerateExpr("86400");
        config.setGenerateExpr("cast(date_format(from_unixtime(unix_timestamp(STR_TO_DATE("
                + PartitionPlanVariableKey.LAST_PARTITION_VALUE.getVariable()
                + ", '%Y%m%d')) + " + PartitionPlanVariableKey.INTERVAL.getVariable()
                + "), '%Y%m%d') as signed)");
        datekeyCreate.setPartitionKeyInvokerParameters(
                getSqlExprBasedGeneratorParameters(config, generateCount, "`datekey`"));
        return datekeyCreate;
    }

    private PartitionPlanKeyConfig getOraclec2CreateConfig(int generateCount) {
        PartitionPlanKeyConfig c2Create = new PartitionPlanKeyConfig();
        c2Create.setPartitionKey("\"C2\"");
        c2Create.setStrategy(PartitionPlanStrategy.CREATE);
        c2Create.setPartitionKeyInvoker("CUSTOM_GENERATOR");
        SqlExprBasedGeneratorConfig config = new SqlExprBasedGeneratorConfig();
        config.setIntervalGenerateExpr("NUMTOYMINTERVAL(1, 'YEAR')");
        config.setGenerateExpr(PartitionPlanVariableKey.LAST_PARTITION_VALUE.getVariable()
                + " + " + PartitionPlanVariableKey.INTERVAL.getVariable());
        c2Create.setPartitionKeyInvokerParameters(
                getSqlExprBasedGeneratorParameters(config, generateCount, "c2"));
        return c2Create;
    }

    private Map<String, Object> getSqlExprBasedNameGeneratorParameters(SqlExprBasedGeneratorConfig config) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(SqlExprBasedPartitionNameGenerator.PARTITION_NAME_GEN_CONFIG_KEY, config);
        return parameters;
    }

    private Map<String, Object> getDropPartitionParameters(int keepCount) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(KeepMostRecentPartitionGenerator.KEEP_RECENT_COUNT_KEY, keepCount);
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

    private static List<String> getOBMysqlDdlContent() throws IOException {
        String delimiter = "\\$\\$\\s*";
        try (InputStream input = PartitionPlanServiceV2Test.class.getClassLoader()
                .getResourceAsStream("partitionplan/obmysql/service_create_table.sql")) {
            byte[] buffer = new byte[input.available()];
            IOUtils.readFully(input, buffer);
            StringSubstitutor substitutor = StringSubstitutor.createInterpolator();
            return new ArrayList<>(Arrays.asList(substitutor.replace(new String(buffer)).split(delimiter)));
        }
    }

    private static List<String> getOBOracleDdlContent() throws IOException {
        String delimiter = "\\$\\$\\s*";
        try (InputStream input = PartitionPlanServiceV2Test.class.getClassLoader()
                .getResourceAsStream("partitionplan/oboracle/service_create_table.sql")) {
            byte[] buffer = new byte[input.available()];
            IOUtils.readFully(input, buffer);
            StringSubstitutor substitutor = StringSubstitutor.createInterpolator();
            return new ArrayList<>(Arrays.asList(substitutor.replace(new String(buffer)).split(delimiter)));
        }
    }

}
