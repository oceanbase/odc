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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.plugin.schema.api.TableExtensionPoint;
import com.oceanbase.odc.plugin.schema.oboracle.OBOracleTableExtension;
import com.oceanbase.odc.plugin.task.api.partitionplan.datatype.TimeDataType;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator;
import com.oceanbase.odc.plugin.task.oboracle.partitionplan.datatype.OBOraclePartitionKeyDataTypeFactory;
import com.oceanbase.odc.plugin.task.oboracle.partitionplan.invoker.OBOracleSqlExprCalculator;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;
import com.oceanbase.tools.dbbrowser.model.datatype.GeneralDataType;

/**
 * Test cases for {@link OBOraclePartitionKeyDataTypeFactory}
 *
 * @author yh263208
 * @date 2024-01-26 20:12
 * @since ODC_release_4.2.4
 */
public class OBOraclePartitionKeyDataTypeFactoryTest {

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
    public void generate_rangeColumnsPartKeys_succeed() throws SQLException {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBOracleConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            TableExtensionPoint tableExtension = new OBOracleTableExtension();
            DBTable dbTable = tableExtension.getDetail(connection, configuration.getDefaultDBName(), RANGE_TABLE_NAME);
            SqlExprCalculator calculator = new OBOracleSqlExprCalculator(connection);
            List<DataType> actuals = new ArrayList<>();
            actuals.add(new OBOraclePartitionKeyDataTypeFactory(calculator, dbTable, "C1").generate());
            actuals.add(new OBOraclePartitionKeyDataTypeFactory(calculator, dbTable, "C2").generate());
            actuals.add(new OBOraclePartitionKeyDataTypeFactory(calculator, dbTable, "C5").generate());
            Assert.assertEquals(Arrays.asList(new TimeDataType("DATE", TimeDataType.SECOND),
                    new TimeDataType("TIMESTAMP", TimeDataType.SECOND),
                    new GeneralDataType(0, 0, "VARCHAR2")), actuals);
        }
    }

    private static List<String> getDdlContent() throws IOException {
        String delimiter = "\\$\\$\\s*";
        try (InputStream input = OBOraclePartitionKeyDataTypeFactoryTest.class.getClassLoader()
                .getResourceAsStream("partitionplan/datatype_factory_create_table_partition.sql")) {
            byte[] buffer = new byte[input.available()];
            IOUtils.readFully(input, buffer);
            StringSubstitutor substitutor = StringSubstitutor.createInterpolator();
            return new ArrayList<>(Arrays.asList(substitutor.replace(new String(buffer)).split(delimiter)));
        }
    }

}
