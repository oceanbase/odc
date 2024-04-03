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
package com.oceanbase.tools.dbbrowser.editor;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.tools.dbbrowser.editor.oracle.OBOracleIndexEditor;
import com.oceanbase.tools.dbbrowser.editor.util.DBObjectSingleTestCase;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;

public class OracleIndexEditorTest {

    private DBTableIndexEditor indexEditor;
    private final String BASE_DIR = "src/test/resources/table/operator/ob/oracle/index";

    @Before
    public void setUp() {
        indexEditor = new OBOracleIndexEditor();
    }

    @Test
    public void generateCreateObjectDDL() {
        String casesJson = DBObjectUtilsTest.loadAsString(BASE_DIR + "/create_index_object_test_cases.json");
        List<DBObjectSingleTestCase<DBTableIndex>> cases =
                MySQLConstraintEditorTest.fromJson(casesJson,
                        new TypeReference<List<DBObjectSingleTestCase<DBTableIndex>>>() {});
        cases.forEach(testCase -> {
            String actual = indexEditor.generateCreateObjectDDL(testCase.getInput().getCurrent());
            Assert.assertEquals(testCase.getOutput(), actual);
        });
    }

    @Test
    public void generateCreateDefinitionDDL() {
        String casesJson = DBObjectUtilsTest.loadAsString(BASE_DIR + "/create_index_definition_test_cases.json");
        List<DBObjectSingleTestCase<DBTableIndex>> cases =
                MySQLConstraintEditorTest.fromJson(casesJson,
                        new TypeReference<List<DBObjectSingleTestCase<DBTableIndex>>>() {});
        cases.forEach(testCase -> {
            String actual = indexEditor.generateCreateDefinitionDDL(testCase.getInput().getCurrent());
            Assert.assertEquals(testCase.getOutput(), actual);
        });
    }

    @Test
    public void generateDropObjectDDL() {
        String casesJson = DBObjectUtilsTest.loadAsString(BASE_DIR + "/drop_index_test_cases.json");
        List<DBObjectSingleTestCase<DBTableIndex>> cases =
                MySQLConstraintEditorTest.fromJson(casesJson,
                        new TypeReference<List<DBObjectSingleTestCase<DBTableIndex>>>() {});
        cases.forEach(testCase -> {
            String actual = indexEditor.generateDropObjectDDL(testCase.getInput().getCurrent());
            Assert.assertEquals(testCase.getOutput(), actual);
        });
    }

}
