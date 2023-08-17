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
import com.oceanbase.tools.dbbrowser.model.DBPLParamMode;
import com.oceanbase.tools.dbbrowser.template.oracle.OracleFunctionTemplate;

/**
 * {@link OracleFunctionTemplateTest}
 *
 * @author yh263208
 * @date 2023-02-22 16:19
 * @since db-browser_1.0.0-SNAPSHOT
 */
public class OracleFunctionTemplateTest {

    @Test
    public void generateCreateObjectTemplate_functionWithParams_generateSucceed() {
        DBObjectTemplate<DBFunction> template = new OracleFunctionTemplate();
        DBFunction function = DBFunction.of("f_test", "INTEGER");
        DBPLParam p1 = DBPLParam.of("p1", DBPLParamMode.OUT, "varchar2");
        DBPLParam p2 = DBPLParam.of("p2", DBPLParamMode.IN, "varchar2");
        p2.setDefaultValue("abcd'rrff");
        DBPLParam p3 = DBPLParam.of("p3", DBPLParamMode.IN, "INTEGER");
        p3.setDefaultValue("1");
        function.setParams(Arrays.asList(p1, p2, p3));
        String expect = "CREATE OR REPLACE FUNCTION \"f_test\"(\n"
                + "\tp1 out varchar2,\n"
                + "\tp2 in varchar2 DEFAULT 'abcd''rrff',\n"
                + "\tp3 in INTEGER DEFAULT '1')\n"
                + "RETURN INTEGER AS\n"
                + "\tV1 INT;\n"
                + "BEGIN\n"
                + "\t-- Enter your function code\n"
                + "END";
        Assert.assertEquals(expect, template.generateCreateObjectTemplate(function));
    }

    @Test
    public void generateCreateObjectTemplate_functionWithoutParams_generateSucceed() {
        DBObjectTemplate<DBFunction> template = new OracleFunctionTemplate();
        DBFunction function = DBFunction.of("f_test", "INTEGER");
        String expect = "CREATE OR REPLACE FUNCTION \"f_test\"\n"
                + "RETURN INTEGER AS\n"
                + "\tV1 INT;\n"
                + "BEGIN\n"
                + "\t-- Enter your function code\n"
                + "END";
        Assert.assertEquals(expect, template.generateCreateObjectTemplate(function));
    }

}
