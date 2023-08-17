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
package com.oceanbase.odc.core.sql.execute.mapper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.tools.dbbrowser.model.datatype.JdbcDataTypeFactory;

public class OracleBinaryNumberMapperTest {
    private DataSource dataSource;
    private final OracleBinaryNumberMapper mapper = new OracleBinaryNumberMapper();

    @Before
    public void setUp() throws Exception {
        dataSource = TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource();
        prepareEnv();
    }

    @After
    public void tearDown() throws Exception {
        destroyEnv();
    }

    @Test
    public void test_Map_BinaryNumber_Inf() throws Exception {
        List<Object> result = queryData(1);
        Assert.assertEquals("Inf", result.get(0));
        Assert.assertEquals("Inf", result.get(1));
    }

    @Test
    public void test_Map_BinaryNumber_Nan() throws Exception {
        List<Object> result = queryData(2);
        Assert.assertEquals("Nan", result.get(0));
        Assert.assertEquals("Nan", result.get(1));
    }

    @Test
    public void test_Map_BinaryNumber_Regular() throws Exception {
        List<Object> result = queryData(3);
        Assert.assertEquals("3.95E+001", result.get(0));
        Assert.assertEquals("1.5699999999999999E+001", result.get(1));
    }

    private void prepareEnv() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            Statement statement = conn.createStatement();
            statement.execute("CREATE TABLE BINARY_TEST (ID INT, FLOAT_ BINARY_FLOAT, DOUBLE_ BINARY_DOUBLE)");
            statement.execute("INSERT INTO BINARY_TEST VALUES (1, 'Inf', 'Inf')");
            statement.execute("INSERT INTO BINARY_TEST VALUES (2, 'Nan', 'Nan')");
            statement.execute("INSERT INTO BINARY_TEST VALUES (3, 39.5f, 15.7d)");
        }
    }

    private void destroyEnv() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            Statement statement = conn.createStatement();
            statement.execute("DROP TABLE BINARY_TEST");
        }
    }

    private List<Object> queryData(int id) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            Statement statement = conn.createStatement();
            try (ResultSet rs = statement.executeQuery("SELECT FLOAT_, DOUBLE_ FROM BINARY_TEST WHERE ID=" + id)) {
                rs.next();
                ResultSetMetaData metaData = rs.getMetaData();
                Object floatCol = mapper.mapCell(new CellData(rs, 0, new JdbcDataTypeFactory(metaData, 0).generate()));
                Object doubleCol = mapper.mapCell(new CellData(rs, 1, new JdbcDataTypeFactory(metaData, 1).generate()));
                return Arrays.asList(floatCol, doubleCol);
            }
        }
    }

}
