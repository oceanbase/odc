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

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.tools.dbbrowser.model.datatype.JdbcDataTypeFactory;

/**
 * @author jingtian
 * @date 2023/11/21
 * @since ODC_release_4.2.4
 */
public class MySQLGeometryMapperTest {
    private DataSource dataSource;
    private final MySQLGeometryMapper mapper = new MySQLGeometryMapper();

    @Before
    public void setUp() throws Exception {
        dataSource = TestDBConfigurations.getInstance().getTestOBMysqlConfiguration().getDataSource();
        prepareEnv();
    }

    @After
    public void tearDown() throws Exception {
        destroyEnv();
    }

    @Test
    public void test_POINT_datatype() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            Statement statement = conn.createStatement();
            try (ResultSet rs = statement.executeQuery("select * from gis_test")) {
                while (rs.next()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    Assert.assertEquals("POINT (1 1)",
                            mapper.mapCell(new CellData(rs, 1, new JdbcDataTypeFactory(metaData, 1).generate())));
                    Assert.assertEquals("POINT (2 2)",
                            mapper.mapCell(new CellData(rs, 2, new JdbcDataTypeFactory(metaData, 2).generate())));
                    Assert.assertEquals("LINESTRING (0 0, 1 1, 2 2)",
                            mapper.mapCell(new CellData(rs, 3, new JdbcDataTypeFactory(metaData, 3).generate())));
                    Assert.assertEquals("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))",
                            mapper.mapCell(new CellData(rs, 4, new JdbcDataTypeFactory(metaData, 4).generate())));
                    Assert.assertEquals("MULTIPOINT ((0 0), (1 1))",
                            mapper.mapCell(new CellData(rs, 5, new JdbcDataTypeFactory(metaData, 5).generate())));
                    Assert.assertEquals("MULTILINESTRING ((0 0, 1 1, 2 2), (3 3, 4 4, 5 5))",
                            mapper.mapCell(new CellData(rs, 6, new JdbcDataTypeFactory(metaData, 6).generate())));
                    Assert.assertEquals("MULTIPOLYGON (((0 0, 0 1, 1 1, 1 0, 0 0)), ((2 2, 2 3, 3 3, 3 2, 2 2)))",
                            mapper.mapCell(new CellData(rs, 7, new JdbcDataTypeFactory(metaData, 7).generate())));
                    Assert.assertEquals("GEOMETRYCOLLECTION (POINT (1 1), LINESTRING (0 0, 1 1, 2 2))",
                            mapper.mapCell(new CellData(rs, 8, new JdbcDataTypeFactory(metaData, 8).generate())));
                }
            }
        }
    }

    private void prepareEnv() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            Statement statement = conn.createStatement();
            statement.execute("create table gis_test (\n"
                    + "    id INT PRIMARY KEY AUTO_INCREMENT,\n"
                    + "    geometry_field GEOMETRY,\n"
                    + "    point_field POINT,\n"
                    + "    linestring_field LINESTRING,\n"
                    + "    polygon_field POLYGON,\n"
                    + "    multipoint_field MULTIPOINT,\n"
                    + "    multilinestring_field MULTILINESTRING,\n"
                    + "    multipolygon_field MULTIPOLYGON,\n"
                    + "    geometrycollection_field GEOMETRYCOLLECTION\n"
                    + ")");
            statement.execute("INSERT INTO gis_test \n"
                    + "VALUES (\n"
                    + "    1,\n"
                    + "    ST_GeomFromText('POINT(1 1)'),\n"
                    + "    ST_GeomFromText('POINT(2 2)'),\n"
                    + "    ST_GeomFromText('LINESTRING(0 0, 1 1, 2 2)'),\n"
                    + "    ST_GeomFromText('POLYGON((0 0, 0 1, 1 1, 1 0, 0 0))'),\n"
                    + "    ST_GeomFromText('MULTIPOINT((0 0), (1 1))'),\n"
                    + "    ST_GeomFromText('MULTILINESTRING((0 0, 1 1, 2 2), (3 3, 4 4, 5 5))'),\n"
                    + "    ST_GeomFromText('MULTIPOLYGON(((0 0, 0 1, 1 1, 1 0, 0 0)), ((2 2, 2 3, 3 3, 3 2,"
                    + " 2 2)))'),\n"
                    + "    ST_GeomFromText('GEOMETRYCOLLECTION(POINT(1 1), LINESTRING(0 0, 1 1, 2 2))')\n"
                    + ")");
        }
    }

    private void destroyEnv() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            Statement statement = conn.createStatement();
            statement.execute("drop table gis_test");
        }
    }
}
