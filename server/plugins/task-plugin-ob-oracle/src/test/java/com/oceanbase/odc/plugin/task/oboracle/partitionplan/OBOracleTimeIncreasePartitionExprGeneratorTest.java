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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.plugin.schema.oboracle.OBOracleTableExtension;
import com.oceanbase.odc.plugin.task.api.partitionplan.datatype.TimeDataType;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.AutoPartitionKeyInvoker;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.create.PartitionExprGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.TimeIncreaseGeneratorConfig;
import com.oceanbase.odc.plugin.task.oboracle.partitionplan.invoker.create.OBOracleTimeIncreasePartitionExprGenerator;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.tools.dbbrowser.model.DBTable;

/**
 * Test cases for {@link OBOracleTimeIncreasePartitionExprGenerator}
 *
 * @author yh263208
 * @date 2024-01-29 14:51
 * @since ODC_release_4.2.4
 */
public class OBOracleTimeIncreasePartitionExprGeneratorTest {

    public static final String RANGE_TABLE_NAME = "RANGE_PARTI_TIME_TBL";

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
    public void generate_secondPrecCurrentMillis_succeed() throws Exception {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBOracleConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            OBOracleTableExtension tableExtension = new OBOracleTableExtension();
            DBTable dbTable = tableExtension.getDetail(connection,
                    configuration.getDefaultDBName(), RANGE_TABLE_NAME);
            AutoPartitionKeyInvoker<List<String>> generator = new OBOracleTimeIncreasePartitionExprGenerator();
            TimeIncreaseGeneratorConfig config = new TimeIncreaseGeneratorConfig();
            config.setFromCurrentTime(true);
            config.setInterval(2);
            config.setIntervalPrecision(TimeDataType.SECOND);
            List<String> actuals = generator.invoke(connection, dbTable, getParameters(config, 5, "C1"));
            Assert.assertEquals(5, actuals.size());
        }
    }

    @Test
    public void generate_hourPrecCurrentMillis_succeed() throws Exception {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBOracleConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            OBOracleTableExtension tableExtension = new OBOracleTableExtension();
            DBTable dbTable = tableExtension.getDetail(connection,
                    configuration.getDefaultDBName(), RANGE_TABLE_NAME);
            AutoPartitionKeyInvoker<List<String>> generator = new OBOracleTimeIncreasePartitionExprGenerator();
            TimeIncreaseGeneratorConfig config = new TimeIncreaseGeneratorConfig();
            long current = System.currentTimeMillis();
            config.setBaseTimestampMillis(current);
            config.setInterval(5);
            config.setIntervalPrecision(TimeDataType.HOUR);
            List<String> actuals = generator.invoke(connection, dbTable, getParameters(config, 5, "C2"));
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date(current));
            List<String> expects = new ArrayList<>();
            calendar.add(Calendar.HOUR_OF_DAY, 5);
            int i = 0;
            do {
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH) + 1;
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                expects.add(String.format("Timestamp '%04d-%02d-%02d %02d:00:00'", year, month, day, hour));
                calendar.add(Calendar.HOUR_OF_DAY, 5);
            } while ((++i) < 5);
            Assert.assertEquals(expects, actuals);
        }
    }

    @Test
    public void generate_yearPrecCurrentMillis_succeed() throws Exception {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBOracleConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            OBOracleTableExtension tableExtension = new OBOracleTableExtension();
            DBTable dbTable = tableExtension.getDetail(connection,
                    configuration.getDefaultDBName(), RANGE_TABLE_NAME);
            AutoPartitionKeyInvoker<List<String>> generator = new OBOracleTimeIncreasePartitionExprGenerator();
            TimeIncreaseGeneratorConfig config = new TimeIncreaseGeneratorConfig();
            long current = System.currentTimeMillis();
            config.setBaseTimestampMillis(current);
            config.setInterval(5);
            config.setIntervalPrecision(TimeDataType.MONTH);
            List<String> actuals = generator.invoke(connection, dbTable, getParameters(config, 5, "C1"));
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date(current));
            List<String> expects = new ArrayList<>();
            calendar.add(Calendar.MONTH, 5);
            int i = 0;
            do {
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH) + 1;
                expects.add(String.format(
                        "TO_DATE(' %04d-%02d-01 00:00:00', 'SYYYY-MM-DD HH24:MI:SS', 'NLS_CALENDAR=GREGORIAN')",
                        year, month));
                calendar.add(Calendar.MONTH, 5);
            } while ((++i) < 5);
            Assert.assertEquals(expects, actuals);
        }
    }

    @Test(expected = BadRequestException.class)
    public void generate_illegalDataType_failed() throws Exception {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBOracleConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            OBOracleTableExtension tableExtension = new OBOracleTableExtension();
            DBTable dbTable = tableExtension.getDetail(connection,
                    configuration.getDefaultDBName(), RANGE_TABLE_NAME);
            AutoPartitionKeyInvoker<List<String>> generator = new OBOracleTimeIncreasePartitionExprGenerator();
            TimeIncreaseGeneratorConfig config = new TimeIncreaseGeneratorConfig();
            long current = System.currentTimeMillis();
            config.setBaseTimestampMillis(current);
            config.setInterval(5);
            config.setIntervalPrecision(TimeDataType.SECOND);
            generator.invoke(connection, dbTable, getParameters(config, 5, "C5"));
        }
    }

    private Map<String, Object> getParameters(TimeIncreaseGeneratorConfig config, Integer count, String partitionKey) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(PartitionExprGenerator.GENERATOR_PARAMETER_KEY, config);
        parameters.put(PartitionExprGenerator.GENERATE_COUNT_KEY, count);
        parameters.put(PartitionExprGenerator.GENERATOR_PARTITION_KEY, partitionKey);
        return parameters;
    }

    private static List<String> getDdlContent() throws IOException {
        String delimiter = "\\$\\$\\s*";
        try (InputStream input = OBOraclePartitionKeyDataTypeFactoryTest.class.getClassLoader()
                .getResourceAsStream("partitionplan/time_partition_generator_create_table_partition.sql")) {
            byte[] buffer = new byte[input.available()];
            IOUtils.readFully(input, buffer);
            StringSubstitutor substitutor = StringSubstitutor.createInterpolator();
            return new ArrayList<>(Arrays.asList(substitutor.replace(new String(buffer)).split(delimiter)));
        }
    }

}
