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

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.service.db.model.PLIdentity;
import com.oceanbase.odc.service.pldebug.operator.DBPLOperator;
import com.oceanbase.odc.service.pldebug.operator.DBPLOperators;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBPackage;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;

/**
 * @author wenniu.ly
 * @date 2022/6/27
 */
public class GetPLObjectTest {
    private static final String CREATE_FUNCTION =
            "create or replace function fun_out(a out int) return integer as v1 int;\n"
                    + "begin\n"
                    + "a := 1;\n"
                    + "v1 := a;\n"
                    + "return v1;\n"
                    + "end";
    private static final String CREATE_PROCEDURE = "create or replace PROCEDURE SEASON\n"
            + "        (\n"
            + "           month in int\n"
            + "        ) IS \n"
            + "        BEGIN\n"
            + "            if month >=1 and month <= 3 then\n"
            + "                dbms_output.put_line('春季');\n"
            + "            elsif month >= 4 and month <=6 THEN\n"
            + "                dbms_output.put_line('夏季');\n"
            + "            elsif month >= 7 and month <= 9 THEN\n"
            + "                dbms_output.put_line('秋季');\n"
            + "            elsif month >= 10 and month <= 12 THEN\n"
            + "                dbms_output.put_line('冬季');\n"
            + "            end if;\n"
            + "        END";
    private static final String CREATE_PACKAGE_HEAD =
            "create or replace PACKAGE PKG1 AS FUNCTION fun_example (p1 IN NUMBER) RETURN NUMBER; PROCEDURE proc_example(p1 IN NUMBER); END PKG1";
    private static final String CREATE_PACKAGE_BODY =
            "create or replace PACKAGE BODY PKG1 AS FUNCTION fun_example (p1 IN NUMBER) RETURN NUMBER AS BEGIN return p1; END; PROCEDURE proc_example(p1 IN NUMBER) AS BEGIN dbms_output.put_line(p1); END; END PKG1";

    private ConnectionSession connectionSession;
    private DBPLOperator dbPlOperator;

    private JdbcOperations jdbcOperations;

    @Before
    public void setUp() throws Exception {

        // insert several pl
        connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);

        DataSource dataSource = TestDBConfigurations.getInstance().getTestOBOracleConfiguration().getDataSource();
        jdbcOperations = new JdbcTemplate(dataSource);

        dbPlOperator = DBPLOperators.create(connectionSession);
        jdbcOperations.execute(CREATE_FUNCTION);
        jdbcOperations.execute(CREATE_PROCEDURE);
        jdbcOperations.execute(CREATE_PACKAGE_HEAD);
        jdbcOperations.execute(CREATE_PACKAGE_BODY);
    }

    @After
    public void tearDown() throws Exception {

        jdbcOperations.execute("drop function fun_out");
        jdbcOperations.execute("drop procedure season");
        // The reason DROP PACKAGE first, because of 'drop package PKG1' will drop body and specification
        jdbcOperations.execute("drop package body PKG1");
        jdbcOperations.execute("drop package PKG1");
    }

    @Test
    public void test_oracle_fetch_procedure() throws Exception {
        Object procedureObj = dbPlOperator.getPLObject(PLIdentity.of(null, "SEASON"));
        Assert.assertTrue(procedureObj instanceof DBProcedure);
        DBProcedure procedure = (DBProcedure) procedureObj;
        Assert.assertEquals("SEASON", procedure.getProName());
        Assert.assertEquals(CREATE_PROCEDURE, procedure.getDdl());
    }

    @Test
    public void test_oracle_fetch_function() throws Exception {
        Object functionObj = dbPlOperator.getPLObject(PLIdentity.of(null, "FUN_OUT"));
        Assert.assertTrue(functionObj instanceof DBFunction);
        DBFunction function = (DBFunction) functionObj;
        Assert.assertEquals("FUN_OUT", function.getFunName());
        Assert.assertEquals(CREATE_FUNCTION, function.getDdl());
    }

    @Test
    public void test_oracle_fetch_package() throws Exception {
        Object packageObj = dbPlOperator.getPLObject(PLIdentity.of(DBObjectType.PACKAGE, "PKG1"));
        Assert.assertTrue(packageObj instanceof DBPackage);
        DBPackage odcPackage = (DBPackage) packageObj;
        Assert.assertEquals("PKG1", odcPackage.getPackageName());
        Assert.assertEquals(CREATE_PACKAGE_HEAD, odcPackage.getPackageHead().getBasicInfo().getDdl());
    }

    @Test
    public void test_oracle_fetch_package_body() throws Exception {
        Object packageObj = dbPlOperator.getPLObject(PLIdentity.of(DBObjectType.PACKAGE_BODY, "PKG1"));
        Assert.assertTrue(packageObj instanceof DBPackage);
        DBPackage odcPackage = (DBPackage) packageObj;
        Assert.assertEquals("PKG1", odcPackage.getPackageName());
        Assert.assertEquals(CREATE_PACKAGE_BODY, odcPackage.getPackageBody().getBasicInfo().getDdl());
    }
}
