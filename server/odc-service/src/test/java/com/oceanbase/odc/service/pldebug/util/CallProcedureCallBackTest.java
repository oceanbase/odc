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
package com.oceanbase.odc.service.pldebug.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBPLParamMode;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;

/**
 * {@link CallProcedureCallBackTest}
 *
 * @author yh263208
 * @date 2023-03-02 17:41
 * @since ODC_release_4.1.2
 */
public class CallProcedureCallBackTest {

    public static final String TEST_CASE_1 = "PL_TEST_1";
    public static final String TEST_CASE_2 = "pl_test_2";
    public static final String TEST_CASE_3 = "PL_TEST_3";
    public static final String TEST_CASE_4 = "PL_TEST_4";

    @BeforeClass
    public static void setUp() throws IOException {
        JdbcTemplate oracle =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        getOraclePLContent().forEach(oracle::execute);
        JdbcTemplate mysql =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBMysqlConfiguration().getDataSource());
        getMySQLPLContent().forEach(mysql::execute);
    }

    @AfterClass
    public static void clear() {
        JdbcTemplate oracle =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        oracle.execute("DROP PROCEDURE " + TEST_CASE_1);
        oracle.execute("DROP PROCEDURE " + TEST_CASE_3);
        oracle.execute("DROP PROCEDURE " + TEST_CASE_4);
        JdbcTemplate mysql =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBMysqlConfiguration().getDataSource());
        mysql.execute("drop procedure " + TEST_CASE_2);
    }

    @Test
    public void doInConnection_setNullToInParam_callSucceed() {
        DBProcedure procedure = new DBProcedure();
        procedure.setProName(TEST_CASE_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("n1");
        param.setDefaultValue(null);
        param.setDataType("varchar2");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue("5");
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("n2");
        param.setDefaultValue(null);
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("n3");
        param.setDefaultValue("");
        param.setDataType("varchar2");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue("5");
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);
        procedure.setParams(list);

        CallProcedureCallBack callBack = new CallProcedureCallBack(procedure, 30, new OracleSqlBuilder());
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        List<DBPLParam> actual = jdbcTemplate.execute(callBack);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.INOUT);
        param.setDefaultValue("10");
        List<DBPLParam> expect = Collections.singletonList(param);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_whenMySQLMode_callSucceed() {
        DBProcedure procedure = new DBProcedure();
        procedure.setProName(TEST_CASE_2);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue("10");
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue("10");
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        procedure.setParams(list);
        CallProcedureCallBack callBack = new CallProcedureCallBack(procedure, 30, new MySQLSqlBuilder());
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBMysqlConfiguration().getDataSource());
        List<DBPLParam> acutal = jdbcTemplate.execute(callBack);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setParamMode(DBPLParamMode.OUT);
        param.setDataType("int");
        param.setDefaultValue("20");
        List<DBPLParam> expect = Collections.singletonList(param);
        Assert.assertEquals(expect, acutal);
    }

    @Test
    public void doInConnection_whenOracleMode_callSucceed() {
        DBProcedure procedure = new DBProcedure();
        procedure.setProName(TEST_CASE_3);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue("5");
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue("5");
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        procedure.setParams(list);
        // 注意版本1.1.0开始
        CallProcedureCallBack callBack = new CallProcedureCallBack(procedure, 30, new OracleSqlBuilder());
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        List<DBPLParam> actual = jdbcTemplate.execute(callBack);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setParamMode(DBPLParamMode.INOUT);
        param.setDataType("int");
        param.setDefaultValue("10");
        List<DBPLParam> expect = Collections.singletonList(param);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_whenOracleModeAndOutParam_callSucceed() {
        DBProcedure procedure = new DBProcedure();
        procedure.setProName(TEST_CASE_4);
        // dbms_output.get_line
        List<DBPLParam> params = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("line");
        param.setParamMode(DBPLParamMode.OUT);
        param.setDataType("varchar2");
        params.add(param);

        param = new DBPLParam();
        param.setParamName("status");
        param.setParamMode(DBPLParamMode.OUT);
        param.setDataType("int");
        params.add(param);
        procedure.setParams(params);

        CallProcedureCallBack callBack = new CallProcedureCallBack(procedure, 30, new OracleSqlBuilder());
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        List<DBPLParam> actual = jdbcTemplate.execute(callBack);

        DBPLParam p1 = new DBPLParam();
        p1.setParamName("line");
        p1.setParamMode(DBPLParamMode.OUT);
        p1.setDataType("varchar2");
        p1.setDefaultValue(null);

        DBPLParam p2 = new DBPLParam();
        p2.setParamName("status");
        p2.setParamMode(DBPLParamMode.OUT);
        p2.setDataType("int");
        p2.setDefaultValue("1");
        List<DBPLParam> expect = Arrays.asList(p1, p2);
        Assert.assertEquals(expect, actual);
    }

    private static List<String> getOraclePLContent() throws IOException {
        String delimiter = "\\$\\$\\s*";
        try (InputStream input = CallProcedureCallBackTest.class.getClassLoader()
                .getResourceAsStream("sql/util/call_procedure_callback_oracle.sql")) {
            byte[] buffer = new byte[input.available()];
            IOUtils.readFully(input, buffer);
            StringSubstitutor substitutor = StringSubstitutor.createInterpolator();
            return new ArrayList<>(Arrays.asList(substitutor.replace(new String(buffer)).split(delimiter)));
        }
    }

    private static List<String> getMySQLPLContent() throws IOException {
        String delimiter = "\\$\\$\\s*";
        try (InputStream input = CallProcedureCallBackTest.class.getClassLoader()
                .getResourceAsStream("sql/util/call_procedure_callback_mysql.sql")) {
            byte[] buffer = new byte[input.available()];
            IOUtils.readFully(input, buffer);
            StringSubstitutor substitutor = StringSubstitutor.createInterpolator();
            return new ArrayList<>(Arrays.asList(substitutor.replace(new String(buffer)).split(delimiter)));
        }
    }

}
