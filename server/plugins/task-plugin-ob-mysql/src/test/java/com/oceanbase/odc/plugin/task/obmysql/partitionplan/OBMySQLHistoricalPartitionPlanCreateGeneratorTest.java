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
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.create.OBMySQLHistoricalPartitionPlanCreateGenerator;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.tools.dbbrowser.model.DBTable;

/**
 * Test cases for {@link OBMySQLHistoricalPartitionPlanCreateGenerator}
 *
 * @author yh263208
 * @date 2024-03-21 09:45
 * @since ODC_release_4.2.4
 */
public class OBMySQLHistoricalPartitionPlanCreateGeneratorTest {

    public static final String UNIXTIMESTAMP_RANGE_PARTI_TBL = "unixtimestamp_range_parti_tbl_create";

    @BeforeClass
    public static void setUp() throws IOException {
        JdbcTemplate mysql = new JdbcTemplate(TestDBConfigurations.getInstance()
                .getTestOBMysqlConfiguration().getDataSource());
        getDdlContent().forEach(mysql::execute);
    }

    @AfterClass
    public static void clear() {
        JdbcTemplate mysql = new JdbcTemplate(TestDBConfigurations.getInstance()
                .getTestOBMysqlConfiguration().getDataSource());
        mysql.execute("DROP TABLE " + UNIXTIMESTAMP_RANGE_PARTI_TBL);
    }

    @Test
    public void generate_monthPrecMarch_succeed() throws Exception {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            OBMySQLTableExtension tableExtension = new OBMySQLTableExtension();
            DBTable dbTable = tableExtension.getDetail(connection,
                    configuration.getDefaultDBName(), UNIXTIMESTAMP_RANGE_PARTI_TBL);
            AutoPartitionKeyInvoker<List<String>> generator = new OBMySQLHistoricalPartitionPlanCreateGenerator();
            TimeIncreaseGeneratorConfig config = new TimeIncreaseGeneratorConfig();
            long current = 1710950400000L;
            config.setBaseTimestampMillis(current);
            config.setInterval(1);
            config.setIntervalPrecision(TimeDataType.MONTH);
            List<String> actuals = generator.invoke(connection, dbTable,
                    getParameters(config, 5, "UNIX_TIMESTAMP(parti_key)"));
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date(current));
            List<String> expects = new ArrayList<>();
            calendar.add(Calendar.MONTH, 2);
            int i = 0;
            do {
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                expects.add(calendar.getTimeInMillis() / 1000 + "");
                calendar.add(Calendar.MONTH, 1);
            } while ((++i) < 5);
            Assert.assertEquals(expects, actuals);
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
        try (InputStream input = OBMySQLHistoricalPartitionPlanCreateGeneratorTest.class.getClassLoader()
                .getResourceAsStream("partitionplan/unix_ts_partition_generator_create_table_partition.sql")) {
            byte[] buffer = new byte[input.available()];
            IOUtils.readFully(input, buffer);
            StringSubstitutor substitutor = StringSubstitutor.createInterpolator();
            return new ArrayList<>(Arrays.asList(substitutor.replace(new String(buffer)).split(delimiter)));
        }
    }

}
