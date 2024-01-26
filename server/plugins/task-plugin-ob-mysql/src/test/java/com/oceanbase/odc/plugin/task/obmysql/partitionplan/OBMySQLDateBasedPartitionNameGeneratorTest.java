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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.partitionname.DateBasedPartitionNameGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.partitionname.PartitionNameGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.DateBasedPartitionNameGeneratorConfig;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.partitionname.OBMySQLDateBasedPartitionNameGenerator;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;

/**
 * Test cases for {@link OBMySQLDateBasedPartitionNameGenerator}
 *
 * @author yh263208
 * @date 2024-01-25 15:42
 * @since ODC_release_4.2.4
 */
public class OBMySQLDateBasedPartitionNameGeneratorTest {

    @Test
    public void generate_sqlExpr_succeed() throws Exception {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            DBTable dbTable = new DBTable();
            DateBasedPartitionNameGeneratorConfig config = new DateBasedPartitionNameGeneratorConfig();
            config.setNamingPrefix("p");
            config.setNamingSuffixExpression("yyyyMMdd");
            PartitionNameGenerator generator = new OBMySQLDateBasedPartitionNameGenerator();
            String actual = generator.invoke(connection, dbTable, getParameters(config));
            DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String expect = "p" + dateFormat.format(new Date());
            Assert.assertEquals(expect, actual);
        }
    }

    private Map<String, Object> getParameters(DateBasedPartitionNameGeneratorConfig config) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(PartitionNameGenerator.TARGET_PARTITION_DEF_KEY, new DBTablePartitionDefinition());
        parameters.put(PartitionNameGenerator.TARGET_PARTITION_DEF_INDEX_KEY, 1);
        parameters.put(DateBasedPartitionNameGenerator.PARTITION_NAME_GENERATOR_KEY, config);
        return parameters;
    }

}
