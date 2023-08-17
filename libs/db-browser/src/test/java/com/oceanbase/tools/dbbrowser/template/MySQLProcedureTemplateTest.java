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
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBPLParamMode;
import com.oceanbase.tools.dbbrowser.model.DBPLSqlSecurity;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.model.DBRoutineCharacteristic;
import com.oceanbase.tools.dbbrowser.model.DBRoutineDataNature;
import com.oceanbase.tools.dbbrowser.template.mysql.MySQLProcedureTemplate;

/**
 * {@link MySQLProcedureTemplateTest}
 *
 * @author yh263208
 * @date 2022-02-22 17:47
 * @since db-browser-1.0.0-SNAPSHOT
 */
public class MySQLProcedureTemplateTest {

    @Test
    public void generateCreateObjectTemplate_procedureWithParams_generateSucceed() {
        DBObjectTemplate<DBProcedure> template = new MySQLProcedureTemplate();
        DBPLParam p1 = DBPLParam.of("p1", DBPLParamMode.IN, "int");
        DBPLParam p2 = DBPLParam.of("p2", DBPLParamMode.INOUT, "varchar(100)");
        DBProcedure procedure = DBProcedure.of(null, "test_proc", Arrays.asList(p1, p2));

        String expect = "create procedure `test_proc`(\n"
                + "\tin `p1` int,\n"
                + "\tinout `p2` varchar(100))\n"
                + "begin\n"
                + "\t-- Enter your procedure code\n"
                + "end";
        Assert.assertEquals(expect, template.generateCreateObjectTemplate(procedure));
    }

    @Test
    public void generateCreateObjectTemplate_procedureWithoutParams_generateSucceed() {
        DBObjectTemplate<DBProcedure> template = new MySQLProcedureTemplate();
        DBProcedure procedure = DBProcedure.of(null, "test_proc", Collections.emptyList());

        String expect = "create procedure `test_proc`()\n"
                + "begin\n"
                + "\t-- Enter your procedure code\n"
                + "end";
        Assert.assertEquals(expect, template.generateCreateObjectTemplate(procedure));
    }

    @Test
    public void generateCreateObjectTemplate_whithDeterministic() {
        DBObjectTemplate<DBProcedure> template = new MySQLProcedureTemplate();
        DBProcedure procedure = DBProcedure.of(null, "test_proc", Collections.emptyList());
        DBRoutineCharacteristic mySQLCharacteristic = new DBRoutineCharacteristic();
        mySQLCharacteristic.setDeterministic(true);
        procedure.setCharacteristic(mySQLCharacteristic);
        String expect = "create procedure `test_proc`()\n"
                + " DETERMINISTIC\n"
                + "begin\n"
                + "\t-- Enter your procedure code\n"
                + "end";
        Assert.assertEquals(expect, template.generateCreateObjectTemplate(procedure));
    }

    @Test
    public void generateCreateObjectTemplate_whithDataNature() {
        DBObjectTemplate<DBProcedure> template = new MySQLProcedureTemplate();
        DBProcedure procedure = DBProcedure.of(null, "test_proc", Collections.emptyList());
        DBRoutineCharacteristic mySQLCharacteristic = new DBRoutineCharacteristic();
        mySQLCharacteristic.setDataNature(DBRoutineDataNature.NO_SQL);
        procedure.setCharacteristic(mySQLCharacteristic);
        String expect = "create procedure `test_proc`()\n"
                + " NO SQL\n"
                + "begin\n"
                + "\t-- Enter your procedure code\n"
                + "end";
        Assert.assertEquals(expect, template.generateCreateObjectTemplate(procedure));
    }

    @Test
    public void generateCreateObjectTemplate_whithComment() {
        DBObjectTemplate<DBProcedure> template = new MySQLProcedureTemplate();
        DBProcedure procedure = DBProcedure.of(null, "test_proc", Collections.emptyList());
        DBRoutineCharacteristic mySQLCharacteristic = new DBRoutineCharacteristic();
        mySQLCharacteristic.setComment("test");
        procedure.setCharacteristic(mySQLCharacteristic);
        String expect = "create procedure `test_proc`()\n"
                + " COMMENT \'test\'\n"
                + "begin\n"
                + "\t-- Enter your procedure code\n"
                + "end";
        Assert.assertEquals(expect, template.generateCreateObjectTemplate(procedure));
    }

    @Test
    public void generateCreateObjectTemplate_whithSqlSecurity() {
        DBObjectTemplate<DBProcedure> template = new MySQLProcedureTemplate();
        DBProcedure procedure = DBProcedure.of(null, "test_proc", Collections.emptyList());
        DBRoutineCharacteristic mySQLCharacteristic = new DBRoutineCharacteristic();
        mySQLCharacteristic.setSqlSecurity(DBPLSqlSecurity.DEFINER);
        procedure.setCharacteristic(mySQLCharacteristic);
        String expect = "create procedure `test_proc`()\n"
                + " SQL SECURITY DEFINER\n"
                + "begin\n"
                + "\t-- Enter your procedure code\n"
                + "end";
        Assert.assertEquals(expect, template.generateCreateObjectTemplate(procedure));
    }

    @Test
    public void generateCreateObjectTemplate_whithAllCharacteristic() {
        DBObjectTemplate<DBProcedure> template = new MySQLProcedureTemplate();
        DBProcedure procedure = DBProcedure.of(null, "test_proc", Collections.emptyList());
        DBRoutineCharacteristic mySQLCharacteristic = new DBRoutineCharacteristic();
        procedure.setCharacteristic(mySQLCharacteristic);
        mySQLCharacteristic.setComment("test");
        mySQLCharacteristic.setDeterministic(false);
        mySQLCharacteristic.setDataNature(DBRoutineDataNature.CONTAINS_SQL);
        mySQLCharacteristic.setSqlSecurity(DBPLSqlSecurity.DEFINER);
        String expect = "create procedure `test_proc`()\n"
                + " COMMENT \'test\'\n"
                + " CONTAINS SQL\n"
                + " SQL SECURITY DEFINER\n"
                + "begin\n"
                + "\t-- Enter your procedure code\n"
                + "end";
        Assert.assertEquals(expect, template.generateCreateObjectTemplate(procedure));
    }

}
