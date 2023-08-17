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

import com.oceanbase.odc.service.db.model.CallFunctionReq;
import com.oceanbase.odc.service.db.model.CallFunctionResp;
import com.oceanbase.odc.service.db.model.CursorResultSet;
import com.oceanbase.odc.service.db.model.OdcDBTableColumn;
import com.oceanbase.odc.service.db.model.PLOutParam;
import com.oceanbase.odc.service.session.model.DBResultSetMetaData;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBPLParamMode;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

public class OBOracleCallFunctionBlockCallBackTest {

    public static final String TEST_CASE_1 = "PL_TEST_1";
    public static final String TEST_CASE_2 = "PL_TEST_2";
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
        oracle.execute("DROP FUNCTION " + TEST_CASE_1);
        oracle.execute("DROP FUNCTION " + TEST_CASE_2);
        oracle.execute("DROP FUNCTION " + TEST_CASE_VARCHAR2_1);
        oracle.execute("DROP FUNCTION " + TEST_CASE_VARCHAR_1);
        oracle.execute("DROP FUNCTION " + TEST_CASE_CHAR_1);
        oracle.execute("DROP FUNCTION " + TEST_CASE_NUMBER_1);
        oracle.execute("DROP FUNCTION " + TEST_CASE_INT_1);
        oracle.execute("DROP FUNCTION " + TEST_CASE_BINARY_INTEGER_1);
        oracle.execute("DROP FUNCTION " + TEST_CASE_INTEGER_1);
        oracle.execute("DROP FUNCTION " + TEST_CASE_DATE_1);
        oracle.execute("DROP FUNCTION " + TEST_CASE_BOOLEAN_1);
        oracle.execute("DROP FUNCTION " + TEST_CASE_CLOB_1);
        oracle.execute("DROP FUNCTION " + TEST_CASE_BLOB_1);
        oracle.execute("DROP FUNCTION " + TEST_CASE_OBJECT_1);
        oracle.execute("DROP FUNCTION " + TEST_CASE_ARRAY_1);
        oracle.execute("DROP FUNCTION " + TEST_CASE_SYS_REFCURSOR_1);
        oracle.execute("DROP TABLE " + TEST_TABLE_A);
        oracle.execute("DROP TYPE " + TEST_OBJECT_CUTOM_TYPE);
        oracle.execute("DROP TYPE " + TEST_ARRAY_INT_ARRAY);
    }

    @Test
    public void doInConnection_whenOracleMode_callFunctionSucceed() {
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("int");
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 int := 1;\n"
                + "  p2 int := 2;\n"
                + "  p3 int := null;\n"
                + "  result int := null;\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("int");
        plOutParam.setValue("3");
        expect.setReturnValue(plOutParam);

        List<PLOutParam> plOutParamList = new ArrayList<>();
        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setCursorResultSet(null);
        plOutParam.setDataType("int");
        plOutParam.setValue("3");
        plOutParamList.add(plOutParam);
        expect.setOutParams(plOutParamList);

        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_whenOracleMode_callFunction_2_Succeed() {
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_2);
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
        param.setDataType("INTEGER");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("DATE");
        param.setParamMode(DBPLParamMode.INOUT);
        list.add(param);

        dbFunction.setParams(list);
        dbFunction.setReturnType("varchar2");
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 varchar2(32767) := 'odc';\n"
                + "  p2 integer;\n"
                + "  p3 DATE := TO_DATE('2020-12-12', 'yyyy-mm-dd');\n"
                + "  result varchar2(32767);\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_2\"(p1 => p1, p2 => p2, p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("varchar2");
        plOutParam.setValue("This is return value");
        expect.setReturnValue(plOutParam);

        List<PLOutParam> plOutParamList = new ArrayList<>();
        plOutParam = new PLOutParam();
        plOutParam.setParamName("p2");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setCursorResultSet(null);
        plOutParam.setDataType("INTEGER");
        plOutParam.setValue("420");
        plOutParamList.add(plOutParam);

        plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setParamMode(DBPLParamMode.INOUT);
        plOutParam.setCursorResultSet(null);
        plOutParam.setDataType("DATE");
        plOutParam.setValue("2020-12-12");
        plOutParamList.add(plOutParam);
        expect.setOutParams(plOutParamList);

        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_varchar2_callSucceed_1() {
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_VARCHAR2_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("varchar2");
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 varchar2(32767) := 'odc';\n"
                + "  p2 varchar2(32767);\n"
                + "  p3 varchar2(32767) := 'odc';\n"
                + "  result varchar2(32767);\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_VARCHAR2_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("varchar2");
        plOutParam.setValue("hello,world");
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
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
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_VARCHAR2_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("varchar2");
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 varchar2(32767) := null;\n"
                + "  p2 varchar2(32767);\n"
                + "  p3 varchar2(32767) := null;\n"
                + "  result varchar2(32767);\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_VARCHAR2_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("varchar2");
        plOutParam.setValue("hello,world");
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
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
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_VARCHAR_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("varchar");
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 varchar(4000) := 'odc';\n"
                + "  p2 varchar(4000);\n"
                + "  p3 varchar(4000) := 'odc';\n"
                + "  result varchar(4000);\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_VARCHAR_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("varchar");
        plOutParam.setValue("hello,world");
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
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
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_VARCHAR_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("varchar");
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 varchar(4000) := null;\n"
                + "  p2 varchar(4000);\n"
                + "  p3 varchar(4000) := null;\n"
                + "  result varchar(4000);\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_VARCHAR_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("varchar");
        plOutParam.setValue("hello,world");
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
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
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_CHAR_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("char");
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 char(32) := 'odc';\n"
                + "  p2 char(32);\n"
                + "  p3 char(3) := 'odc';\n"
                + "  result char(3);\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_CHAR_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("char");
        plOutParam.setValue("odc");
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
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
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_CHAR_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("char");
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 char(32) := null;\n"
                + "  p2 char(32);\n"
                + "  p3 char(3) := null;\n"
                + "  result char(3);\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_CHAR_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("char");
        plOutParam.setValue("odc");
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
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
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_NUMBER_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("number");
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 number(10,5) := 100.001;\n"
                + "  p2 number(10,5);\n"
                + "  p3 number(10,5) := 100.001;\n"
                + "  result number(10,5);\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_NUMBER_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("number");
        plOutParam.setValue("347.52");
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
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
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_NUMBER_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("number");
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 number(10,5) := null;\n"
                + "  p2 number(10,5);\n"
                + "  p3 number(10,5) := null;\n"
                + "  result number(10,5);\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_NUMBER_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("number");
        plOutParam.setValue("347.52");
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
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
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_INT_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("int");
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 int := 101;\n"
                + "  p2 int;\n"
                + "  p3 int := 101;\n"
                + "  result int;\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_INT_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("int");
        plOutParam.setValue("347");
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
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
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_INT_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("int");
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 int := null;\n"
                + "  p2 int;\n"
                + "  p3 int := null;\n"
                + "  result int;\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_INT_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("int");
        plOutParam.setValue("347");
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
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
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_BINARY_INTEGER_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("BINARY_INTEGER");
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 BINARY_INTEGER := 101;\n"
                + "  p2 BINARY_INTEGER;\n"
                + "  p3 BINARY_INTEGER := 101;\n"
                + "  result BINARY_INTEGER;\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_BINARY_INTEGER_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("BINARY_INTEGER");
        plOutParam.setValue("347");
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
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
    public void doInConnection_Integer_callSucceed_1() {
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_INTEGER_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("INTEGER");
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 INTEGER := 101;\n"
                + "  p2 INTEGER;\n"
                + "  p3 INTEGER := 101;\n"
                + "  result INTEGER;\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_INTEGER_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("INTEGER");
        plOutParam.setValue("347");
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
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
    public void doInConnection_Integer_callSucceed_2() {
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_INTEGER_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("INTEGER");
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 INTEGER := null;\n"
                + "  p2 INTEGER;\n"
                + "  p3 INTEGER := null;\n"
                + "  result INTEGER;\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_INTEGER_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("INTEGER");
        plOutParam.setValue("347");
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
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
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_DATE_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("date");
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 date := TO_DATE('2020-12-12', 'yyyy-mm-dd');\n"
                + "  p2 date;\n"
                + "  p3 date := TO_DATE('2020-12-12', 'yyyy-MM-dd');\n"
                + "  result date;\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_DATE_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("date");
        plOutParam.setValue("2020-12-12");
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
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
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_DATE_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("date");
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 date := null;\n"
                + "  p2 date;\n"
                + "  p3 date := null;\n"
                + "  result date;\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_DATE_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("date");
        plOutParam.setValue("2020-12-12");
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
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
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_BOOLEAN_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("BOOLEAN");
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 BOOLEAN := TRUE;\n"
                + "  p2 BOOLEAN;\n"
                + "  p3 BOOLEAN := TRUE;\n"
                + "  result BOOLEAN;\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_BOOLEAN_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("BOOLEAN");
        plOutParam.setValue(String.valueOf(true));
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
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
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_BOOLEAN_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("BOOLEAN");
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 BOOLEAN := null;\n"
                + "  p2 BOOLEAN;\n"
                + "  p3 BOOLEAN := null;\n"
                + "  result BOOLEAN;\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_BOOLEAN_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("BOOLEAN");
        plOutParam.setValue(String.valueOf(true));
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
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
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_CLOB_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("clob");
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 CLOB := 'odc';\n"
                + "  p2 CLOB;\n"
                + "  p3 CLOB := 'odc';\n"
                + "  result CLOB;\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_CLOB_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("clob");
        plOutParam.setValue("odc");
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
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
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_CLOB_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("clob");
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 CLOB := null;\n"
                + "  p2 CLOB;\n"
                + "  p3 CLOB := null;\n"
                + "  result CLOB;\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_CLOB_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("clob");
        plOutParam.setValue("odc");
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
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
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_BLOB_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("varchar2");
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 BLOB := UTL_RAW.CAST_TO_RAW('odc');\n"
                + "  p2 VARCHAR2(32767);\n"
                + "  result VARCHAR2(32767);\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_BLOB_1\"(p1 => p1,p2 => p2);\n"
                + "  p2 := p2;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("varchar2");
        plOutParam.setValue("odc");
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
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
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_BLOB_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("varchar2");
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 BLOB := null;\n"
                + "  p2 VARCHAR2(32767);\n"
                + "  result VARCHAR2(32767);\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_BLOB_1\"(p1 => p1,p2 => p2);\n"
                + "  p2 := p2;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("varchar2");
        plOutParam.setValue("odc");
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
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
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_OBJECT_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("cutom_type");
        dbFunction.setReturnExtendedType(true);
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 cutom_type := cutom_type(101, 'ODC');\n"
                + "  p2 cutom_type;\n"
                + "  p3 cutom_type := cutom_type(101, 'ODC');\n"
                + "  result cutom_type;\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_OBJECT_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("cutom_type");
        plOutParam.setValue("<Ext>");
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
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
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_OBJECT_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("cutom_type");
        dbFunction.setReturnExtendedType(true);
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 cutom_type := null;\n"
                + "  p2 cutom_type;\n"
                + "  p3 cutom_type := null;\n"
                + "  result cutom_type;\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_OBJECT_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("cutom_type");
        plOutParam.setValue("<Ext>");
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
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
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_ARRAY_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("int_array");
        dbFunction.setReturnExtendedType(true);
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 int_array := int_array(1,2,3,4,5,6);\n"
                + "  p2 int_array;\n"
                + "  p3 int_array := int_array(1,2,3,4,5,6);\n"
                + "  result int_array;\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_ARRAY_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("int_array");
        plOutParam.setValue("<Ext>");
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
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
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_ARRAY_1);
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

        dbFunction.setParams(list);
        dbFunction.setReturnType("int_array");
        dbFunction.setReturnExtendedType(true);
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 int_array := null;\n"
                + "  p2 int_array;\n"
                + "  p3 int_array := null;\n"
                + "  result int_array;\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_ARRAY_1\"(p1 => p1,p2 => p2,p3 => p3);\n"
                + "  p2 := p2;\n"
                + "  p3 := p3;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callback = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("int_array");
        plOutParam.setValue("<Ext>");
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
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
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction dbFunction = new DBFunction();
        dbFunction.setFunName(TEST_CASE_SYS_REFCURSOR_1);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue(null);
        param.setDataType("SYS_REFCURSOR");
        param.setExtendedType(true);
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);

        dbFunction.setParams(list);
        dbFunction.setReturnType("SYS_REFCURSOR");
        dbFunction.setReturnExtendedType(true);
        callFunctionReq.setFunction(dbFunction);
        callFunctionReq.setAnonymousBlockDdl("DECLARE\n"
                + "  p1 SYS_REFCURSOR;\n"
                + "  result SYS_REFCURSOR;\n"
                + "BEGIN\n"
                + "  result :=\n"
                + "  \"PL_TEST_SYS_REFCURSOR_1\"(p1 => p1);\n"
                + "  p1 := p1;\n"
                + "END;");

        ConnectionCallback<CallFunctionResp> callBack = new OBOracleCallFunctionBlockCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callBack);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("result");
        plOutParam.setDataType("SYS_REFCURSOR");
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
        expect.setReturnValue(plOutParam);

        List<PLOutParam> outParams = new ArrayList<>();
        plOutParam = new PLOutParam();
        plOutParam.setParamName("p1");
        plOutParam.setDataType("SYS_REFCURSOR");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setValue("<Cursor>");
        cursorResultSet = new CursorResultSet();
        dbResultSetMetaData = new DBResultSetMetaData();
        columnList = new ArrayList<>();
        odcdbTableColumn1 = new OdcDBTableColumn();
        odcdbTableColumn1.setColumnName("A");
        odcdbTableColumn1.setTypeName("VARCHAR2");
        columnList.add(odcdbTableColumn1);

        odcdbTableColumn2 = new OdcDBTableColumn();
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
        try (InputStream input = OBOracleCallFunctionBlockCallBackTest.class.getClassLoader()
                .getResourceAsStream("db/oracle_function_callback_test.sql")) {
            byte[] buffer = new byte[input.available()];
            IOUtils.readFully(input, buffer);
            StringSubstitutor substitutor = StringSubstitutor.createInterpolator();
            return new ArrayList<>(Arrays.asList(substitutor.replace(new String(buffer)).split(delimiter)));
        }
    }

}
