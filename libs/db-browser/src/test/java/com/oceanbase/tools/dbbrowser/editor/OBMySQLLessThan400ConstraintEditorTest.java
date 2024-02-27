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
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLLessThan400ConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.util.DBObjectSingleTestCase;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;

/**
 * @author jingtian
 * @date 2024/1/15
 * @since ODC_release_4.2.4
 */
public class OBMySQLLessThan400ConstraintEditorTest {
    private DBTableConstraintEditor constraintEditor;
    private final String BASE_DIR = "src/test/resources/table/operator/ob";

    @Before
    public void setUp() {
        constraintEditor = new OBMySQLLessThan400ConstraintEditor();
    }

    @Test
    public void generateCreateObjectDDL() {
        String casesJson = DBObjectUtilsTest.loadAsString(BASE_DIR + "/add_primary_key_cases_less_than_400.json");
        List<DBObjectSingleTestCase<DBTableConstraint>> cases =
                MySQLConstraintEditorTest.fromJson(casesJson,
                        new TypeReference<List<DBObjectSingleTestCase<DBTableConstraint>>>() {});
        cases.forEach(testCase -> {
            String actual = constraintEditor.generateCreateObjectDDL(testCase.getInput().getCurrent());
            Assert.assertEquals(testCase.getOutput(), actual);
        });
    }

    @Test
    public void generateDropObjectDDL() {
        String casesJson = DBObjectUtilsTest.loadAsString(BASE_DIR + "/drop_primary_key_cases_less_than_400.json");
        List<DBObjectSingleTestCase<DBTableConstraint>> cases =
                MySQLConstraintEditorTest.fromJson(casesJson,
                        new TypeReference<List<DBObjectSingleTestCase<DBTableConstraint>>>() {});
        cases.forEach(testCase -> {
            String actual = constraintEditor.generateDropObjectDDL(testCase.getInput().getCurrent());
            Assert.assertEquals(testCase.getOutput(), actual);
        });
    }
}
