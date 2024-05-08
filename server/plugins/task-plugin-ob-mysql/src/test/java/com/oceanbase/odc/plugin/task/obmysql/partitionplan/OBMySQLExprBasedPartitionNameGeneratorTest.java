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
import com.oceanbase.odc.plugin.task.api.partitionplan.model.PartitionPlanVariableKey;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.SqlExprBasedGeneratorConfig;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.partitionname.OBMySQLExprBasedPartitionNameGenerator;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;

/**
 * Test cases for {@link OBMySQLExprBasedPartitionNameGenerator}
 *
 * @author yh263208
 * @date 2024-01-25 15:31
 * @since ODC_release_4.2.4
 */
public class OBMySQLExprBasedPartitionNameGeneratorTest {

    @Test
    public void generate_sqlExpr_succeed() throws Exception {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            DBTable dbTable = new DBTable();
            DBTablePartition partition = new DBTablePartition();
            DBTablePartitionOption option = new DBTablePartitionOption();
            option.setColumnNames(Collections.singletonList("`col`"));
            partition.setPartitionOption(option);
            DBTablePartitionDefinition definition = new DBTablePartitionDefinition();
            definition.setMaxValues(Collections.singletonList("'2024-01-25 00:00:00'"));
            partition.setPartitionDefinitions(Collections.singletonList(definition));
            dbTable.setPartition(partition);
            PartitionNameGenerator generator = new OBMySQLExprBasedPartitionNameGenerator();
            SqlExprBasedGeneratorConfig config = new SqlExprBasedGeneratorConfig();
            config.setGenerateExpr("concat('p', date_format(from_unixtime(unix_timestamp(${col}) + "
                    + PartitionPlanVariableKey.INTERVAL.getVariable() + "), '%Y%m%d'))");
            config.setIntervalGenerateExpr("86400");
            String actual = generator.invoke(connection, dbTable, getParameters(0, config));
            Assert.assertEquals("p20240126", actual);
        }
    }

    private Map<String, Object> getParameters(int index, SqlExprBasedGeneratorConfig config) {
        Map<String, Object> parameters = new HashMap<>();
        DBTablePartitionDefinition definition = new DBTablePartitionDefinition();
        definition.setMaxValues(Collections.singletonList("'2024-01-25 00:00:00'"));
        parameters.put(PartitionNameGenerator.TARGET_PARTITION_DEF_KEY, definition);
        parameters.put(PartitionNameGenerator.TARGET_PARTITION_DEF_INDEX_KEY, index);
        parameters.put(PartitionNameGenerator.PARTITION_NAME_GENERATOR_KEY, config);
        return parameters;
    }

}
