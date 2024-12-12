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
import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBPLParamMode;

/**
 * {@link OBOracleCallFunctionCallBackTest}
 *
 * @author yh263208
 * @date 2023-03-06 11:00
 * @since ODC_release_4.2.0
 */
public class OBOracleCallFunctionCallBackTest {

    public static final String TEST_CASE_1 = "FUNC_TEST";
    public static final String TEST_CASE_2 = "FUNC_TEST_1";

    @BeforeClass
    public static void setUp() throws IOException {
        JdbcTemplate oracle =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        getContent().forEach(oracle::execute);
    }

    @AfterClass
    public static void clear() {
        JdbcTemplate oracle =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        oracle.execute("DROP FUNCTION " + TEST_CASE_1);
        oracle.execute("DROP FUNCTION " + TEST_CASE_2);
    }

    @Test
    public void doInConnection_functionWithReturnValue_getReturnValueSucceed() {
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

        param = new DBPLParam();
        param.setParamName("n1");
        param.setDefaultValue(null);
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("n2");
        param.setDefaultValue(null);
        param.setDataType("varchar2");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("n3");
        param.setDefaultValue("");
        param.setDataType("varchar2");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);
        function.setParams(list);
        function.setReturnType("int");

        OBOracleCallFunctionCallBack callBack = new OBOracleCallFunctionCallBack(function, 30);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        DBFunction actual = jdbcTemplate.execute(callBack);

        DBFunction expect = new DBFunction();
        expect.setParams(null);
        expect.setFunName(TEST_CASE_1);
        expect.setReturnType("int");
        expect.setReturnValue("20");
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void doInConnection_functionWithOutParam_getOutParamValueSucceed() {
        DBFunction function = new DBFunction();
        function.setFunName(TEST_CASE_2);
        function.setReturnType("int");

        List<DBPLParam> list = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("p1");
        param.setDefaultValue("100");
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("p2");
        param.setDefaultValue("100");
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.IN);
        list.add(param);

        param = new DBPLParam();
        param.setParamName("total");
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.OUT);
        list.add(param);
        function.setParams(list);

        OBOracleCallFunctionCallBack callBack = new OBOracleCallFunctionCallBack(function, 30);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource());
        DBFunction actual = jdbcTemplate.execute(callBack);

        DBFunction expect = new DBFunction();
        expect.setFunName(TEST_CASE_2);
        expect.setReturnValue("200");
        expect.setReturnType("int");

        param = new DBPLParam();
        param.setSeqNum(0);
        param.setDefaultValue("200");
        param.setParamName("total");
        param.setDataType("int");
        param.setParamMode(DBPLParamMode.OUT);
        expect.setParams(Collections.singletonList(param));
        Assert.assertEquals(expect, actual);
    }

    private static List<String> getContent() throws IOException {
        String delimiter = "\\$\\$\\s*";
        try (InputStream input = CallProcedureCallBackTest.class.getClassLoader()
                .getResourceAsStream("sql/util/ob_oracle_call_function_callback.sql")) {
            byte[] buffer = new byte[input.available()];
            IOUtils.readFully(input, buffer);
            StringSubstitutor substitutor = StringSubstitutor.createInterpolator();
            return new ArrayList<>(Arrays.asList(substitutor.replace(new String(buffer)).split(delimiter)));
        }
    }

}
