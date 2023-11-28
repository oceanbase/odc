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
package com.oceanbase.odc.service.dml;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.core.shared.constant.DialectType;

/**
 * Test for {@link DataConvertUtil}
 *
 * @author yh263208
 * @date 2021-06-03 21:25
 * @since ODC_release_2.4.2
 */
public class DataConvertUtilTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    @Ignore("TODO: fix this test")
    public void testTimestamp() {
        String timestamp = "2023-07-11T20:04:31.008891234+08:00";
        String dataType = "timestamp(5)";
        String queryString = DataConvertUtil.convertToSqlString(DialectType.OB_ORACLE, dataType, timestamp);
        Assert.assertEquals("to_timestamp('2023-07-11 20:04:31.008891234', 'YYYY-MM-DD HH24:MI:SS.FF')", queryString);
    }

    @Test
    @Ignore("TODO: fix this test")
    public void testTimeStampLTZ() {
        String timestamp = "2023-07-11T12:04:31.008891234Z";
        String dataType = "timestamp(5) with local time zone";
        String queryString = DataConvertUtil.convertToSqlString(DialectType.OB_ORACLE, dataType, timestamp);
        Assert.assertEquals("to_timestamp('2023-07-11 20:04:31.008891234', 'YYYY-MM-DD HH24:MI:SS.FF')", queryString);
    }

    @Test
    public void testTimeStampTZ() {
        String timestamp = "2023-07-11T20:04:31.008891234+08:00";
        String dataType = "timestamp(5) with time zone";
        String actual = DataConvertUtil.convertToSqlString(DialectType.OB_ORACLE, dataType, timestamp);
        Assert.assertEquals(
                "to_timestamp_tz('2023-07-11 20:04:31.008891234 +08:00', 'YYYY-MM-DD HH24:MI:SS.FF TZH:TZM')", actual);
    }

    @Test
    public void testStringValue() {
        Assert.assertEquals("'abcd''dfg'",
                DataConvertUtil.convertToSqlString(DialectType.OB_ORACLE, "CHAR(17)", "abcd'dfg"));
        Assert.assertEquals("'abcd\\sss'",
                DataConvertUtil.convertToSqlString(DialectType.OB_ORACLE, "VARCHAR(17)", "abcd\\sss"));
        Assert.assertEquals("'abcd''dfg'",
                DataConvertUtil.convertToSqlString(DialectType.OB_MYSQL, "VARCHAR(17)", "abcd'dfg"));
        Assert.assertEquals("'abcd\\\\sss'",
                DataConvertUtil.convertToSqlString(DialectType.OB_MYSQL, "VARCHAR(17)", "abcd\\sss"));
    }

    @Test
    public void testNumberValue() {
        Assert.assertEquals("1312.213123",
                DataConvertUtil.convertToSqlString(DialectType.OB_ORACLE, "NUMber(32, 8)", "1312.213123"));
        Assert.assertEquals("1312.213123",
                DataConvertUtil.convertToSqlString(DialectType.OB_ORACLE, "number", "1312.213123"));
        Assert.assertEquals("1312", DataConvertUtil.convertToSqlString(DialectType.OB_ORACLE, "int", "1312"));
        Assert.assertEquals("1312", DataConvertUtil.convertToSqlString(DialectType.OB_ORACLE, "bigint", "1312"));
        Assert.assertEquals("1312", DataConvertUtil.convertToSqlString(DialectType.OB_ORACLE, "smallint", "1312"));
        Assert.assertEquals("1312", DataConvertUtil.convertToSqlString(DialectType.OB_ORACLE, "mediumint", "1312"));
        Assert.assertEquals("1312", DataConvertUtil.convertToSqlString(DialectType.OB_ORACLE, "tinyint", "1312"));
        Assert.assertEquals("1312.11",
                DataConvertUtil.convertToSqlString(DialectType.OB_ORACLE, "decimal(12,3)", "1312.11"));
        Assert.assertEquals("1312", DataConvertUtil.convertToSqlString(DialectType.OB_ORACLE, "float", "1312"));
        Assert.assertEquals("1312", DataConvertUtil.convertToSqlString(DialectType.OB_ORACLE, "double", "1312"));
    }

    @Test
    @Ignore("TODO: fix this test")
    public void testDateTypeValue() {
        Assert.assertEquals("to_date('2021-05-04 20:23:34', 'YYYY-MM-DD HH24:MI:SS')",
                DataConvertUtil.convertToSqlString(DialectType.OB_ORACLE, "dAte", "2021-05-04T12:23:34Z"));
        Assert.assertEquals("'2021/05/04 12:23:34'",
                DataConvertUtil.convertToSqlString(DialectType.OB_MYSQL, "dAte", "2021/05/04 12:23:34"));
    }

    @Test
    public void testByteTypeValue() {
        Assert.assertEquals("load_file('2021-05-04 12:23:34')",
                DataConvertUtil.convertToSqlString(DialectType.OB_MYSQL,
                        new DataValue("2021-05-04 12:23:34", "blob", ValueContentType.FILE, ValueEncodeType.TXT)));
        Assert.assertEquals("'2021-05-04'",
                DataConvertUtil.convertToSqlString(DialectType.OB_MYSQL, "raw(14)", "2021-05-04"));
        Assert.assertEquals("'2021-05-04 12:23:34'",
                DataConvertUtil.convertToSqlString(DialectType.OB_MYSQL, "varbinary(123)", "2021-05-04 12:23:34"));
    }

    @Test
    public void testDateTypeValueForMysql() {
        Assert.assertEquals("'2021-05-04 12:23:34'",
                DataConvertUtil.convertToSqlString(DialectType.OB_MYSQL, "date", "2021-05-04 12:23:34"));
        Assert.assertEquals("'2021-05-04 12:23:34'",
                DataConvertUtil.convertToSqlString(DialectType.OB_MYSQL, "timestamp(5)", "2021-05-04 12:23:34"));
        Assert.assertEquals("'12:23:34'",
                DataConvertUtil.convertToSqlString(DialectType.OB_MYSQL, "time(5)", "12:23:34"));
        Assert.assertEquals("'2021-05-04 12:23:34'",
                DataConvertUtil.convertToSqlString(DialectType.OB_MYSQL, "datetime", "2021-05-04 12:23:34"));
    }

    @Test
    @Ignore("TODO: fix this test")
    public void convertToSqlString_dashAsDelimiter_convertCorrectly() {
        Assert.assertEquals("to_date('2021-05-04 20:54:23', 'YYYY-MM-DD HH24:MI:SS')",
                DataConvertUtil.convertToSqlString(DialectType.OB_ORACLE, "date", "2021-05-04T12:54:23Z"));
    }

    @Test
    @Ignore("TODO: fix this test")
    public void convertToSqlString_slashAsDelimiterWithoutTime_convertCorrectly() {
        Assert.assertEquals("to_date('2021-05-04 20:12:12', 'YYYY-MM-DD HH24:MI:SS')",
                DataConvertUtil.convertToSqlString(DialectType.OB_ORACLE, "date", "2021-05-04T12:12:12Z"));
    }

    @Test
    public void testYearTypeValueForMysql() {
        Assert.assertEquals("'2021'", DataConvertUtil.convertToSqlString(DialectType.OB_MYSQL, "year(3)", "2021"));
    }

    @Test
    public void testBitTypeValueForMysql() {
        Assert.assertEquals("'2021-05-04'",
                DataConvertUtil.convertToSqlString(DialectType.OB_MYSQL, "bit(3)", "2021-05-04"));
    }

    @Test
    public void testIntervalDSForOracle() {
        Assert.assertEquals("'3 2:25:45.123'", DataConvertUtil.convertToSqlString(DialectType.OB_ORACLE,
                "interval day(2) to second(6)", "3 2:25:45.123"));
    }

    @Test
    public void testIntervalYMForOracle() {
        Assert.assertEquals("'+000000004 04:00:00.000000000'", DataConvertUtil.convertToSqlString(DialectType.OB_ORACLE,
                "interval year(2) to month(6)", "+000000004 04:00:00.000000000"));
    }

    @Test
    public void testGeometryForMySQLAndOBMySQL() {
        Assert.assertArrayEquals(new Object[] {
                "ST_GeomFromText('POINT (1 2)', 4322)",
                "ST_GeomFromText('POINT (1 2)')",
                "ST_GeomFromText('LINESTRING (0 0, 1 1, 1 2, 2 2)', 4322)",
                "ST_GeomFromText('POLYGON ((0 0, 1 0, 1 1, 0 1, 0 0))', 4326)",
                "ST_GeomFromText('MULTIPOINT ((0 0), (1 1), (2 2))', 4326)",
                "ST_GeomFromText('MULTILINESTRING ((0 0, 1 1, 2 2), (3 3, 4 4, 5 5))', 4326)",
                "ST_GeomFromText('MULTIPOLYGON (((0 0, 1 0, 1 1, 0 1, 0 0)), ((2 2, 3 2, 3 3, 2 3, 2 2)))', 4326)",
                "ST_GeomFromText('GEOMETRYCOLLECTION (POINT (0 0), LINESTRING (1 1, 2 2))', 4326)"
        }, new Object[] {
                DataConvertUtil.convertToSqlString(DialectType.OB_MYSQL, "geometry", "POINT (1 2) | 4322"),
                DataConvertUtil.convertToSqlString(DialectType.OB_MYSQL, "point", "POINT (1 2)"),
                DataConvertUtil.convertToSqlString(DialectType.OB_MYSQL, "linestring",
                        "LINESTRING (0 0, 1 1, 1 2, 2 2) | 4322"),
                DataConvertUtil.convertToSqlString(DialectType.MYSQL, "polygon",
                        "POLYGON ((0 0, 1 0, 1 1, 0 1, 0 0)) | 4326"),
                DataConvertUtil.convertToSqlString(DialectType.MYSQL, "multipoint",
                        "MULTIPOINT ((0 0), (1 1), (2 2)) | 4326"),
                DataConvertUtil.convertToSqlString(DialectType.MYSQL, "multilinestring",
                        "MULTILINESTRING ((0 0, 1 1, 2 2), (3 3, 4 4, 5 5)) | 4326"),
                DataConvertUtil.convertToSqlString(DialectType.MYSQL, "multipolygon",
                        "MULTIPOLYGON (((0 0, 1 0, 1 1, 0 1, 0 0)), ((2 2, 3 2, 3 3, 2 3, 2 2))) | 4326"),
                DataConvertUtil.convertToSqlString(DialectType.MYSQL, "geometrycollection",
                        "GEOMETRYCOLLECTION (POINT (0 0), LINESTRING (1 1, 2 2)) | 4326")
        });
    }
}
