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
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.drop.DropPartitionGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.drop.KeepMostLatestPartitionGenerator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.drop.OBMySQLKeepLatestPartitionGenerator;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTableAbstractPartitionDefinition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;

/**
 * Test cases for {@link OBMySQLKeepLatestPartitionGenerator}
 *
 * @author yh263208
 * @date 2024-01-24 15:54
 * @since ODC_release_4.2.4
 */
public class OBMySQLKeepLatestPartitionGeneratorTest {

    public static final String RANGE_COLUMNS_DATE_TABLE_NAME = "range_parti_drop_test";

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
    }

    @Test
    public void generate_keepRecentOne_succeed() throws Exception {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            OBMySQLTableExtension tableExtension = new OBMySQLTableExtension();
            DBTable dbTable = tableExtension.getDetail(connection,
                    configuration.getDefaultDBName(), RANGE_COLUMNS_DATE_TABLE_NAME);
            DropPartitionGenerator generator = new OBMySQLKeepLatestPartitionGenerator();
            List<DBTablePartitionDefinition> toDelete = generator.invoke(connection, dbTable, getParameters(1));
            List<String> actuals = toDelete.stream().map(DBTableAbstractPartitionDefinition::getName)
                    .collect(Collectors.toList());
            Assert.assertEquals(Arrays.asList("p2", "p1"), actuals);
        }
    }

    private Map<String, Object> getParameters(int keepCount) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(KeepMostLatestPartitionGenerator.KEEP_LATEST_COUNT_KEY, keepCount);
        return parameters;
    }

    private static List<String> getDdlContent() throws IOException {
        String delimiter = "\\$\\$\\s*";
        try (InputStream input = OBMySQLPartitionKeyDataTypeFactoryTest.class.getClassLoader()
                .getResourceAsStream("partitionplan/drop_partition_create_table_partition.sql")) {
            byte[] buffer = new byte[input.available()];
            IOUtils.readFully(input, buffer);
            StringSubstitutor substitutor = StringSubstitutor.createInterpolator();
            return new ArrayList<>(Arrays.asList(substitutor.replace(new String(buffer)).split(delimiter)));
        }
    }

}
