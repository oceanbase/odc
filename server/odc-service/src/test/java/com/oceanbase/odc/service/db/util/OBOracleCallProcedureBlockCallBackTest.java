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
package com.oceanbase.odc.service.db.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.service.db.model.CallProcedureReq;
import com.oceanbase.odc.service.db.model.CallProcedureResp;
import com.oceanbase.odc.service.db.model.CursorResultSet;
import com.oceanbase.odc.service.db.model.OdcDBTableColumn;
import com.oceanbase.odc.service.db.model.PLOutParam;
import com.oceanbase.odc.service.session.model.DBResultSetMetaData;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBPLParamMode;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

public class OBOracleCallProcedureBlockCallBackTest {

    public static final String TEST_CASE_1 = "PL_TEST_1";
    public static final String TEST_CASE_2 = "PL_TEST_2";
    public static final String TEST_CASE_3 = "PL_TEST_3";
    public static final String TEST_CASE_4 = "PL_TEST_4";
    public static final String TEST_CASE_5 = "PL_TEST_5";
    public static final String TEST_CASE_VARCHAR2_1 = "PL_TEST_VARCHAR2_1";
    public static final String TEST_CASE_VARCHAR_1 = "PL_TEST_VARCHAR_1";
    public static final String TEST_CASE_CHAR_1 = "PL_TEST_CHAR_1";
    public static final String TEST_CASE_NUMBER_1 = "PL_TEST_NUMBER_1";
    public static final String TEST_CASE_INT_1 = "PL_TEST_INT_1";
    public static final String TEST_CASE_BINARY_INTEGER_1 = "PL_TEST_BINARY_INTEGER_1";
    public static final String TEST_CASE_INTEGER_1 = "PL_TEST_INTEGER_1";
    public static final String TEST_CASE_DATE_1 = "PL_TEST_DATE_1";
    public static final String TEST_CASE_BOOLEAN_1 = "PL_TEST_BOOLEAN_1";
    public static final String TEST_CASE_CLOB_1 = "PL_TEST_CLOB_1";
    public static final String TEST_CASE_BLOB_1 = "PL_TEST_BLOB_1";
    public static final String TEST_CASE_OBJECT_1 = "PL_TEST_OBJECT_1";
    public static final String TEST_CASE_ARRAY_1 = "PL_TEST_ARRAY_1";
    public static final String TEST_CASE_SYS_REFCURSOR_1 = "PL_TEST_SYS_REFCURSOR_1";
    public static final String TEST_TABLE_A = "A";
    public static final String TEST_OBJECT_CUTOM_TYPE = "CUTOM_TYPE";
    public static final String TEST_ARRAY_INT_ARRAY = "INT_ARRAY";

    @BeforeClass
    public static void setUp() throws IOException {
        JdbcTemplate oracle =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        getOraclePLContent().forEach(oracle::execute);
    }

    @AfterClass
    public static void clear() {
        JdbcTemplate oracle =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        oracle.execute("DROP PROCEDURE " + TEST_CASE_1);
        oracle.execute("DROP PROCEDURE " + TEST_CASE_2);
        oracle.execute("DROP PROCEDURE " + TEST_CASE_3);
        oracle.execute("DROP PROCEDURE " + TEST_CASE_4);
        oracle.execute("DROP PROCEDURE " + TEST_CASE_5);
        oracle.execute("DROP PROCEDURE " + TEST_CASE_VARCHAR2_1);
        oracle.execute("DROP PROCEDURE " + TEST_CASE_VARCHAR_1);
        oracle.execute("DROP PROCEDURE " + TEST_CASE_CHAR_1);
        oracle.execute("DROP PROCEDURE " + TEST_CASE_NUMBER_1);
        oracle.execute("DROP PROCEDURE " + TEST_CASE_INT_1);
        oracle.execute("DROP PROCEDURE " + TEST_CASE_BINARY_INTEGER_1);
        oracle.execute("DROP PROCEDURE " + TEST_CASE_INTEGER_1);
        oracle.execute("DROP PROCEDURE " + TEST_CASE_DATE_1);
        oracle.execute("DROP PROCEDURE " + TEST_CASE_BOOLEAN_1);
        oracle.execute("DROP PROCEDURE " + TEST_CASE_CLOB_1);
        oracle.execute("DROP PROCEDURE " + TEST_CASE_BLOB_1);
        oracle.execute("DROP PROCEDURE " + TEST_CASE_OBJECT_1);
        oracle.execute("DROP PROCEDURE " + TEST_CASE_ARRAY_1);
        oracle.execute("DROP PROCEDURE " + TEST_CASE_SYS_REFCURSOR_1);
        oracle.execute("DROP TABLE " + TEST_TABLE_A);
        oracle.execute("DROP TYPE " + TEST_OBJECT_CUTOM_TYPE);
        oracle.execute("DROP TYPE " + TEST_ARRAY_INT_ARRAY);
    }

