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
package com.oceanbase.odc.service.db.parser;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.service.db.model.AnonymousBlockFunctionCall;
import com.oceanbase.odc.service.db.model.AnonymousBlockProcedureCall;
import com.oceanbase.odc.service.db.parser.result.ParserCallPLByAnonymousBlockResult;

public class AnonymousBlockParserTest {

    public static final String TEST_DDL_1 = "DECLARE\n"
            + "  p1 my_obj_1 := my_obj_1(1,'a',1.2);\n"
            + "  p2 int := 123;\n"
            + "  p3 varchar := 'a';\n"
            + "  result my_obj_1;\n"
            + "BEGIN\n"
            + "  result := \"PACKAGE\".\"FUN\"(p1 => p1,p2 => p2,p3 => p3);\n"
            + "  p3 := p3;\n"
            + "END;";

    public static final String TEST_DDL_2 = "DECLARE\n"
            + "  p1 my_obj_1 := my_obj_1(1,'a',1.2);\n"
            + "  p2 int := 123;\n"
            + "  p3 varchar := 'a';\n"
            + "BEGIN\n"
            + "  \"BYM\".\n"
            + "  \"PACKAGE\".\n"
            + "  \"PROC\"(p1 => p1,p2 => p2,p3 => p3);\n"
            + "  p3 := p3;\n"
            + "END;";

    public static final String TEST_DDL_3 = "DECLARE\n"
            + "  N1 varchar2 := null;\n"
            + "  P1 int := 5;\n"
            + "  N2 int := null;\n"
            + "  N3 varchar2 := '';\n"
            + "  P2 int := 5;\n"
            + "  P3 int;\n"
            + "BEGIN\n"
            + "  \"PL_TEST_2\"(N1 => N1,P1 => P1,N2 => N2,N3 => N3,P2 => P2,P3 => P3);\n"
            + "  p2 := p2;\n"
            + "  p3 := p3;\n"
            + "END;";

    public static final String TEST_DDL_4 = "DECLARE\n"
            + "  p_in int := null;\n"
            + "BEGIN\n"
            + "  \"IN_PARAM\"(p_in => p_in);\n"
            + "END;";

    public static final String TEST_DDL_5 = "DECLARE\n"
            + "  p1 integer := null;\n"
            + "  result integer;\n"
            + "BEGIN\n"
            + "  result := \"INOUT_PARAM\"(p1 => p1);\n"
            + "END;";

    public static final String TEST_DDL_6 = "DECLARE\n"
            + "  in_deptNo INT := null;\n"
            + "  out_curEmp SYS_REFCURSOR;\n"
            + "BEGIN\n"
            + "  \"TEST_CASE_CURSOR\"(in_deptNo => in_deptNo, out_curEmp => out_curEmp);\n"
            + "  out_curEmp := out_curEmp;\n"
            + "END;";

    @Test
    public void parserAnonymousBlock_callFunctionSucceed() {
        ParserCallPLByAnonymousBlockResult result =
                AnonymousBlockParser.parserCallPLAnonymousBlockResult(TEST_DDL_1, 0);
        Map<String, AnonymousBlockFunctionCall> functionCallMap = result.getFunctionCallMap();
        AnonymousBlockFunctionCall functionCall = functionCallMap.get("FUN");
        int actual = functionCall.getCallLine();
        int expect = 7;
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void parserAnonymousBlock_callFunction_2_Succeed() {
        ParserCallPLByAnonymousBlockResult result =
                AnonymousBlockParser.parserCallPLAnonymousBlockResult(TEST_DDL_5, 0);
        Map<String, AnonymousBlockFunctionCall> functionCallMap = result.getFunctionCallMap();
        AnonymousBlockFunctionCall functionCall = functionCallMap.get("INOUT_PARAM");
        int actual = functionCall.getCallLine();
        int expect = 5;
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void parserAnonymousBlock_callProcedureSucceed() {
        ParserCallPLByAnonymousBlockResult result =
                AnonymousBlockParser.parserCallPLAnonymousBlockResult(TEST_DDL_2, 0);
        Map<String, AnonymousBlockProcedureCall> procedureCallMap = result.getProcedureCallMap();
        AnonymousBlockProcedureCall procedureCall = procedureCallMap.get("PROC");
        int actual = procedureCall.getCallLine();
        int expect = 6;
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void parserAnonymousBlock_callProcedure_2_Succeed() {
        ParserCallPLByAnonymousBlockResult result =
                AnonymousBlockParser.parserCallPLAnonymousBlockResult(TEST_DDL_3, 0);
        Map<String, AnonymousBlockProcedureCall> procedureCallMap = result.getProcedureCallMap();
        AnonymousBlockProcedureCall procedureCall = procedureCallMap.get("PL_TEST_2");
        int actual = procedureCall.getCallLine();
        int expect = 9;
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void parserAnonymousBlock_callProcedure_3_Succeed() {
        ParserCallPLByAnonymousBlockResult result =
                AnonymousBlockParser.parserCallPLAnonymousBlockResult(TEST_DDL_4, 0);
        Map<String, AnonymousBlockProcedureCall> procedureCallMap = result.getProcedureCallMap();
        AnonymousBlockProcedureCall procedureCall = procedureCallMap.get("IN_PARAM");
        int actual = procedureCall.getCallLine();
        int expect = 4;
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void parserAnonymousBlock_callProcedure_4_Succeed() {
        ParserCallPLByAnonymousBlockResult result =
                AnonymousBlockParser.parserCallPLAnonymousBlockResult(TEST_DDL_6, 0);
        Map<String, AnonymousBlockProcedureCall> procedureCallMap = result.getProcedureCallMap();
        AnonymousBlockProcedureCall procedureCall = procedureCallMap.get("TEST_CASE_CURSOR");
        int actual = procedureCall.getCallLine();
        int expect = 5;
        Assert.assertEquals(expect, actual);
    }
}
