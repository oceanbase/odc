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
package com.oceanbase.odc.service.datasecurity.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.core.shared.exception.BadArgumentException;

/**
 * @author gaoda.xy
 * @date 2023/5/24 14:00
 */
public class ParameterValidateUtilTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void test_validatePathExpression_valid() {
        List<String> pathIncludes = Arrays.asList("*.b.c", "a._.c");
        List<String> pathExcludes = new ArrayList<>();
        ParameterValidateUtil.validatePathExpression(pathIncludes, pathExcludes);
    }

    @Test
    public void test_validatePathExpression_invalidIncludes() {
        thrown.expect(BadArgumentException.class);
        thrown.expectMessage("pathInclude");
        List<String> pathIncludes = Arrays.asList("*", "a._.c");
        List<String> pathExcludes = Arrays.asList("a.b.$");
        ParameterValidateUtil.validatePathExpression(pathIncludes, pathExcludes);
    }

    @Test
    public void test_validatePathExpression_invalidExcludes() {
        thrown.expect(BadArgumentException.class);
        thrown.expectMessage("pathExclude");
        List<String> pathIncludes = Arrays.asList("a._.c");
        List<String> pathExcludes = Arrays.asList("b.$");
        ParameterValidateUtil.validatePathExpression(pathIncludes, pathExcludes);
    }

    @Test
    public void test_validateRegexExpression_valid() {
        String regexExpression = "^user\\S*$";
        ParameterValidateUtil.validateRegexExpression(regexExpression, null, null, null);
    }

    @Test
    public void validateRegexExpression_invalid_databaseRegexExpression() {
        thrown.expect(BadArgumentException.class);
        thrown.expectMessage("databaseRegexExpression");
        String regexExpression = "[";
        ParameterValidateUtil.validateRegexExpression(regexExpression, null, null, null);
    }

    @Test
    public void validateRegexExpression_invalid_tableRegexExpression() {
        thrown.expect(BadArgumentException.class);
        thrown.expectMessage("tableRegexExpression");
        String regexExpression = "[";
        ParameterValidateUtil.validateRegexExpression(null, regexExpression, null, null);
    }

    @Test
    public void validateRegexExpression_invalid_columnRegexExpression() {
        thrown.expect(BadArgumentException.class);
        thrown.expectMessage("columnRegexExpression");
        String regexExpression = "[";
        ParameterValidateUtil.validateRegexExpression(null, null, regexExpression, null);
    }

    @Test
    public void validateRegexExpression_invalid_columnCommentRegexExpression() {
        thrown.expect(BadArgumentException.class);
        thrown.expectMessage("columnCommentRegexExpression");
        String regexExpression = "[";
        ParameterValidateUtil.validateRegexExpression(null, null, null, regexExpression);
    }

    @Test
    public void test_validateGroovyScript_valid() {
        String groovyScript = "if (column.schema == \"schema\") {\n"
                + "    if (column.table.length() == 5) {\n"
                + "        if (column.name.equals(\"column\")) {\n"
                + "            if (column.comment == null) {\n"
                + "                if (column.type == \"varchar\") {\n"
                + "                    return true;\n"
                + "                }\n"
                + "            }\n"
                + "        }\n"
                + "    }\n"
                + "}\n"
                + "return false;";
        ParameterValidateUtil.validateGroovyScript(groovyScript);
    }

    @Test
    public void test_validateGroovyScript_validMethodCall() {
        String groovyScript = "Math.max(1, 2)";
        ParameterValidateUtil.validateGroovyScript(groovyScript);
    }

    @Test
    public void test_validateGroovyScript_invalid() {
        thrown.expect(Exception.class);
        thrown.expectMessage("No such property");
        String groovyScript = "invalid groovy script";
        ParameterValidateUtil.validateGroovyScript(groovyScript);
    }

    @Test
    public void test_validateGroovyScript_invokeClassInBlackList_System() {
        thrown.expect(Exception.class);
        thrown.expectMessage("Method call is not security");
        String groovyScript = "System.exit(-1);";
        ParameterValidateUtil.validateGroovyScript(groovyScript);
    }

    @Test
    public void test_validateGroovyScript_invokeClassInBlackList_Thread() {
        thrown.expect(Exception.class);
        thrown.expectMessage("Method call is not security");
        String groovyScript = "Thread.sleep(1000);";
        ParameterValidateUtil.validateGroovyScript(groovyScript);
    }

    @Test
    public void test_validateGroovyScript_executeSystemCommand_action1() {
        thrown.expect(Exception.class);
        thrown.expectMessage("Method call is not security");
        String groovyScript = "def cmd = \"rm -rf /groovy_test.txt\";\ncmd.execute();";
        ParameterValidateUtil.validateGroovyScript(groovyScript);
    }

    @Test
    public void test_validateGroovyScript_executeSystemCommand_action2() {
        thrown.expect(Exception.class);
        thrown.expectMessage("Method call is not security");
        String groovyScript = "def proc = Runtime.getRuntime().exec([\"ls\", \"-al\"])\n"
                + "def result = proc.inputStream.text\n"
                + "println result";
        ParameterValidateUtil.validateGroovyScript(groovyScript);
    }

    @Test
    public void test_validateGroovyScript_executeSystemCommand_action3() {
        thrown.expect(Exception.class);
        thrown.expectMessage("Indirect import checks");
        String groovyScript = "def processBuilder = new ProcessBuilder([\"ls\", \"/\"])\n"
                + "def proc = processBuilder.start()\n"
                + "def result = proc.inputStream.text";
        ParameterValidateUtil.validateGroovyScript(groovyScript);
    }

}
