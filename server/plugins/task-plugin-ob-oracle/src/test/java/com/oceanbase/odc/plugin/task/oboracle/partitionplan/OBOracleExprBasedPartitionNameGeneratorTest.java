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

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.partitionname.PartitionNameGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.PartitionPlanVariableKey;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.SqlExprBasedGeneratorConfig;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.partitionname.OBMySQLExprBasedPartitionNameGenerator;
import com.oceanbase.odc.plugin.task.oboracle.partitionplan.invoker.partitionname.OBOracleExprBasedPartitionNameGenerator;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;

/**
 * Test cases for {@link OBOracleExprBasedPartitionNameGenerator}
 *
 * @author yh263208
 * @date 2024-01-29 15:52
 * @since ODC_release_4.2.4
 */
public class OBOracleExprBasedPartitionNameGeneratorTest {

    @Test
    public void generate_sqlExpr_succeed() throws Exception {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBOracleConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            DBTable dbTable = new DBTable();
            PartitionNameGenerator generator = new OBMySQLExprBasedPartitionNameGenerator();
            SqlExprBasedGeneratorConfig config = new SqlExprBasedGeneratorConfig();
            config.setGenerateExpr("CONCAT('P', TO_CHAR(TO_DATE('2022-01-01 12:00:00', 'YYYY-MM-DD HH24:MI:SS') + "
                    + PartitionPlanVariableKey.INTERVAL.getVariable() + ", 'YYYYMMDD'))");
            config.setIntervalGenerateExpr("NUMTOYMINTERVAL(1, 'YEAR')");
            String actual = generator.invoke(connection, dbTable, getParameters(0, config));
            Assert.assertEquals("P20230101", actual);
        }
    }

    private Map<String, Object> getParameters(int index, SqlExprBasedGeneratorConfig config) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(PartitionNameGenerator.TARGET_PARTITION_DEF_KEY, new DBTablePartitionDefinition());
        parameters.put(PartitionNameGenerator.TARGET_PARTITION_DEF_INDEX_KEY, index);
        parameters.put(PartitionNameGenerator.PARTITION_NAME_GENERATOR_KEY, config);
        return parameters;
    }
}
