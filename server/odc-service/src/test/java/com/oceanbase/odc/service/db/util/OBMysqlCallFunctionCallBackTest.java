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
import com.oceanbase.odc.service.db.model.PLOutParam;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBPLParamMode;

public class OBMysqlCallFunctionCallBackTest {

    public static final String TEST_CASE_1 = "func_test";
    public static final String TEST_CASE_2 = "func_test_1";
    public static final String TEST_CASE_3 = "func_test_2";

    @BeforeClass
    public static void setUp() throws IOException {
        JdbcTemplate mysql =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBMysqlConfiguration().getDataSource());
        getContent().forEach(mysql::execute);
    }

    @AfterClass
    public static void clear() {
        JdbcTemplate mysql =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBMysqlConfiguration().getDataSource());
        mysql.execute("DROP FUNCTION " + TEST_CASE_1);
        mysql.execute("DROP FUNCTION " + TEST_CASE_2);
        mysql.execute("DROP FUNCTION " + TEST_CASE_3);
    }

    @Test
    public void doInConnection_normalFunction_callSucceed() {
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction function = new DBFunction();
        function.setFunName(TEST_CASE_1);
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
        function.setParams(list);
        function.setReturnType("int");
        callFunctionReq.setFunction(function);

        ConnectionCallback<CallFunctionResp> callback = new OBMysqlCallFunctionCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBMysqlConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName(TEST_CASE_1);
        plOutParam.setDataType("int");
        plOutParam.setValue("20");
        expect.setReturnValue(plOutParam);
        expect.setOutParams(null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_nullParamExists_callSucceed() {
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction function = new DBFunction();
        function.setFunName(TEST_CASE_2);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p0");
        param.setDefaultValue("1");
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue("2");
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue(null);
        param.setDataType("varchar");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);
        function.setParams(list);
        function.setReturnType("int");
        callFunctionReq.setFunction(function);

        ConnectionCallback<CallFunctionResp> callback = new OBMysqlCallFunctionCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBMysqlConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName(TEST_CASE_2);
        plOutParam.setDataType("int");
        plOutParam.setValue("1");
        expect.setReturnValue(plOutParam);
        expect.setOutParams(null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_emptyParamExists_callSucceed() {
        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction function = new DBFunction();
        function.setFunName(TEST_CASE_2);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p0");
        param.setDefaultValue("1");
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue("2");
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p3");
        param.setDefaultValue("");
        param.setDataType("varchar");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);
        function.setParams(list);
        function.setReturnType("int");
        callFunctionReq.setFunction(function);

        ConnectionCallback<CallFunctionResp> callback = new OBMysqlCallFunctionCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBMysqlConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName(TEST_CASE_2);
        plOutParam.setDataType("int");
        plOutParam.setValue("2");
        expect.setReturnValue(plOutParam);
        expect.setOutParams(null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_unusualParam_callSucceed() {
        String input = "'in\\put'";

        CallFunctionReq callFunctionReq = new CallFunctionReq();
        DBFunction function = new DBFunction();
        function.setFunName(TEST_CASE_3);
        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p0");
        param.setDefaultValue(input);
        param.setDataType("varchar(20)");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);
        function.setParams(list);
        callFunctionReq.setFunction(function);

        ConnectionCallback<CallFunctionResp> callback = new OBMysqlCallFunctionCallBack(callFunctionReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBMysqlConfiguration().getDataSource());
        CallFunctionResp actual = jdbcTemplate.execute(callback);

        CallFunctionResp expect = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName(TEST_CASE_3);
        plOutParam.setDataType("varchar(20)");
        plOutParam.setValue(input);
        expect.setReturnValue(plOutParam);
        expect.setOutParams(null);
        Assert.assertEquals(expect, actual);
    }

    private static List<String> getContent() throws IOException {
        String delimiter = "\\$\\$\\s*";
        try (InputStream input = OBMysqlCallFunctionCallBackTest.class.getClassLoader()
                .getResourceAsStream("db/mysql_function_callback_test.sql")) {
            byte[] buffer = new byte[input.available()];
            IOUtils.readFully(input, buffer);
            StringSubstitutor substitutor = StringSubstitutor.createInterpolator();
            return new ArrayList<>(Arrays.asList(substitutor.replace(new String(buffer)).split(delimiter)));
        }
    }

}
