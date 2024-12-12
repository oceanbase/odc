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
package com.oceanbase.odc.service.pldebug;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.service.pldebug.operator.DBPLOperator;
import com.oceanbase.odc.service.pldebug.operator.DBPLOperators;
import com.oceanbase.odc.service.pldebug.util.CallProcedureCallBack;
import com.oceanbase.odc.service.pldebug.util.OBOracleCallFunctionCallBack;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBPLParamMode;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;

/**
 * @author yaobin
 * @date 2023-03-02
 * @since 4.1.0
 */
public class PLDebugTest {

    private static DataSource dataSource;
    private static JdbcOperations jdbcOperations;

    @BeforeClass
    public static void setUp() throws Exception {
        TestDBConfiguration testDBConfiguration = TestDBConfigurations.getInstance()
                .getTestOBOracleConfiguration();

        String createProcedure = "create or replace PROCEDURE proc(A NUMBER, B NUMBER, C OUT NUMBER) IS\n"
                + "  z NUMBER := 0;\n"
                + "BEGIN\n"
                + "  Z := A+B;\n"
                + "  C := Z;\n"
                + "END;";

        String createFunction = "create or replace function func(A INT, B INT) RETURN NUMBER IS\n"
                + "  z NUMBER := 0;\n"
                + "BEGIN\n"
                + "  Z := A * B;\n"
                + "  RETURN Z;\n"
                + "END;";
        dataSource = testDBConfiguration.getDataSource();
        try (
                Statement statement = dataSource.getConnection().createStatement()) {
            statement.execute(createProcedure);
            statement.execute(createFunction);
        }
        jdbcOperations = new JdbcTemplate(dataSource);

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (dataSource != null) {
            try (
                    Statement statement = dataSource.getConnection().createStatement()) {
                statement.execute("drop procedure proc");
                statement.execute("drop function func");
            }
        }
    }

    @Test
    public void test_call_procedure() throws Exception {

        List<DBPLParam> params = new ArrayList<>();
        DBPLParam paramA = DBPLParam.of("A", DBPLParamMode.IN, "INT");
        paramA.setDefaultValue("2");
        params.add(paramA);
        DBPLParam paramB = DBPLParam.of("B", DBPLParamMode.IN, "INT");
        paramB.setDefaultValue("3");
        params.add(paramB);

        DBPLParam paramC = DBPLParam.of("C", DBPLParamMode.OUT, "INT");
        params.add(paramC);
        DBProcedure odcProcedure = DBProcedure.of(null, "PROC", params);

        CallProcedureCallBack procedureCallBack = new CallProcedureCallBack(odcProcedure, 30, new OracleSqlBuilder());
        List<DBPLParam> result = jdbcOperations.execute(procedureCallBack);

        int returnVal = Integer.parseInt(result.get(0).getDefaultValue());

        Assert.assertEquals(5, returnVal);

    }

    @Test
    public void test_call_function() throws Exception {

        List<DBPLParam> params = new ArrayList<>();
        DBPLParam paramA = DBPLParam.of("A", DBPLParamMode.IN, "INT");
        paramA.setDefaultValue("2");
        params.add(paramA);
        DBPLParam paramB = DBPLParam.of("B", DBPLParamMode.IN, "INT");
        paramB.setDefaultValue("3");
        params.add(paramB);

        DBFunction odcFunction = new DBFunction();
        odcFunction.setFunName("FUNC");
        odcFunction.setParams(params);
        OBOracleCallFunctionCallBack callFunctionCallBack = new OBOracleCallFunctionCallBack(odcFunction, 30);
        DBFunction result = jdbcOperations.execute(callFunctionCallBack);
        int returnVal = Integer.parseInt(result.getReturnValue());

        Assert.assertEquals(
                6, returnVal);

    }


    @Test
    public void test_can_debug_pl() throws Exception {

        ConnectionSession connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        DBPLOperator dbplOperator = DBPLOperators.create(connectionSession);

        Boolean flag = dbplOperator.isSupportPLDebug();
        Assert.assertTrue(flag);

    }
}
