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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.plugin.schema.obmysql.OBMySQLTableExtension;
import com.oceanbase.odc.plugin.task.api.partitionplan.AutoPartitionExtensionPoint;
import com.oceanbase.odc.plugin.task.api.partitionplan.datatype.TimeDataType;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;
import com.oceanbase.tools.dbbrowser.model.datatype.GeneralDataType;

/**
 * Test cases for {@link OBMySQLAutoPartitionExtensionPoint}
 *
 * @author yh263208
 * @date 2024-01-24 14:30
 * @since ODC_release_4.2.4
 */
public class OBMySQLAutoPartitionExtensionPointTest {

    public static final String LIST_PARTI_TABLE_NAME = "list_parti_tbl";
    public static final String NO_PARTI_TABLE_NAME = "no_parti_tbl";
    public static final String RANGE_TABLE_NAME = "range_parti_ext_tbl";
    public static final String RANGE_MAX_VALUE_TABLE_NAME = "range_maxvalue_parti_ext_tbl";
    public static final String RANGE_COLUMNS_TABLE_NAME = "range_col_parti_ext_tbl";

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
        oracle.execute("DROP TABLE " + RANGE_COLUMNS_TABLE_NAME);
        oracle.execute("DROP TABLE " + LIST_PARTI_TABLE_NAME);
        oracle.execute("DROP TABLE " + NO_PARTI_TABLE_NAME);
        oracle.execute("DROP TABLE " + RANGE_TABLE_NAME);
        oracle.execute("DROP TABLE " + RANGE_MAX_VALUE_TABLE_NAME);
    }

    @Test
    public void supports_rangeParti_returnTrue() throws SQLException {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            DBTable dbTable = new OBMySQLTableExtension().getDetail(connection,
                    configuration.getDefaultDBName(), RANGE_COLUMNS_TABLE_NAME);
            AutoPartitionExtensionPoint extensionPoint = new OBMySQLAutoPartitionExtensionPoint();
            Assert.assertTrue(extensionPoint.supports(dbTable.getPartition()));
        }
    }

    @Test
    public void supports_rangePartiMaxValue_returnFalse() throws SQLException {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        Connection connection = configuration.getDataSource().getConnection();
        DBTable dbTable = new OBMySQLTableExtension().getDetail(connection,
                configuration.getDefaultDBName(), RANGE_MAX_VALUE_TABLE_NAME);
        AutoPartitionExtensionPoint extensionPoint = new OBMySQLAutoPartitionExtensionPoint();
        Assert.assertFalse(extensionPoint.supports(dbTable.getPartition()));
    }

    @Test
    public void supports_nonParti_returnFalse() throws SQLException {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            DBTable dbTable = new OBMySQLTableExtension().getDetail(connection,
                    configuration.getDefaultDBName(), NO_PARTI_TABLE_NAME);
            AutoPartitionExtensionPoint extensionPoint = new OBMySQLAutoPartitionExtensionPoint();
            Assert.assertFalse(extensionPoint.supports(dbTable.getPartition()));
        }
    }

    @Test
    public void supports_listParti_returnFalse() throws SQLException {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            DBTable dbTable = new OBMySQLTableExtension().getDetail(connection,
                    configuration.getDefaultDBName(), LIST_PARTI_TABLE_NAME);
            AutoPartitionExtensionPoint extensionPoint = new OBMySQLAutoPartitionExtensionPoint();
            Assert.assertFalse(extensionPoint.supports(dbTable.getPartition()));
        }
    }

    @Test
    public void getPartitionKeyDataTypes_rangeParti_getSucceed() throws SQLException, IOException {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            DBTable dbTable = new OBMySQLTableExtension().getDetail(connection,
                    configuration.getDefaultDBName(), RANGE_COLUMNS_TABLE_NAME);
            AutoPartitionExtensionPoint extensionPoint = new OBMySQLAutoPartitionExtensionPoint();
            List<DataType> actuals = extensionPoint.getPartitionKeyDataTypes(connection, dbTable);
            Assert.assertEquals(Arrays.asList(
                    new GeneralDataType(0, 0, "varchar"), new TimeDataType(TimeDataType.DAY)), actuals);
        }
    }

    @Test
    public void getPartitionKeyDataTypes_rangeExprParti_getSucceed() throws SQLException, IOException {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            DBTable dbTable = new OBMySQLTableExtension().getDetail(connection,
                    configuration.getDefaultDBName(), RANGE_TABLE_NAME);
            AutoPartitionExtensionPoint extensionPoint = new OBMySQLAutoPartitionExtensionPoint();
            List<DataType> actuals = extensionPoint.getPartitionKeyDataTypes(connection, dbTable);
            Assert.assertEquals(Collections.singletonList(new GeneralDataType(4, 0, "BIGINT")), actuals);
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getPartitionKeyDataTypes_listParti_exptThrown() throws SQLException, IOException {
        TestDBConfiguration configuration = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
        try (Connection connection = configuration.getDataSource().getConnection()) {
            DBTable dbTable = new OBMySQLTableExtension().getDetail(connection,
                    configuration.getDefaultDBName(), LIST_PARTI_TABLE_NAME);
            AutoPartitionExtensionPoint extensionPoint = new OBMySQLAutoPartitionExtensionPoint();
            extensionPoint.getPartitionKeyDataTypes(connection, dbTable);
        }
    }

    @Test
    public void unquoteIdentifier_wrappedStr_succeed() {
        List<String> inputs = Arrays.asList("`abcd`", "abcde");
        AutoPartitionExtensionPoint extensionPoint = new OBMySQLAutoPartitionExtensionPoint();
        List<String> actuals = inputs.stream().map(extensionPoint::unquoteIdentifier).collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList("abcd", "abcde"), actuals);
    }

    private static List<String> getDdlContent() throws IOException {
        String delimiter = "\\$\\$\\s*";
        try (InputStream input = OBMySQLAutoPartitionKeyDataTypeFactoryTest.class.getClassLoader()
                .getResourceAsStream("partitionplan/extension_point_create_table_partition.sql")) {
            byte[] buffer = new byte[input.available()];
            IOUtils.readFully(input, buffer);
            StringSubstitutor substitutor = StringSubstitutor.createInterpolator();
            return new ArrayList<>(Arrays.asList(substitutor.replace(new String(buffer)).split(delimiter)));
        }
    }

}