    @Test
    public void doInConnection_whenOracleMode_callProcedureSucceed() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 int := 1;\n"
                + "  p2 int := 2;\n"
                + "  p3 int;\n"
                + "BEGIN\n"
                + "  \"PL_TEST_1\"(p1=> p1,p2 =>p2,p3 => p3);\n"
                + "  p3 :=p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("int");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("3");

        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_setNullToInParam_callSucceed() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure procedure = new DBProcedure();
        procedure.setProName(TEST_CASE_2);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("n1");
        param.setDefaultValue(null);
        param.setDataType("VARCHAR2");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
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
        param.setDataType("VARCHAR2");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        procedure.setParams(list);
        callProcedureReq.setProcedure(procedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  n1 VARCHAR2(32767) := null;\n"
                + "  p1 int := 5;\n"
                + "  n2 int := null;\n"
                + "  n3 VARCHAR2(32767) := '';\n"
                + "  p2 int := 5;\n"
                + "  p3 int;\n"
                + "BEGIN\n"
                + "  \"PL_TEST_2\"(n1 => n1,p1 =>p1,n2 =>n2,n3 => n3,p2=> p2,p3 => p3);\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("int");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue("10");

        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_whenOracleMode_callSucceed() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
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
        callProcedureReq.setProcedure(procedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 int := 5;\n"
                + "  p2 int := 5;\n"
                + "  p3 int;\n"
                + "BEGIN\n"
                + "  \"PL_TEST_3\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p3 := p3;\n"
                + "END;");
        // 注意版本1.1.0开始
        ConnectionCallback<CallProcedureResp> callBack = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callBack);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("int");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue("10");

        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_whenOracleModeAndOutParam_callSucceed() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure procedure = new DBProcedure();
        procedure.setProName(TEST_CASE_4);
        // dbms_output.get_line
        List<DBPLParam> params = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("LINE");
        param.setParamMode(DBPLParamMode.OUT);
        param.setDataType("varchar2");
        params.add(param);

        param = new DBPLParam();
        param.setParamName("STATUS");
        param.setParamMode(DBPLParamMode.OUT);
        param.setDataType("int");
        params.add(param);

