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

import com.oceanbase.odc.plugin.schema.obmysql.OBMySQLTableExtension;
import com.oceanbase.odc.plugin.task.api.partitionplan.datatype.TimeDataType;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.AutoPartitionKeyInvoker;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.create.PartitionExprGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.TimeIncreaseGeneratorConfig;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.create.OBMySQLTimeIncreasePartitionExprGenerator;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.tools.dbbrowser.model.DBTable;

/**
 * Test cases for {@link OBMySQLTimeIncreasePartitionExprGenerator}
 *
 * @author yh263208
 * @date 2024-01-24 21:48
 * @since ODC_release_4.2.4
 */
public class OBMySQLTimeIncreasePartitionExprGeneratorTest {

    public static final String RANGE_COLUMNS_DATE_TABLE_NAME = "range_column_date_parti_tbl1";
    public static final String RANGE_COLUMNS_DATETIME_TABLE_NAME = "range_column_datetime_parti_tbl1";

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
        oracle.execute("DROP TABLE " + RANGE_COLUMNS_DATE_TABLE_NAME);
        oracle.execute("DROP TABLE " + RANGE_COLUMNS_DATETIME_TABLE_NAME);
    }

    @Test
    public void generate_secondPrecCurrentMillis_succeed() throws Exception {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            OBMySQLTableExtension tableExtension = new OBMySQLTableExtension();
            DBTable dbTable = tableExtension.getDetail(connection,
                    configuration.getDefaultDBName(), RANGE_COLUMNS_DATETIME_TABLE_NAME);
            AutoPartitionKeyInvoker<List<String>> generator = new OBMySQLTimeIncreasePartitionExprGenerator();
            TimeIncreaseGeneratorConfig config = new TimeIncreaseGeneratorConfig();
            config.setFromCurrentTime(true);
            config.setInterval(2);
            config.setIntervalPrecision(TimeDataType.SECOND);
            List<String> actuals = generator.invoke(connection, dbTable, getParameters(config, 5, "c3"));
            Assert.assertEquals(5, actuals.size());
        }
    }

    @Test
    public void generate_hourPrecCurrentMillis_succeed() throws Exception {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            OBMySQLTableExtension tableExtension = new OBMySQLTableExtension();
            DBTable dbTable = tableExtension.getDetail(connection,
                    configuration.getDefaultDBName(), RANGE_COLUMNS_DATETIME_TABLE_NAME);
            AutoPartitionKeyInvoker<List<String>> generator = new OBMySQLTimeIncreasePartitionExprGenerator();
            TimeIncreaseGeneratorConfig config = new TimeIncreaseGeneratorConfig();
            long current = System.currentTimeMillis();
            config.setFromTimestampMillis(current);
            config.setInterval(5);
            config.setIntervalPrecision(TimeDataType.HOUR);
            List<String> actuals = generator.invoke(connection, dbTable, getParameters(config, 5, "c3"));
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date(current));
            List<String> expects = new ArrayList<>();
            int i = 0;
            do {
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH) + 1;
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                expects.add(String.format("'%04d-%02d-%02d %02d:00:00'", year, month, day, hour));
                calendar.add(Calendar.HOUR_OF_DAY, 5);
            } while ((++i) < 5);
            Assert.assertEquals(expects, actuals);
        }
    }

    @Test
    public void generate_dayPrecCurrentMillis_succeed() throws Exception {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            OBMySQLTableExtension tableExtension = new OBMySQLTableExtension();
            DBTable dbTable = tableExtension.getDetail(connection,
                    configuration.getDefaultDBName(), RANGE_COLUMNS_DATETIME_TABLE_NAME);
            AutoPartitionKeyInvoker<List<String>> generator = new OBMySQLTimeIncreasePartitionExprGenerator();
            TimeIncreaseGeneratorConfig config = new TimeIncreaseGeneratorConfig();
            long current = System.currentTimeMillis();
            config.setFromTimestampMillis(current);
            config.setInterval(5);
            config.setIntervalPrecision(TimeDataType.DAY);
            List<String> actuals = generator.invoke(connection, dbTable, getParameters(config, 5, "c3"));
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date(current));
            List<String> expects = new ArrayList<>();
            int i = 0;
            do {
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH) + 1;
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                expects.add(String.format("'%04d-%02d-%02d 00:00:00'", year, month, day));
                calendar.add(Calendar.DAY_OF_MONTH, 5);
            } while ((++i) < 5);
            Assert.assertEquals(expects, actuals);
        }
    }

    @Test
    public void generate_yearPrecCurrentMillis_succeed() throws Exception {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            OBMySQLTableExtension tableExtension = new OBMySQLTableExtension();
            DBTable dbTable = tableExtension.getDetail(connection,
                    configuration.getDefaultDBName(), RANGE_COLUMNS_DATE_TABLE_NAME);
            AutoPartitionKeyInvoker<List<String>> generator = new OBMySQLTimeIncreasePartitionExprGenerator();
            TimeIncreaseGeneratorConfig config = new TimeIncreaseGeneratorConfig();
            long current = System.currentTimeMillis();
            config.setFromTimestampMillis(current);
            config.setInterval(5);
            config.setIntervalPrecision(TimeDataType.YEAR);
            List<String> actuals = generator.invoke(connection, dbTable, getParameters(config, 5, "c3"));
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date(current));
            List<String> expects = new ArrayList<>();
            int i = 0;
            do {
                int year = calendar.get(Calendar.YEAR);
                expects.add(String.format("'%04d-01-01'", year));
                calendar.add(Calendar.YEAR, 5);
            } while ((++i) < 5);
            Assert.assertEquals(expects, actuals);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void generate_illegalPrec_failed() throws Exception {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            OBMySQLTableExtension tableExtension = new OBMySQLTableExtension();
            DBTable dbTable = tableExtension.getDetail(connection,
                    configuration.getDefaultDBName(), RANGE_COLUMNS_DATE_TABLE_NAME);
            AutoPartitionKeyInvoker<List<String>> generator = new OBMySQLTimeIncreasePartitionExprGenerator();
            TimeIncreaseGeneratorConfig config = new TimeIncreaseGeneratorConfig();
            long current = System.currentTimeMillis();
            config.setFromTimestampMillis(current);
            config.setInterval(5);
            config.setIntervalPrecision(TimeDataType.SECOND);
            generator.invoke(connection, dbTable, getParameters(config, 5, "c3"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void generate_illegalDataType_failed() throws Exception {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            OBMySQLTableExtension tableExtension = new OBMySQLTableExtension();
            DBTable dbTable = tableExtension.getDetail(connection,
                    configuration.getDefaultDBName(), RANGE_COLUMNS_DATE_TABLE_NAME);
            AutoPartitionKeyInvoker<List<String>> generator = new OBMySQLTimeIncreasePartitionExprGenerator();
            TimeIncreaseGeneratorConfig config = new TimeIncreaseGeneratorConfig();
            long current = System.currentTimeMillis();
            config.setFromTimestampMillis(current);
            config.setInterval(5);
            config.setIntervalPrecision(TimeDataType.SECOND);
            generator.invoke(connection, dbTable, getParameters(config, 5, "c2"));
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
        try (InputStream input = OBMySQLPartitionKeyDataTypeFactoryTest.class.getClassLoader()
                .getResourceAsStream("partitionplan/time_partition_generator_create_table_partition.sql")) {
            byte[] buffer = new byte[input.available()];
            IOUtils.readFully(input, buffer);
            StringSubstitutor substitutor = StringSubstitutor.createInterpolator();
            return new ArrayList<>(Arrays.asList(substitutor.replace(new String(buffer)).split(delimiter)));
        }
    }

}
