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

import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.partitionname.PartitionNameGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.DateBasedPartitionNameGeneratorConfig;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.NamingSuffixStrategy;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.partitionname.OBMySQLDateBasedPartitionNameGenerator;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;

/**
 * Test cases for {@link OBMySQLDateBasedPartitionNameGenerator}
 *
 * @author yh263208
 * @date 2024-01-25 15:42
 * @since ODC_release_4.2.4
 */
public class OBMySQLDateBasedPartitionNameGeneratorTest {

    @Test
    public void generate_refUpperBound_succeed() throws Exception {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            DBTable dbTable = new DBTable();
            DBTablePartition partition = new DBTablePartition();
            DBTablePartitionOption option = new DBTablePartitionOption();
            option.setColumnNames(Collections.singletonList("col"));
            partition.setPartitionOption(option);
            DBTablePartitionDefinition definition = new DBTablePartitionDefinition();
            definition.setMaxValues(Collections.singletonList("'2024-03-01 00:00:00'"));
            partition.setPartitionDefinitions(Collections.singletonList(definition));
            dbTable.setPartition(partition);
            DateBasedPartitionNameGeneratorConfig config = new DateBasedPartitionNameGeneratorConfig();
            config.setNamingPrefix("p");
            config.setNamingSuffixExpression("yyyyMMdd");
            config.setRefPartitionKey("col");
            PartitionNameGenerator generator = new OBMySQLDateBasedPartitionNameGenerator();
            String actual = generator.invoke(connection, dbTable, getParameters(config));
            String expect = "p20240302";
            Assert.assertEquals(expect, actual);
        }
    }

    @Test
    public void generate_refLowerBound_succeed() throws Exception {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            DBTable dbTable = new DBTable();
            DBTablePartition partition = new DBTablePartition();
            DBTablePartitionOption option = new DBTablePartitionOption();
            option.setColumnNames(Collections.singletonList("col"));
            partition.setPartitionOption(option);
            DBTablePartitionDefinition definition = new DBTablePartitionDefinition();
            definition.setMaxValues(Collections.singletonList("'2024-03-01 00:00:00'"));
            partition.setPartitionDefinitions(Collections.singletonList(definition));
            dbTable.setPartition(partition);
            DateBasedPartitionNameGeneratorConfig config = new DateBasedPartitionNameGeneratorConfig();
            config.setNamingPrefix("p");
            config.setNamingSuffixExpression("yyyyMMdd");
            config.setRefPartitionKey("col");
            config.setNamingSuffixStrategy(NamingSuffixStrategy.PARTITION_LOWER_BOUND);
            PartitionNameGenerator generator = new OBMySQLDateBasedPartitionNameGenerator();
            String actual = generator.invoke(connection, dbTable, getParameters(config));
            String expect = "p20240301";
            Assert.assertEquals(expect, actual);
        }
    }

    private Map<String, Object> getParameters(DateBasedPartitionNameGeneratorConfig config) {
        Map<String, Object> parameters = new HashMap<>();
        DBTablePartitionDefinition definition = new DBTablePartitionDefinition();
        definition.setMaxValues(Collections.singletonList("'2024-03-02 00:00:00'"));
        parameters.put(PartitionNameGenerator.TARGET_PARTITION_DEF_KEY, Collections.singletonList(definition));
        parameters.put(PartitionNameGenerator.TARGET_PARTITION_DEF_INDEX_KEY, 0);
        parameters.put(PartitionNameGenerator.PARTITION_NAME_GENERATOR_KEY, config);
        return parameters;
    }

}
