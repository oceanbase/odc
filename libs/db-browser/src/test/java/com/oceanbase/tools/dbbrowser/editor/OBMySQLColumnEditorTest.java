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
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLColumnEditor;
import com.oceanbase.tools.dbbrowser.editor.util.DBObjectListTestCase;
import com.oceanbase.tools.dbbrowser.editor.util.DBObjectSingleTestCase;
import com.oceanbase.tools.dbbrowser.editor.util.DBObjectTupleTestCase;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

public class OBMySQLColumnEditorTest {

    private DBTableColumnEditor columnEditor;
    private final String BASE_DIR = "src/test/resources/table/operator/ob/mysql/column";

    @Before
    public void setUp() {
        this.columnEditor = new MySQLColumnEditor();
    }

    @Test
    public void generateCreateObjectDDL() {
        String casesJson = DBObjectUtilsTest.loadAsString(BASE_DIR + "/create_column_object_test_cases.json");
        List<DBObjectSingleTestCase<DBTableColumn>> cases =
                MySQLConstraintEditorTest.fromJson(casesJson,
                        new TypeReference<List<DBObjectSingleTestCase<DBTableColumn>>>() {});
        cases.forEach(testCase -> {
            String actual = columnEditor.generateCreateObjectDDL(testCase.getInput().getCurrent());
            Assert.assertEquals(testCase.getOutput(), actual);
        });
    }

    @Test
    public void generateCreateDefinitionDDL() {
        String casesJson = DBObjectUtilsTest.loadAsString(BASE_DIR + "/create_column_definition_test_cases.json");
        List<DBObjectSingleTestCase<DBTableColumn>> cases =
                MySQLConstraintEditorTest.fromJson(casesJson,
                        new TypeReference<List<DBObjectSingleTestCase<DBTableColumn>>>() {});
        cases.forEach(testCase -> {
            String actual = columnEditor.generateCreateDefinitionDDL(testCase.getInput().getCurrent());
            Assert.assertEquals(testCase.getOutput(), actual);
        });
    }

    @Test
    public void generateDropObjectDDL() {
        String casesJson = DBObjectUtilsTest.loadAsString(BASE_DIR + "/drop_column_test_cases.json");
        List<DBObjectSingleTestCase<DBTableColumn>> cases =
                MySQLConstraintEditorTest.fromJson(casesJson,
                        new TypeReference<List<DBObjectSingleTestCase<DBTableColumn>>>() {});
        cases.forEach(testCase -> {
            String actual = columnEditor.generateDropObjectDDL(testCase.getInput().getCurrent());
            Assert.assertEquals(testCase.getOutput(), actual);
        });
    }

    @Test
    public void generateUpdateSingleColumnDDL() {
        String casesJson = DBObjectUtilsTest.loadAsString(BASE_DIR + "/update_single_column_test_cases.json");
        List<DBObjectTupleTestCase<DBTableColumn>> cases =
                MySQLConstraintEditorTest.fromJson(casesJson,
                        new TypeReference<List<DBObjectTupleTestCase<DBTableColumn>>>() {});
        cases.forEach(testCase -> {
            String actual = columnEditor.generateUpdateObjectDDL(testCase.getInput().getPrevious(),
                    testCase.getInput().getCurrent());
            Assert.assertEquals(testCase.getOutput(), actual);
        });
    }

    @Test
    public void generateUpdateColumnListDDL() {
        String casesJson = DBObjectUtilsTest.loadAsString(BASE_DIR + "/update_columns_list_test_cases.json");
        List<DBObjectListTestCase<DBTableColumn>> cases =
                MySQLConstraintEditorTest.fromJson(casesJson,
                        new TypeReference<List<DBObjectListTestCase<DBTableColumn>>>() {});
        cases.forEach(testCase -> {
            String actual =
                    columnEditor.generateUpdateObjectListDDL(testCase.getInput().getPrevious(),
                            testCase.getInput().getCurrent());
            Assert.assertEquals(testCase.getOutput(), actual);
        });
    }

}
