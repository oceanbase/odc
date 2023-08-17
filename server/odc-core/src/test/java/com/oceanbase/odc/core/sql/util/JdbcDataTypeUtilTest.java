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
package com.oceanbase.odc.core.sql.util;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBPLParamMode;

/**
 * @author wenniu.ly
 * @date 2022/7/5
 */
public class JdbcDataTypeUtilTest {

    @Test
    public void test_parse_null_type() {
        Assert.assertEquals(JDBCType.VARCHAR, JdbcDataTypeUtil.parseDataType(""));
        Assert.assertEquals(JDBCType.VARCHAR, JdbcDataTypeUtil.parseDataType(null));
    }

    @Test
    public void test_parse_boolean_type() {
        Assert.assertEquals(JDBCType.BOOLEAN, JdbcDataTypeUtil.parseDataType("bool"));
        Assert.assertEquals(JDBCType.BOOLEAN, JdbcDataTypeUtil.parseDataType("boolean"));
    }

    @Test
    public void test_parse_integer_type() {
        Assert.assertEquals(JDBCType.INTEGER, JdbcDataTypeUtil.parseDataType("int"));
        Assert.assertEquals(JDBCType.INTEGER, JdbcDataTypeUtil.parseDataType("PLS_INTEGER"));
        Assert.assertEquals(JDBCType.INTEGER, JdbcDataTypeUtil.parseDataType("BINARY_INTEGER"));
        Assert.assertEquals(JDBCType.INTEGER, JdbcDataTypeUtil.parseDataType("NATURAL"));
        Assert.assertEquals(JDBCType.INTEGER, JdbcDataTypeUtil.parseDataType("NATURALN"));
        Assert.assertEquals(JDBCType.INTEGER, JdbcDataTypeUtil.parseDataType("POSITIVE"));
        Assert.assertEquals(JDBCType.INTEGER, JdbcDataTypeUtil.parseDataType("POSITIVEN"));
        Assert.assertEquals(JDBCType.INTEGER, JdbcDataTypeUtil.parseDataType("SIGNTYPE"));
        Assert.assertEquals(JDBCType.INTEGER, JdbcDataTypeUtil.parseDataType("SIMPLE_INTEGER"));
        Assert.assertEquals(JDBCType.INTEGER, JdbcDataTypeUtil.parseDataType("MEDIUMINT"));
    }

    @Test
    public void test_parse_number_type() {
        Assert.assertEquals(JDBCType.NUMERIC, JdbcDataTypeUtil.parseDataType("number"));
        Assert.assertEquals(JDBCType.NUMERIC, JdbcDataTypeUtil.parseDataType("numeric"));
    }

    @Test
    public void test_parse_float_type() {
        Assert.assertEquals(JDBCType.FLOAT, JdbcDataTypeUtil.parseDataType("binary_float"));
        Assert.assertEquals(JDBCType.FLOAT, JdbcDataTypeUtil.parseDataType("simple_float"));
        Assert.assertEquals(JDBCType.FLOAT, JdbcDataTypeUtil.parseDataType("float"));
    }

    @Test
    public void test_parse_double_type() {
        Assert.assertEquals(JDBCType.DOUBLE, JdbcDataTypeUtil.parseDataType("binary_double"));
        Assert.assertEquals(JDBCType.DOUBLE, JdbcDataTypeUtil.parseDataType("simple_double"));
        Assert.assertEquals(JDBCType.DOUBLE, JdbcDataTypeUtil.parseDataType("double"));
    }

    @Test
    public void test_parse_char_type() {
        Assert.assertEquals(JDBCType.VARCHAR, JdbcDataTypeUtil.parseDataType("varchar2"));
        Assert.assertEquals(JDBCType.VARCHAR, JdbcDataTypeUtil.parseDataType("varchar"));
    }

