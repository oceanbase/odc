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
package com.oceanbase.odc.service.datasecurity.recognizer;

import java.util.Optional;

import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.service.datasecurity.model.RecognitionResult;
import com.oceanbase.odc.service.datasecurity.model.SensitiveLevel;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRule;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRuleType;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;


public class GroovyColumnRecognizerTest {


    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void test_recognize_true() {
        SensitiveRule rule = createGroovyRule(1L, buildDefaultGroovyScript());
        ColumnRecognizer recognizer = new GroovyColumnRecognizer(rule);
        DBTableColumn dbTableColumn = createTestColumn();
        Optional<RecognitionResult> resultOpt = recognizer.recognize(dbTableColumn);
        Assert.assertTrue("Script matching is successful. An Optional with a value should be returned.",
                resultOpt.isPresent());
        RecognitionResult result = resultOpt.get();
        Assert.assertEquals("The matching rule ID should be 1.", rule.getId(), result.getMatchedRuleId());
        Assert.assertEquals("The rule type should be GROOVY", SensitiveRuleType.GROOVY, result.getSourceRuleType());
    }

    @Test
    public void test_recognize_false() {
        SensitiveRule rule = createGroovyRule(1L, buildDefaultGroovyScript());
        ColumnRecognizer recognizer = new GroovyColumnRecognizer(rule);
        DBTableColumn dbTableColumn = createTestColumn();
        dbTableColumn.setTableName("unmatched_table");
        Optional<RecognitionResult> resultOpt = recognizer.recognize(dbTableColumn);
        Assert.assertFalse("Script matching failed. It should return an empty Optional.", resultOpt.isPresent());
    }

    @Test
    public void test_recognize_nullColumnName() {
        SensitiveRule rule = createGroovyRule(1L, buildDefaultGroovyScript());
        ColumnRecognizer recognizer = new GroovyColumnRecognizer(rule);
        DBTableColumn dbTableColumn = createTestColumn();
        dbTableColumn.setName(null);
        Optional<RecognitionResult> resultOpt = recognizer.recognize(dbTableColumn);
        Assert.assertFalse("The script execution has failed. It should return an empty Optional.",
                resultOpt.isPresent());
    }

    @Test
    public void test_securityInterceptor_systemExit() {
        thrown.expect(Exception.class);
        thrown.expectMessage("Method call is not security");
        String script = "System.exit(-1);";
        new GroovyColumnRecognizer(createGroovyRule(1L, script));
    }

    @Test
    public void test_securityInterceptor_forLoop() {
        thrown.expect(MultipleCompilationErrorsException.class);
        thrown.expectMessage("ForStatements are not allowed");
        String script = "for (int i = 0; i < 1; i++) {\n"
                + "    i = 0;\n"
                + "}";
        new GroovyColumnRecognizer(createGroovyRule(1L, script));
    }

    @Test
    public void test_securityInterceptor_whileLoop() {
        thrown.expect(MultipleCompilationErrorsException.class);
        thrown.expectMessage("WhileStatements are not allowed");
        String script = "while(true) {\n"
                + "    int i = 0;\n"
                + "}";
        new GroovyColumnRecognizer(createGroovyRule(1L, script));
    }

    @Test
    public void test_securityInterceptor_threadSleep() {
        thrown.expect(MultipleCompilationErrorsException.class);
        thrown.expectMessage("java.lang.Thread");
        String script = "Thread.sleep(1000);";
        new GroovyColumnRecognizer(createGroovyRule(1L, script));
    }

    @Test
    public void test_securityInterceptor_importPackage() {
        thrown.expect(MultipleCompilationErrorsException.class);
        thrown.expectMessage("java.lang.System");
        String script = "import java.lang.System;";
        new GroovyColumnRecognizer(createGroovyRule(1L, script));
    }

    // --- 辅助方法 ---

    private String buildDefaultGroovyScript() {
        return "if (column.name.equals(\"column\")) {\n"
                + "    if (column.table.equalsIgnoreCase(\"iam_user\")) {\n"
                + "        if (column.schema.length() > 0) {\n"
                + "            if (column.comment.indexOf(\"user\") > 0) {\n"
                + "                if (column.type.toLowerCase().equals(\"varchar\")) {\n"
                + "                    return true;\n"
                + "                }\n"
                + "            }\n"
                + "        }\n"
                + "    }\n"
                + "}\n"
                + "return false;";
    }

    private DBTableColumn createTestColumn() {
        DBTableColumn dbTableColumn = new DBTableColumn();
        dbTableColumn.setSchemaName("odc_meta");
        dbTableColumn.setTableName("iam_user");
        dbTableColumn.setName("column");
        dbTableColumn.setTypeName("varchar");
        dbTableColumn.setComment("record user info");
        return dbTableColumn;
    }

    private SensitiveRule createGroovyRule(Long id, String script) {
        SensitiveRule rule = new SensitiveRule();
        rule.setId(id);
        rule.setType(SensitiveRuleType.GROOVY);
        rule.setGroovyScript(script);
        rule.setLevel(SensitiveLevel.HIGH);
        rule.setEnabled(true);
        return rule;
    }
}
