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

import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

/**
 * @author gaoda.xy
 * @date 2023/5/23 19:35
 */
public class GroovyColumnRecognizerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void test_recognize_true() {
        ColumnRecognizer recognizer = new GroovyColumnRecognizer(buildGroovyScript());
        DBTableColumn dbTableColumn = createDBTableColumn();
        Assert.assertTrue(recognizer.recognize(dbTableColumn));
    }

    @Test
    public void test_recognize_false() {
        ColumnRecognizer recognizer = new GroovyColumnRecognizer(buildGroovyScript());
        DBTableColumn dbTableColumn = createDBTableColumn();
        dbTableColumn.setTableName("unmatched");
        Assert.assertFalse(recognizer.recognize(dbTableColumn));
    }

    @Test
    public void test_recognize_nullColumnName() {
        ColumnRecognizer recognizer = new GroovyColumnRecognizer(buildGroovyScript());
        DBTableColumn dbTableColumn = createDBTableColumn();
        dbTableColumn.setTableName(null);
        Assert.assertFalse(recognizer.recognize(dbTableColumn));
    }

    @Test
    public void test_securityInterceptor_systemExit() {
        thrown.expect(Exception.class);
        thrown.expectMessage("Method call is not security");
        String script = "System.exit(-1);";
        new GroovyColumnRecognizer(script);
    }

    @Test
    public void test_securityInterceptor_forLoop() {
        thrown.expect(MultipleCompilationErrorsException.class);
        thrown.expectMessage("ForStatements are not allowed");
        String script = "for (int i = 0; i < 1; i++) {\n"
                + "    i = 0;\n"
                + "}";
        new GroovyColumnRecognizer(script);
    }

    @Test
    public void test_securityInterceptor_whileLoop() {
        thrown.expect(MultipleCompilationErrorsException.class);
        thrown.expectMessage("WhileStatements are not allowed");
        String script = "while(true) {\n"
                + "    int i = 0;\n"
                + "}";
        new GroovyColumnRecognizer(script);
    }

    @Test
    public void test_securityInterceptor_threadSleep() throws InterruptedException {
        thrown.expect(MultipleCompilationErrorsException.class);
        thrown.expectMessage("java.lang.Thread");
        String script = "Thread.sleep(1000);";
        new GroovyColumnRecognizer(script);
    }

    @Test
    public void test_securityInterceptor_importPackage() throws InterruptedException {
        thrown.expect(MultipleCompilationErrorsException.class);
        thrown.expectMessage("java.lang.System");
        String script = "import java.lang.System;";
        new GroovyColumnRecognizer(script);
    }

    private String buildGroovyScript() {
        return "if (column.name.equals(\"column\")) {\n"
                + "    if (column.table.equalsIgnoreCase(\"IAM_USER\")) {\n"
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

    private DBTableColumn createDBTableColumn() {
        DBTableColumn dbTableColumn = new DBTableColumn();
        dbTableColumn.setSchemaName("odc_meta");
        dbTableColumn.setTableName("iam_user");
        dbTableColumn.setName("column");
        dbTableColumn.setTypeName("varchar");
        dbTableColumn.setComment("record user info");
        return dbTableColumn;
    }

}
