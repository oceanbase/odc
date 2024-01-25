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

    public static final String REAL_RANGE_TABLE_NAME = "range_svc_parti_tbl";
    public static final String OVERLAP_RANGE_TABLE_NAME = "range_svc_parti_overlap_tbl";
    @Autowired
    private PartitionPlanServiceV2 partitionPlanService;

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
        oracle.execute("DROP TABLE " + REAL_RANGE_TABLE_NAME);
        oracle.execute("DROP TABLE " + OVERLAP_RANGE_TABLE_NAME);
    }

    @Test
    public void generatePartitionDdl_onlyCreate_generateSucceed() throws Exception {
        PartitionPlanTableConfig tableConfig = new PartitionPlanTableConfig();
        tableConfig.setTableName(REAL_RANGE_TABLE_NAME);
        tableConfig.setPartitionNameInvoker("CUSTOM_PARTITION_NAME_GENERATOR");
        tableConfig.setPartitionNameInvokerParameters(
                getSqlExprBasedNameGeneratorParameters("concat('p', '20240125')"));
        int generateCount = 5;
        PartitionPlanKeyConfig c3Create = getc3CreateConfig(generateCount);
        PartitionPlanKeyConfig datekeyCreate = getdatekeyCreateConfig(generateCount);
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
                    configuration.getDefaultDBName(), REAL_RANGE_TABLE_NAME)));
            Assert.assertEquals(expect, actual);
        }
    }

    @Test
    public void generatePartitionDdl_onlyDrop_generateSucceed() throws Exception {
        PartitionPlanTableConfig tableConfig = new PartitionPlanTableConfig();
        tableConfig.setTableName(REAL_RANGE_TABLE_NAME);
        PartitionPlanKeyConfig dropConfig = getDropConfig();
        tableConfig.setPartitionKeyConfigs(Collections.singletonList(dropConfig));

        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            Map<PartitionPlanStrategy, List<String>> actual = this.partitionPlanService.generatePartitionDdl(
                    connection, DialectType.OB_MYSQL, configuration.getDefaultDBName(), tableConfig);
            Map<PartitionPlanStrategy, List<String>> expect = new HashMap<>();
            expect.put(PartitionPlanStrategy.DROP, Collections.singletonList(String.format(
                    "ALTER TABLE %s.%s DROP PARTITION (p20220830, p20220829) UPDATE GLOBAL INDEXES;",
                    configuration.getDefaultDBName(), REAL_RANGE_TABLE_NAME)));
            Assert.assertEquals(expect, actual);
        }
    }

    @Test
    public void generatePartitionDdl_bothCreateAndDrop_generateSucceed() throws Exception {
        PartitionPlanTableConfig tableConfig = new PartitionPlanTableConfig();
        tableConfig.setTableName(REAL_RANGE_TABLE_NAME);
        tableConfig.setPartitionNameInvoker("CUSTOM_PARTITION_NAME_GENERATOR");
        tableConfig.setPartitionNameInvokerParameters(
                getSqlExprBasedNameGeneratorParameters("concat('p', '20240125')"));
        int generateCount = 5;
        PartitionPlanKeyConfig c3Create = getc3CreateConfig(generateCount);
        PartitionPlanKeyConfig datekeyCreate = getdatekeyCreateConfig(generateCount);
        PartitionPlanKeyConfig dropConfig = getDropConfig();
        tableConfig.setPartitionKeyConfigs(Arrays.asList(c3Create, datekeyCreate, dropConfig));

        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            Map<PartitionPlanStrategy, List<String>> actual = this.partitionPlanService.generatePartitionDdl(
                    connection, DialectType.OB_MYSQL, configuration.getDefaultDBName(), tableConfig);
            Map<PartitionPlanStrategy, List<String>> expect = new HashMap<>();
            expect.put(PartitionPlanStrategy.DROP, Collections.singletonList(String.format(
                    "ALTER TABLE %s.%s DROP PARTITION (p20220830, p20220829) UPDATE GLOBAL INDEXES;",
                    configuration.getDefaultDBName(), REAL_RANGE_TABLE_NAME)));
            expect.put(PartitionPlanStrategy.CREATE, Collections.singletonList(String.format(
                    "ALTER TABLE %s.%s ADD PARTITION (\n"
                            + "\tPARTITION `p20240125` VALUES LESS THAN (20220801,'2024-01-25'),\n"
                            + "\tPARTITION `p20240125` VALUES LESS THAN (20220802,'2024-01-26'),\n"
                            + "\tPARTITION `p20240125` VALUES LESS THAN (20220803,'2024-01-27'),\n"
                            + "\tPARTITION `p20240125` VALUES LESS THAN (20220804,'2024-01-28'),\n"
                            + "\tPARTITION `p20240125` VALUES LESS THAN (20220805,'2024-01-29'));\n",
                    configuration.getDefaultDBName(), REAL_RANGE_TABLE_NAME)));
            Assert.assertEquals(expect, actual);
        }
    }

    @Test
    public void generatePartitionDdl_bothCreateAndDropWithOverlap_generateSucceed() throws Exception {
        PartitionPlanTableConfig tableConfig = new PartitionPlanTableConfig();
        tableConfig.setTableName(OVERLAP_RANGE_TABLE_NAME);
        tableConfig.setPartitionNameInvoker("CUSTOM_PARTITION_NAME_GENERATOR");
        tableConfig.setPartitionNameInvokerParameters(
                getSqlExprBasedNameGeneratorParameters("concat('p', '20240125')"));
        int generateCount = 5;
        PartitionPlanKeyConfig c3Create = getc3CreateConfig(generateCount);
        PartitionPlanKeyConfig datekeyCreate = getdatekeyCreateConfig(generateCount);
        PartitionPlanKeyConfig dropConfig = getDropConfig();
        tableConfig.setPartitionKeyConfigs(Arrays.asList(c3Create, datekeyCreate, dropConfig));

        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            Map<PartitionPlanStrategy, List<String>> actual = this.partitionPlanService.generatePartitionDdl(
                    connection, DialectType.OB_MYSQL, configuration.getDefaultDBName(), tableConfig);
            Map<PartitionPlanStrategy, List<String>> expect = new HashMap<>();
            expect.put(PartitionPlanStrategy.DROP, Collections.singletonList(String.format(
                    "ALTER TABLE %s.%s DROP PARTITION (p20220830, p20220829) UPDATE GLOBAL INDEXES;",
                    configuration.getDefaultDBName(), OVERLAP_RANGE_TABLE_NAME)));
            expect.put(PartitionPlanStrategy.CREATE, Collections.singletonList(String.format(
                    "ALTER TABLE %s.%s ADD PARTITION (\n"
                            + "\tPARTITION `p20240125` VALUES LESS THAN (20220802,'2024-01-26'),\n"
                            + "\tPARTITION `p20240125` VALUES LESS THAN (20220803,'2024-01-27'),\n"
                            + "\tPARTITION `p20240125` VALUES LESS THAN (20220804,'2024-01-28'),\n"
                            + "\tPARTITION `p20240125` VALUES LESS THAN (20220805,'2024-01-29'));\n",
                    configuration.getDefaultDBName(), OVERLAP_RANGE_TABLE_NAME)));
            Assert.assertEquals(expect, actual);
        }
    }

    private PartitionPlanKeyConfig getDropConfig() {
        PartitionPlanKeyConfig dropConfig = new PartitionPlanKeyConfig();
        dropConfig.setPartitionKey(null);
        dropConfig.setStrategy(PartitionPlanStrategy.DROP);
        dropConfig.setPartitionKeyInvoker("KEEP_MOST_RECENT_GENERATOR");
        dropConfig.setPartitionKeyInvokerParameters(getDropPartitionParameters(1));
        return dropConfig;
    }

    private PartitionPlanKeyConfig getc3CreateConfig(int generateCount) {
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

    private PartitionPlanKeyConfig getdatekeyCreateConfig(int generateCount) {
        PartitionPlanKeyConfig datekeyCreate = new PartitionPlanKeyConfig();
        datekeyCreate.setPartitionKey("`datekey`");
        datekeyCreate.setStrategy(PartitionPlanStrategy.CREATE);
        datekeyCreate.setPartitionKeyInvoker("CUSTOM_GENERATOR");
        SqlExprBasedGeneratorConfig config = new SqlExprBasedGeneratorConfig();
        config.setIntervalGenerateExpr("86400");
        config.setPartitionLowerBoundGenerateExpr("cast(date_format(from_unixtime(unix_timestamp(STR_TO_DATE("
                + PartitionPlanVariableKey.LAST_PARTITION_VALUE.getVariable()
                + ", '%Y%m%d')) + " + PartitionPlanVariableKey.INTERVAL.getVariable()
                + "), '%Y%m%d') as signed)");
        datekeyCreate.setPartitionKeyInvokerParameters(
                getSqlExprBasedGeneratorParameters(config, generateCount, "`datekey`"));
        return datekeyCreate;
    }

    private Map<String, Object> getSqlExprBasedNameGeneratorParameters(String expr) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(SqlExprBasedPartitionNameGenerator.PARTITION_NAME_EXPR_KEY, expr);
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

    private static List<String> getDdlContent() throws IOException {
        String delimiter = "\\$\\$\\s*";
        try (InputStream input = PartitionPlanServiceV2Test.class.getClassLoader()
                .getResourceAsStream("partitionplan/service_create_table.sql")) {
            byte[] buffer = new byte[input.available()];
            IOUtils.readFully(input, buffer);
            StringSubstitutor substitutor = StringSubstitutor.createInterpolator();
            return new ArrayList<>(Arrays.asList(substitutor.replace(new String(buffer)).split(delimiter)));
        }
    }

}