        procedure.setParams(params);
        callProcedureReq.setProcedure(procedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  LINE varchar2(32767);\n"
                + "  STATUS int;\n"
                + "BEGIN\n"
                + "  \"PL_TEST_4\"(LINE => LINE,STATUS => STATUS);\n"
                + "  LINE := LINE;\n"
                + "  STATUS := STATUS;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callBack = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callBack);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("LINE");
        plOutParam.setDataType("varchar2");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue(null);
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("STATUS");
        plOutParam.setDataType("int");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("1");
        outParams.add(plOutParam);

        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_whenOracleModeAndInOutParam_callSucceed() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure procedure = new DBProcedure();
        procedure.setProName(TEST_CASE_5);
        List<DBPLParam> params = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setParamMode(DBPLParamMode.IN);
        param.setDataType("varchar2");
        params.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setParamMode(DBPLParamMode.OUT);
        param.setDataType("INTEGER");
        params.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setParamMode(DBPLParamMode.INOUT);
        param.setDataType("DATE");
        params.add(param);

        procedure.setParams(params);
        callProcedureReq.setProcedure(procedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 varchar2(32767) := 'odc';\n"
                + "  p2 integer;\n"
                + "  p3 DATE := TO_DATE('2020-12-12', 'yyyy-mm-dd');\n"
                + "BEGIN\n"
                + "  \"PL_TEST_5\"(p1 => p1, p2 => p2, p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callBack = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callBack);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("INTEGER");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("420");
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("DATE");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue("2020-12-12");
        outParams.add(plOutParam);

        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_varchar2_callSucceed_1() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_VARCHAR2_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("varchar2");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("varchar2");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("varchar2");
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 varchar2(32767) := 'odc';\n"
                + "  p2 varchar2(32767);\n"
                + "  p3 varchar2(32767) := 'odc';\n"
                + "BEGIN\n"
                + "  \"PL_TEST_VARCHAR2_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("varchar2");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("YIMING");
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("varchar2");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue("odc");
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_varchar2_callSucceed_2() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_VARCHAR2_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("varchar2");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("varchar2");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("varchar2");
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 varchar2(32767) := null;\n"
                + "  p2 varchar2(32767);\n"
                + "  p3 varchar2(32767) := null;\n"
                + "BEGIN\n"
                + "  \"PL_TEST_VARCHAR2_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("varchar2");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("YIMING");
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("varchar2");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue(null);
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_varchar_callSucceed_1() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_VARCHAR_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("varchar");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("varchar");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("varchar");
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 varchar(4000) := 'odc';\n"
                + "  p2 varchar(4000);\n"
                + "  p3 varchar(4000) := 'odc';\n"
                + "BEGIN\n"
                + "  \"PL_TEST_VARCHAR_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("varchar");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("YIMING");
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("varchar");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue("odc");
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_varchar_callSucceed_2() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_VARCHAR_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("varchar");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("varchar");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("varchar");
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 varchar(4000) := null;\n"
                + "  p2 varchar(4000);\n"
                + "  p3 varchar(4000) := null;\n"
                + "BEGIN\n"
                + "  \"PL_TEST_VARCHAR_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("varchar");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("YIMING");
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("varchar");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue(null);
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_char_callSucceed_1() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_CHAR_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("char");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("char");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("char");
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 char(32) := 'odc';\n"
                + "  p2 char(32);\n"
                + "  p3 char(3) := 'odc';\n"
                + "BEGIN\n"
                + "  \"PL_TEST_CHAR_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("char");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("YIMING");
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("char");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue("odc");
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_char_callSucceed_2() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_CHAR_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("char");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("char");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("char");
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 char(32) := null;\n"
                + "  p2 char(32);\n"
                + "  p3 char(3) := null;\n"
                + "BEGIN\n"
                + "  \"PL_TEST_CHAR_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("char");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("YIMING");
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("char");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue(null);
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_number_callSucceed_1() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_NUMBER_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("number");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("number");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("number");
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 number(10,5) := 100.001;\n"
                + "  p2 number(10,5);\n"
                + "  p3 number(10,5) := 100.001;\n"
                + "BEGIN\n"
                + "  \"PL_TEST_NUMBER_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("number");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("100.001");
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("number");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue("100.001");
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_number_callSucceed_2() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_NUMBER_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("number");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("number");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("number");
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 number(10,5) := null;\n"
                + "  p2 number(10,5);\n"
                + "  p3 number(10,5) := null;\n"
                + "BEGIN\n"
                + "  \"PL_TEST_NUMBER_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("number");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("100.001");
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("number");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue(null);
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_int_callSucceed_1() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_INT_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 int := 101;\n"
                + "  p2 int;\n"
                + "  p3 int := 101;\n"
                + "BEGIN\n"
                + "  \"PL_TEST_INT_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("int");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("101");
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("int");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue("101");
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_int_callSucceed_2() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_INT_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 int := null;\n"
                + "  p2 int;\n"
                + "  p3 int := null;\n"
                + "BEGIN\n"
                + "  \"PL_TEST_INT_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("int");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("101");
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("int");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue(null);
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_binaryInteger_callSucceed_1() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_BINARY_INTEGER_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("BINARY_INTEGER");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("BINARY_INTEGER");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("BINARY_INTEGER");
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 BINARY_INTEGER := 101;\n"
                + "  p2 BINARY_INTEGER;\n"
                + "  p3 BINARY_INTEGER := 101;\n"
                + "BEGIN\n"
                + "  \"PL_TEST_BINARY_INTEGER_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("BINARY_INTEGER");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("101");
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("BINARY_INTEGER");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue("101");
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_binaryInteger_callSucceed_2() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_BINARY_INTEGER_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("BINARY_INTEGER");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("BINARY_INTEGER");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("BINARY_INTEGER");
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 BINARY_INTEGER := null;\n"
                + "  p2 BINARY_INTEGER;\n"
                + "  p3 BINARY_INTEGER := null;\n"
                + "BEGIN\n"
                + "  \"PL_TEST_BINARY_INTEGER_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("BINARY_INTEGER");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("101");
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("BINARY_INTEGER");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue(null);
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_Integer_callSucceed_2() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_INTEGER_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("INTEGER");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("INTEGER");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("INTEGER");
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 INTEGER := 101;\n"
                + "  p2 INTEGER;\n"
                + "  p3 INTEGER := 101;\n"
                + "BEGIN\n"
                + "  \"PL_TEST_INTEGER_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("INTEGER");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("101");
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("INTEGER");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue("101");
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_Integer_callSucceed_1() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_INTEGER_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("INTEGER");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("INTEGER");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("INTEGER");
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 INTEGER := null;\n"
                + "  p2 INTEGER;\n"
                + "  p3 INTEGER := null;\n"
                + "BEGIN\n"
                + "  \"PL_TEST_INTEGER_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("INTEGER");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("101");
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("INTEGER");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue(null);
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_date_callSucceed_1() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_DATE_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("date");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("date");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("date");
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 date := TO_DATE('2020-12-12', 'yyyy-mm-dd');\n"
                + "  p2 date;\n"
                + "  p3 date := TO_DATE('2020-12-12', 'yyyy-MM-dd');\n"
                + "BEGIN\n"
                + "  \"PL_TEST_DATE_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("date");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("2020-12-12");
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("date");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue("2020-12-12");
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_date_callSucceed_2() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_DATE_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("date");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("date");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("date");
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 date := null;\n"
                + "  p2 date;\n"
                + "  p3 date := null;\n"
                + "BEGIN\n"
                + "  \"PL_TEST_DATE_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("date");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("2020-12-12");
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("date");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue(null);
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_boolean_callSucceed_1() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_BOOLEAN_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("BOOLEAN");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("BOOLEAN");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("BOOLEAN");
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 BOOLEAN := TRUE;\n"
                + "  p2 BOOLEAN;\n"
                + "  p3 BOOLEAN := TRUE;\n"
                + "BEGIN\n"
                + "  \"PL_TEST_BOOLEAN_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("BOOLEAN");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue(String.valueOf(false));
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("BOOLEAN");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue(String.valueOf(true));
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_boolean_callSucceed_2() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_BOOLEAN_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("BOOLEAN");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("BOOLEAN");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("BOOLEAN");
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 BOOLEAN := null;\n"
                + "  p2 BOOLEAN;\n"
                + "  p3 BOOLEAN := null;\n"
                + "BEGIN\n"
                + "  \"PL_TEST_BOOLEAN_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("BOOLEAN");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue(String.valueOf(false));
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("BOOLEAN");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue(null);
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_clob_callSucceed_1() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_CLOB_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("clob");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("clob");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("clob");
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 CLOB := 'odc';\n"
                + "  p2 CLOB;\n"
                + "  p3 CLOB := 'odc';\n"
                + "BEGIN\n"
                + "  \"PL_TEST_CLOB_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("CLOB");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("YIMING");
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("CLOB");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue("odc");
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }


    @Test
    public void doInConnection_clob_callSucceed_2() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_CLOB_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("clob");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("clob");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("clob");
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 CLOB := null;\n"
                + "  p2 CLOB;\n"
                + "  p3 CLOB := null;\n"
                + "BEGIN\n"
                + "  \"PL_TEST_CLOB_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("CLOB");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("YIMING");
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("CLOB");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue(null);
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_blob_callSucceed_1() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_BLOB_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("blob");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("varchar2");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 BLOB := UTL_RAW.CAST_TO_RAW('odc');\n"
                + "  p2 VARCHAR2(32767);\n"
                + "BEGIN\n"
                + "  \"PL_TEST_BLOB_1\"(p1 => p1,p2 => p2);\n"
                + "  p2 := p2;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("varchar2");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("odc");
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_blob_callSucceed_2() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_BLOB_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("blob");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("varchar2");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 BLOB := null;\n"
                + "  p2 VARCHAR2(32767);\n"
                + "BEGIN\n"
                + "  \"PL_TEST_BLOB_1\"(p1 => p1,p2 => p2);\n"
                + "  p2 := p2;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("varchar2");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue(null);
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_object_callSucceed_1() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_OBJECT_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("cutom_type");
        param.setExtendedType(true);
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("cutom_type");
        param.setExtendedType(true);
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("cutom_type");
        param.setExtendedType(true);
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 cutom_type := cutom_type(101, 'ODC');\n"
                + "  p2 cutom_type;\n"
                + "  p3 cutom_type := cutom_type(101, 'ODC');\n"
                + "BEGIN\n"
                + "  \"PL_TEST_OBJECT_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("cutom_type");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("<Ext>");
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("cutom_type");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue("<Ext>");
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_object_callSucceed_2() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_OBJECT_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("cutom_type");
        param.setExtendedType(true);
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("cutom_type");
        param.setExtendedType(true);
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("cutom_type");
        param.setExtendedType(true);
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 cutom_type := null;\n"
                + "  p2 cutom_type;\n"
                + "  p3 cutom_type := null;\n"
                + "BEGIN\n"
                + "  \"PL_TEST_OBJECT_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("cutom_type");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("<Ext>");
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("cutom_type");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue("<Ext>");
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_array_callSucceed_1() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_ARRAY_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("int_array");
        param.setExtendedType(true);
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("int_array");
        param.setExtendedType(true);
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("int_array");
        param.setExtendedType(true);
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 int_array := int_array(1,2,3,4,5,6);\n"
                + "  p2 int_array;\n"
                + "  p3 int_array := int_array(1,2,3,4,5,6);\n"
                + "BEGIN\n"
                + "  \"PL_TEST_ARRAY_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("int_array");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("<Ext>");
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("int_array");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue("<Ext>");
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_array_callSucceed_2() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_ARRAY_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("int_array");
        param.setExtendedType(true);
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue(null);
        param.setDataType("int_array");
        param.setExtendedType(true);
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("int_array");
        param.setExtendedType(true);
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 int_array := null;\n"
                + "  p2 int_array;\n"
                + "  p3 int_array := null;\n"
                + "BEGIN\n"
                + "  \"PL_TEST_ARRAY_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callback = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callback);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setDataType("int_array");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("<Ext>");
        outParams.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setDataType("int_array");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setValue("<Ext>");
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_sysRefCursor_callSucceed_1() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
        DBProcedure dbProcedure = new DBProcedure();
        dbProcedure.setProName(TEST_CASE_SYS_REFCURSOR_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("SYS_REFCURSOR");
        param.setParamMode(DBPLParamMode.OUT);
        param.setExtendedType(true);
        list.add(param);
        dbProcedure.setParams(list);
        callProcedureReq.setProcedure(dbProcedure);
        callProcedureReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 SYS_REFCURSOR;\n"
                + "BEGIN\n"
                + "  \"PL_TEST_SYS_REFCURSOR_1\"(p1 => p1);\n"
                + "  p1 := p1;\n"
                + "END;");

        ConnectionCallback<CallProcedureResp> callBack = new OBOracleCallProcedureBlockCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallProcedureResp actual = jdbcTemplate.execute(callBack);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p1");
        plOutParam.setDataType("SYS_REFCURSOR");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("<Cursor>");
        CursorResultSet cursorResultSet = new CursorResultSet();
        DBResultSetMetaData dbResultSetMetaData = new DBResultSetMetaData();
        List<DBTableColumn> columnList = new ArrayList<>();
        OdcDBTableColumn odcdbTableColumn1 = new OdcDBTableColumn();
        odcdbTableColumn1.setColumnName("A");
        odcdbTableColumn1.setTypeName("VARCHAR2");
        columnList.add(odcdbTableColumn1);

        OdcDBTableColumn odcdbTableColumn2 = new OdcDBTableColumn();
        odcdbTableColumn2.setColumnName("B");
        odcdbTableColumn2.setTypeName("VARCHAR2");
        columnList.add(odcdbTableColumn2);

        dbResultSetMetaData.setColumnList(columnList);
        cursorResultSet.setResultSetMetaData(dbResultSetMetaData);
        plOutParam.setCursorResultSet(cursorResultSet);
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, actual);
    }

    private static List<String> getOraclePLContent() throws IOException {
        String delimiter = "\\$\\$\\s*";
        try (InputStream input = OBOracleCallProcedureBlockCallBackTest.class.getClassLoader()
                .getResourceAsStream("db/oracle_procedure_callback_test.sql")) {
            byte[] buffer = new byte[input.available()];
            IOUtils.readFully(input, buffer);
            StringSubstitutor substitutor = StringSubstitutor.createInterpolator();
            return new ArrayList<>(Arrays.asList(substitutor.replace(new String(buffer)).split(delimiter)));
        }
    }

}
