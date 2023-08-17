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

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBPLSqlSecurity;
import com.oceanbase.tools.dbbrowser.model.DBRoutineCharacteristic;
import com.oceanbase.tools.dbbrowser.model.DBRoutineDataNature;
import com.oceanbase.tools.dbbrowser.template.mysql.MySQLFunctionTemplate;

/**
 * {@link MySQLFunctionTemplateTest}
 *
 * @author yh263208
 * @date 2023-02-22 14:43
 * @since db-browser_1.0.0-SNAPSHOT
 */
public class MySQLFunctionTemplateTest {

    @Test
    public void generateCreateObjectTemplate_functionWithParams_generateSucceed() {
        MySQLFunctionTemplate template = new MySQLFunctionTemplate();
        DBFunction function = DBFunction.of("f_test", "int");
        function.setParams(Arrays.asList(DBPLParam.of("p1", null, "int"),
                DBPLParam.of("p2", null, "varchar(100)")));
        String expect = "create function `f_test`(\n"
                + "\tp1 int,\n"
                + "\tp2 varchar(100))\n"
                + "returns int\n"
                + "begin\n"
                + "\t-- Enter your function code\n"
                + "end";
        Assert.assertEquals(expect, template.generateCreateObjectTemplate(function));
    }

    @Test
    public void generateCreateObjectTemplate_functionWithoutParams_generateSucceed() {
        MySQLFunctionTemplate template = new MySQLFunctionTemplate();
        DBFunction function = DBFunction.of("f_test", "int");
        String expect = "create function `f_test`()\n"
                + "returns int\n"
                + "begin\n"
                + "\t-- Enter your function code\n"
                + "end";
        Assert.assertEquals(expect, template.generateCreateObjectTemplate(function));
    }

    @Test
    public void generateCreateObjectTemplate_whithDeterministic() {
        MySQLFunctionTemplate template = new MySQLFunctionTemplate();
        DBFunction function = DBFunction.of("f_test", "int");
        DBRoutineCharacteristic mySQLCharacteristic = new DBRoutineCharacteristic();
        mySQLCharacteristic.setDeterministic(true);
        function.setCharacteristic(mySQLCharacteristic);
        String expect = "create function `f_test`()\n"
                + "returns int\n"
                + " DETERMINISTIC\n"
                + "begin\n"
                + "\t-- Enter your function code\n"
                + "end";
        Assert.assertEquals(expect, template.generateCreateObjectTemplate(function));
    }

    @Test
    public void generateCreateObjectTemplate_whithDataNature() {
        MySQLFunctionTemplate template = new MySQLFunctionTemplate();
        DBFunction function = DBFunction.of("f_test", "int");
        DBRoutineCharacteristic mySQLCharacteristic = new DBRoutineCharacteristic();
        mySQLCharacteristic.setDataNature(DBRoutineDataNature.NO_SQL);
        function.setCharacteristic(mySQLCharacteristic);
        String expect = "create function `f_test`()\n"
                + "returns int\n"
                + " NO SQL\n"
                + "begin\n"
                + "\t-- Enter your function code\n"
                + "end";
        Assert.assertEquals(expect, template.generateCreateObjectTemplate(function));
    }

    @Test
    public void generateCreateObjectTemplate_whithComment() {
        MySQLFunctionTemplate template = new MySQLFunctionTemplate();
        DBFunction function = DBFunction.of("f_test", "int");
        DBRoutineCharacteristic mySQLCharacteristic = new DBRoutineCharacteristic();
        mySQLCharacteristic.setComment("test");
        function.setCharacteristic(mySQLCharacteristic);
        String expect = "create function `f_test`()\n"
                + "returns int\n"
                + " COMMENT \'test\'\n"
                + "begin\n"
                + "\t-- Enter your function code\n"
                + "end";
        Assert.assertEquals(expect, template.generateCreateObjectTemplate(function));
    }

    @Test
    public void generateCreateObjectTemplate_whithSqlSecurity() {
        MySQLFunctionTemplate template = new MySQLFunctionTemplate();
        DBFunction function = DBFunction.of("f_test", "int");
        DBRoutineCharacteristic mySQLCharacteristic = new DBRoutineCharacteristic();
        mySQLCharacteristic.setSqlSecurity(DBPLSqlSecurity.DEFINER);
        function.setCharacteristic(mySQLCharacteristic);
        String expect = "create function `f_test`()\n"
                + "returns int\n"
                + " SQL SECURITY DEFINER\n"
                + "begin\n"
                + "\t-- Enter your function code\n"
                + "end";
        Assert.assertEquals(expect, template.generateCreateObjectTemplate(function));
    }

    @Test
    public void generateCreateObjectTemplate_whithAllCharacteristic() {
        MySQLFunctionTemplate template = new MySQLFunctionTemplate();
        DBFunction function = DBFunction.of("f_test", "int");
        DBRoutineCharacteristic mySQLCharacteristic = new DBRoutineCharacteristic();
        mySQLCharacteristic.setComment("test");
        mySQLCharacteristic.setDeterministic(false);
        mySQLCharacteristic.setDataNature(DBRoutineDataNature.CONTAINS_SQL);
        mySQLCharacteristic.setSqlSecurity(DBPLSqlSecurity.DEFINER);
        function.setCharacteristic(mySQLCharacteristic);
        String expect = "create function `f_test`()\n"
                + "returns int\n"
                + " COMMENT \'test\'\n"
                + " CONTAINS SQL\n"
                + " SQL SECURITY DEFINER\n"
                + "begin\n"
                + "\t-- Enter your function code\n"
                + "end";
        Assert.assertEquals(expect, template.generateCreateObjectTemplate(function));
    }

}
