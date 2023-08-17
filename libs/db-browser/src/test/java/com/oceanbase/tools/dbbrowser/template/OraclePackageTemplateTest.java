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
package com.oceanbase.tools.dbbrowser.template;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.tools.dbbrowser.env.BaseTestEnv;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBPackage;
import com.oceanbase.tools.dbbrowser.template.oracle.OraclePackageTemplate;

/**
 * {@link OraclePackageTemplateTest}
 *
 * @author yh263208
 * @date 2022-02-22 17:06
 * @since db-browser-1.0.0-SNAPSHOT
 */
public class OraclePackageTemplateTest extends BaseTestEnv {

    @Test
    public void generateCreateObjectTemplate_package_generateSucceed() {
        DBObjectTemplate<DBPackage> editor = new OraclePackageTemplate(new JdbcTemplate(getOBOracleDataSource()));
        DBPackage dbPackage = DBPackage.ofPackage("test_pkg");
        String expect = "CREATE OR REPLACE PACKAGE \"test_pkg\" AS\n"
                + "\tFUNCTION FUNC_EXAMPLE (p1 IN NUMBER) RETURN NUMBER;\n"
                + "\tPROCEDURE PROC_EXAMPLE (p1 IN NUMBER);\n"
                + "END";
        Assert.assertEquals(expect, editor.generateCreateObjectTemplate(dbPackage));
    }

    @Test
    public void generateCreateObjectTemplate_packageBody_generateSucceed() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(getOBOracleDataSource());
        DBObjectTemplate<DBPackage> editor = new OraclePackageTemplate(jdbcTemplate);
        DBPackage dbPackage = DBPackage.ofPackage("TEST_PKG");
        jdbcTemplate.execute(editor.generateCreateObjectTemplate(dbPackage));

        dbPackage = DBPackage.ofPackageBody("TEST_PKG");
        String expect = "CREATE OR REPLACE PACKAGE BODY TEST_PKG AS\n\n"
                + "FUNCTION FUNC_EXAMPLE (p1 IN NUMBER) RETURN NUMBER AS\n"
                + "BEGIN\n"
                + "\t-- TODO\n"
                + "END;\n\n"
                + "PROCEDURE PROC_EXAMPLE (p1 IN NUMBER) AS\n"
                + "BEGIN\n"
                + "\t-- TODO\n"
                + "END;\n\n"
                + "END TEST_PKG;";
        Assert.assertEquals(expect, editor.generateCreateObjectTemplate(dbPackage));
    }

    @Test
    public void generateCreateObjectTemplate_withVariables_generateSucceed() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(getOBOracleDataSource());
        DBObjectTemplate<DBPackage> editor = new OraclePackageTemplate(jdbcTemplate);
        jdbcTemplate.execute("CREATE OR REPLACE PACKAGE TEST_PKG AS "
                + "\n\tval1 constant integer := -1400;"
                + "\n\tval2 number:=0.1;"
                + "\n\tFUNCTION fun_example (p1 IN NUMBER) RETURN NUMBER;"
                + "\n\tPROCEDURE proc_example(p1 IN NUMBER);"
                + "\nEND TEST_PKG;");
        DBPackage dbPackage = new DBPackage();
        dbPackage.setPackageName("TEST_PKG");
        dbPackage.setPackageType(DBObjectType.PACKAGE_BODY.getName());

        String expect = "CREATE OR REPLACE PACKAGE BODY TEST_PKG AS\n\n"
                + "FUNCTION fun_example (p1 IN NUMBER) RETURN NUMBER AS\n"
                + "BEGIN\n"
                + "\t-- TODO\n"
                + "END;\n\n"
                + "PROCEDURE proc_example(p1 IN NUMBER) AS\n"
                + "BEGIN\n"
                + "\t-- TODO\n"
                + "END;\n\n"
                + "END TEST_PKG;";
        Assert.assertEquals(expect, editor.generateCreateObjectTemplate(dbPackage));
    }

}