    @Test
    public void test_parse_mysql_type_with_parenthesis() {
        Assert.assertEquals(JDBCType.TINYINT, JdbcDataTypeUtil.parseDataType("tinyint(5)"));
        Assert.assertEquals(JDBCType.SMALLINT, JdbcDataTypeUtil.parseDataType("smallint(8)"));
        Assert.assertEquals(JDBCType.INTEGER, JdbcDataTypeUtil.parseDataType("mediumint(9)"));
        Assert.assertEquals(JDBCType.INTEGER, JdbcDataTypeUtil.parseDataType("integer(10)"));
        Assert.assertEquals(JDBCType.BIGINT, JdbcDataTypeUtil.parseDataType("bigint(20)"));

        Assert.assertEquals(JDBCType.FLOAT, JdbcDataTypeUtil.parseDataType("float(6,3)"));
        Assert.assertEquals(JDBCType.DOUBLE, JdbcDataTypeUtil.parseDataType("double(5)"));
        Assert.assertEquals(JDBCType.NUMERIC, JdbcDataTypeUtil.parseDataType("number(6,3)"));
        Assert.assertEquals(JDBCType.DATE, JdbcDataTypeUtil.parseDataType("datetime(7)"));
        Assert.assertEquals(JDBCType.TIMESTAMP, JdbcDataTypeUtil.parseDataType("timestamp(6)"));
        Assert.assertEquals(JDBCType.TIME, JdbcDataTypeUtil.parseDataType("time(5)"));
    }

    @Test
    public void validateInParameter_dateStrWithHrsMinAndSecs_validateInSucceed() {
        DBPLParam param = new DBPLParam();
        param.setParamMode(DBPLParamMode.IN);
        param.setDataType("DATE");
        param.setDefaultValue("2022-03-02 16:12:02");
        Assert.assertEquals(true, JdbcDataTypeUtil.validateInParameter(param));
    }

    @Test
    public void validateInParameter_dateStrWithHrsMinAndSecs_validateInFailed() {
        DBPLParam param = new DBPLParam();
        param.setParamMode(DBPLParamMode.IN);
        param.setDataType("DATE");
        param.setDefaultValue("2022-03-0216:12:02");
        Assert.assertEquals(false, JdbcDataTypeUtil.validateInParameter(param));
    }

    @Test
    public void validateInParameter_dateStrWithOutHrsMinAndSecs_validateInSucceed() {
        DBPLParam param = new DBPLParam();
        param.setParamMode(DBPLParamMode.IN);
        param.setDataType("DATE");
        param.setDefaultValue("2022-03-02");
        Assert.assertEquals(true, JdbcDataTypeUtil.validateInParameter(param));
    }

    @Test
    public void setValueIntoStatement_dateStrWithoutHrsMinAndSecs_setValueSucceed()
            throws SQLException, ParseException {
        ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Integer> indexCaptor = ArgumentCaptor.forClass(Integer.class);
        CallableStatement statement = Mockito.mock(CallableStatement.class);
        JdbcDataTypeUtil.setValueIntoStatement(statement, 1, "DATE", "2022-03-02");
        Mockito.verify(statement).setDate(indexCaptor.capture(), dateCaptor.capture());

        Date actual = dateCaptor.getValue();
        Calendar calendar = new GregorianCalendar();
        calendar.set(2022, Calendar.MARCH, 2, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date expect = new Date(calendar.getTime().getTime());
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void setValueIntoStatement_dateStrWithHrsMinAndSecs_setValueSucceed() throws SQLException, ParseException {
        ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Integer> indexCaptor = ArgumentCaptor.forClass(Integer.class);
        CallableStatement statement = Mockito.mock(CallableStatement.class);
        JdbcDataTypeUtil.setValueIntoStatement(statement, 1, "DATE", "2022-03-02 16:12:02");
        Mockito.verify(statement).setDate(indexCaptor.capture(), dateCaptor.capture());

        Date actual = dateCaptor.getValue();
        Calendar calendar = new GregorianCalendar();
        calendar.set(2022, Calendar.MARCH, 2, 16, 12, 2);
        calendar.set(Calendar.MILLISECOND, 0);
        Date expect = new Date(calendar.getTime().getTime());
        Assert.assertEquals(expect, actual);
    }

    @Test(expected = com.oceanbase.odc.core.shared.exception.BadArgumentException.class)
    public void setValueIntoStatement_dateStrWithHrsMinAndSecs_setValueFailed() throws SQLException, ParseException {
        CallableStatement statement = Mockito.mock(CallableStatement.class);
        JdbcDataTypeUtil.setValueIntoStatement(statement, 1, "DATE", "2022-03-0216:12:02");
    }
}

