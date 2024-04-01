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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
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
import com.oceanbase.odc.plugin.task.api.partitionplan.datatype.TimeDataType;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.drop.DropPartitionGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.util.TimeDataTypeUtil;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.drop.OBMySQLHistoricalPartitionPlanDropGenerator;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTableAbstractPartitionDefinition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;

/**
 * Test cases for {@link OBMySQLHistoricalPartitionPlanDropGenerator}
 *
 * @author yh263208
 * @date 2024-03-11 16:39
 * @since ODC-release_3.2.4
 */
public class OBMySQLHistoricalPartitionPlanDropGeneratorTest {

    public static final String DATETIME_RANGE_PARTI_TBL = "datetime_range_parti_tbl";
    public static final String UNIXTIMESTAMP_RANGE_PARTI_TBL = "unixtimestamp_range_parti_tbl";

    @BeforeClass
    public static void setUp() throws IOException {
        JdbcTemplate oracle = new JdbcTemplate(TestDBConfigurations.getInstance()
                .getTestOBMysqlConfiguration().getDataSource());
        getDdlContent().forEach(oracle::execute);
    }

    @AfterClass
    public static void clear() {
        JdbcTemplate mysql = new JdbcTemplate(TestDBConfigurations.getInstance()
                .getTestOBMysqlConfiguration().getDataSource());
        mysql.execute("DROP TABLE " + DATETIME_RANGE_PARTI_TBL);
        mysql.execute("DROP TABLE " + UNIXTIMESTAMP_RANGE_PARTI_TBL);
    }

    @Test
    public void generate_datetimeTbl_succeed() throws Exception {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            OBMySQLTableExtension tableExtension = new OBMySQLTableExtension();
            DBTable dbTable = tableExtension.getDetail(connection,
                    configuration.getDefaultDBName(), DATETIME_RANGE_PARTI_TBL);
            DropPartitionGenerator generator = new OBMySQLHistoricalPartitionPlanDropGenerator();
            List<DBTablePartitionDefinition> toDelete = generator.invoke(connection, dbTable, getParameters(2, 3));
            List<String> actuals = toDelete.stream().map(DBTableAbstractPartitionDefinition::getName)
                    .collect(Collectors.toList());
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, -2);
            String bound = new SimpleDateFormat("''yyyy-MM-dd HH:mm:ss''")
                    .format(TimeDataTypeUtil.removeExcessPrecision(calendar.getTime(), TimeDataType.MONTH));
            List<String> expect = dbTable.getPartition().getPartitionDefinitions().stream()
                    .filter(d -> d.getMaxValues().get(0).compareTo(bound) < 0)
                    .map(DBTableAbstractPartitionDefinition::getName).collect(Collectors.toList());
            Assert.assertEquals(expect, actuals);
        }
    }

    @Test
    public void generate_unixTimestampTbl_succeed() throws Exception {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            OBMySQLTableExtension tableExtension = new OBMySQLTableExtension();
            DBTable dbTable = tableExtension.getDetail(connection,
                    configuration.getDefaultDBName(), UNIXTIMESTAMP_RANGE_PARTI_TBL);
            DropPartitionGenerator generator = new OBMySQLHistoricalPartitionPlanDropGenerator();
            List<DBTablePartitionDefinition> toDelete = generator.invoke(connection, dbTable, getParameters(2, 3));
            List<String> actuals = toDelete.stream().map(DBTableAbstractPartitionDefinition::getName)
                    .collect(Collectors.toList());
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, -2);
            Date date = TimeDataTypeUtil.removeExcessPrecision(calendar.getTime(), TimeDataType.MONTH);
            String bound = date.getTime() / 1000 + "";
            List<String> expect = dbTable.getPartition().getPartitionDefinitions().stream()
                    .filter(d -> d.getMaxValues().get(0).compareTo(bound) < 0)
                    .map(DBTableAbstractPartitionDefinition::getName).collect(Collectors.toList());
            Assert.assertEquals(expect, actuals);
        }
    }

    private Map<String, Object> getParameters(int periodUnit, int expirePeriod) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(OBMySQLHistoricalPartitionPlanDropGenerator.PERIOD_UNIT_KEY, periodUnit);
        parameters.put(OBMySQLHistoricalPartitionPlanDropGenerator.EXPIRE_PERIOD_KEY, expirePeriod);
        return parameters;
    }

    private static List<String> getDdlContent() throws IOException {
        String delimiter = "\\$\\$\\s*";
        try (InputStream input = OBMySQLPartitionKeyDataTypeFactoryTest.class.getClassLoader()
                .getResourceAsStream("partitionplan/drop_partition_by_time_create_table_partition.sql")) {
            byte[] buffer = new byte[input.available()];
            IOUtils.readFully(input, buffer);
            StringSubstitutor substitutor = StringSubstitutor.createInterpolator();
            return new ArrayList<>(Arrays.asList(substitutor.replace(new String(buffer)).split(delimiter)));
        }
    }

}
