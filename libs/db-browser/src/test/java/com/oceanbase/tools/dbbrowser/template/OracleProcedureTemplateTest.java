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
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.template.oracle.OracleProcedureTemplate;

/**
 * {@link OracleProcedureTemplateTest}
 *
 * @author yh263208
 * @date 2022-02-22 17:47
 * @since db-browser-1.0.0-SNAPSHOT
 */
public class OracleProcedureTemplateTest {

    @Test
    public void generateCreateObjectTemplate_procedureWithParams_generateSucceed() {
        DBObjectTemplate<DBProcedure> template = new OracleProcedureTemplate();
        DBPLParam p1 = DBPLParam.of("p1", DBPLParamMode.IN, "varchar2");
        p1.setDefaultValue("abc'aaa");
        DBPLParam p2 = DBPLParam.of("p2", DBPLParamMode.INOUT, "varchar2");
        DBPLParam p3 = DBPLParam.of("p3", DBPLParamMode.IN, "INTEGER");
        p3.setDefaultValue("1");
        DBProcedure procedure = DBProcedure.of(null, "test_proc", Arrays.asList(p1, p2, p3));
        String expect = "CREATE OR REPLACE PROCEDURE \"test_proc\"(\n"
                + "\tp1 in varchar2 DEFAULT 'abc''aaa',\n"
                + "\tp2 in out varchar2,\n"
                + "\tp3 in INTEGER DEFAULT '1') IS\n"
                + "BEGIN\n"
                + "\t-- Enter your procedure code\n"
                + "END";
        Assert.assertEquals(expect, template.generateCreateObjectTemplate(procedure));
    }

    @Test
    public void generateCreateObjectTemplate_procedureWithoutParams_generateSucceed() {
        DBObjectTemplate<DBProcedure> template = new OracleProcedureTemplate();
        DBProcedure procedure = DBProcedure.of(null, "test_proc", Collections.emptyList());
        String expect = "CREATE OR REPLACE PROCEDURE \"test_proc\" IS\n"
                + "BEGIN\n"
                + "\t-- Enter your procedure code\n"
                + "END";
        Assert.assertEquals(expect, template.generateCreateObjectTemplate(procedure));
    }

}
