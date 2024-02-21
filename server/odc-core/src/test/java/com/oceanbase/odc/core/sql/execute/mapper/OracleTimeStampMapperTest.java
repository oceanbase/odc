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

import com.oceanbase.odc.core.sql.execute.model.TimeFormatResult;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.tools.dbbrowser.model.datatype.JdbcDataTypeFactory;

/**
 * @author jingtian
 * @date 2024/2/20
 * @since ODC_release_4.2.4
 */
public class OracleTimeStampMapperTest {
    private DataSource dataSource;
    private final OracleNlsFormatTimestampTZMapper timeStampTZMapper =
            new OracleNlsFormatTimestampTZMapper("DD-MM-RR TZH TZM");
    private final OracleNlsFormatTimestampLTZMapper timeStampLTZMapper =
            new OracleNlsFormatTimestampLTZMapper("DD-MM-RR");

    @Before
    public void setUp() throws Exception {
        dataSource = TestDBConfigurations.getInstance().getTestOracleConfiguration().getDataSource();
        prepareEnv();
    }

    @Test
    public void test_timeStampTZ_datatype() throws Exception {
        TimeFormatResult expect = new TimeFormatResult("02-02-22 +01 00", 1643793683000L, 0, "GMT+01:00");
        try (Connection conn = dataSource.getConnection()) {
            Statement statement = conn.createStatement();
            try (ResultSet rs = statement.executeQuery("select * from oracle_time_test")) {
                while (rs.next()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    TimeFormatResult actual = (TimeFormatResult) timeStampTZMapper.mapCell(
                            new CellData(rs, 3, new JdbcDataTypeFactory(metaData, 3).generate()));
                    Assert.assertEquals(expect, actual);
                }
            }
        }
    }

    @Test
    public void test_timeStampLTZ_datatype() throws Exception {
        TimeFormatResult expect = new TimeFormatResult("03-03-22", 1646273042000L, 0, "Asia/Shanghai");
        try (Connection conn = dataSource.getConnection()) {
            Statement statement = conn.createStatement();
            try (ResultSet rs = statement.executeQuery("select * from oracle_time_test")) {
                while (rs.next()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    TimeFormatResult actual = (TimeFormatResult) timeStampLTZMapper.mapCell(
                            new CellData(rs, 4, new JdbcDataTypeFactory(metaData, 4).generate()));
                    Assert.assertEquals(expect, actual);
                }
            }
        }
    }

    @After
    public void tearDown() throws Exception {
        destroyEnv();
    }

    private void prepareEnv() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            Statement statement = conn.createStatement();
            statement.execute("CREATE TABLE oracle_time_test (\n"
                    + "    id NUMBER PRIMARY KEY,\n"
                    + "    date_field DATE,\n"
                    + "    timestamp_field TIMESTAMP,\n"
                    + "    timestamp_tz_field TIMESTAMP WITH TIME ZONE,\n"
                    + "    timestamp_ltz_field TIMESTAMP WITH LOCAL TIME ZONE\n"
                    + ")");
            statement.execute("INSERT INTO oracle_time_test (id, date_field, timestamp_field, timestamp_tz_field, "
                    + "timestamp_ltz_field) VALUES (\n"
                    + "    1,\n"
                    + "    TO_DATE('2022-01-01 10:00:00', 'YYYY-MM-DD HH24:MI:SS'),\n"
                    + "    TO_TIMESTAMP('2022-01-01 10:30:30', 'YYYY-MM-DD HH24:MI:SS'),\n"
                    + "    TO_TIMESTAMP_TZ('2022-02-02 10:21:23 +01:00', 'YYYY-MM-DD HH24:MI:SS TZH:TZM'), \n"
                    + "    TO_TIMESTAMP('2022-03-03 10:04:02', 'YYYY-MM-DD HH24:MI:SS') \n"
                    + ")");
        }
    }

    private void destroyEnv() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            Statement statement = conn.createStatement();
            statement.execute("drop table oracle_time_test");
        }
    }
}
