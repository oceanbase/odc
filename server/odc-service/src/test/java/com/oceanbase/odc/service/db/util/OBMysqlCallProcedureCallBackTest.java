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
import com.oceanbase.odc.service.db.model.PLOutParam;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBPLParamMode;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;

public class OBMysqlCallProcedureCallBackTest {

    public static final String TEST_CASE_2 = "pl_test_2";


    @BeforeClass
    public static void setUp() throws IOException {
        JdbcTemplate mysql =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBMysqlConfiguration().getDataSource());
        getMySQLPLContent().forEach(mysql::execute);
    }

    @AfterClass
    public static void clear() {
        JdbcTemplate mysql =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBMysqlConfiguration().getDataSource());
        mysql.execute("drop procedure " + TEST_CASE_2);
    }

    @Test
    public void doInConnection_whenMySQLMode_callSucceed() {
        CallProcedureReq callProcedureReq = new CallProcedureReq();
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
        callProcedureReq.setProcedure(procedure);

        ConnectionCallback<CallProcedureResp> callBack =
                new OBMysqlCallProcedureCallBack(callProcedureReq, -1);
        JdbcTemplate jdbcTemplate =
                new JdbcTemplate(TestDBConfigurations.getInstance().getTestOBMysqlConfiguration().getDataSource());
        CallProcedureResp acutal = jdbcTemplate.execute(callBack);

        CallProcedureResp expect = new CallProcedureResp();
        List<PLOutParam> outParams = new ArrayList<>();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setParamName("p3");
        plOutParam.setParamMode(DBPLParamMode.OUT);
        plOutParam.setDataType("int");
        plOutParam.setValue("20");
        outParams.add(plOutParam);
        expect.setOutParams(outParams);
        Assert.assertEquals(expect, acutal);
    }

    private static List<String> getMySQLPLContent() throws IOException {
        String delimiter = "\\$\\$\\s*";
        try (InputStream input = OBMysqlCallProcedureCallBack.class.getClassLoader()
                .getResourceAsStream("db/mysql_procedure_callback_test.sql")) {
            byte[] buffer = new byte[input.available()];
            IOUtils.readFully(input, buffer);
            StringSubstitutor substitutor = StringSubstitutor.createInterpolator();
            return new ArrayList<>(Arrays.asList(substitutor.replace(new String(buffer)).split(delimiter)));
        }
    }

}

