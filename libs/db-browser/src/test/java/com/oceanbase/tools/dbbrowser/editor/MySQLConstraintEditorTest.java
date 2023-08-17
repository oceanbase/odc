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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.util.DBObjectListTestCase;
import com.oceanbase.tools.dbbrowser.editor.util.DBObjectSingleTestCase;
import com.oceanbase.tools.dbbrowser.editor.util.DBObjectTupleTestCase;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;

public class MySQLConstraintEditorTest {

    private DBTableConstraintEditor constraintEditor;
    private final String BASE_DIR = "src/test/resources/table/operator/mysql/constraint";
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .addModule(new Jdk8Module())
            .addModule(new JavaTimeModule())
            .build();

    @Before
    public void setUp() {
        constraintEditor = new MySQLConstraintEditor();
    }

    @Test
    public void generateCreateObjectDDL() {
        String casesJson = DBObjectUtilsTest.loadAsString(BASE_DIR + "/create_constraint_object_test_cases.json");
        List<DBObjectSingleTestCase<DBTableConstraint>> cases =
                fromJson(casesJson, new TypeReference<List<DBObjectSingleTestCase<DBTableConstraint>>>() {});
        cases.forEach(testCase -> {
            String actual = constraintEditor.generateCreateObjectDDL(testCase.getInput().getCurrent());
            Assert.assertEquals(testCase.getOutput(), actual);
        });
    }

    @Test
    public void generateCreateDefinitionDDL() {
        String casesJson = DBObjectUtilsTest.loadAsString(BASE_DIR + "/create_constraint_definition_test_cases.json");
        List<DBObjectSingleTestCase<DBTableConstraint>> cases =
                fromJson(casesJson, new TypeReference<List<DBObjectSingleTestCase<DBTableConstraint>>>() {});
        cases.forEach(testCase -> {
            String actual = constraintEditor.generateCreateDefinitionDDL(testCase.getInput().getCurrent());
            Assert.assertEquals(testCase.getOutput(), actual);
        });
    }

    @Test
    public void generateDropObjectDDL() {
        String casesJson = DBObjectUtilsTest.loadAsString(BASE_DIR + "/drop_constraint_test_cases.json");
        List<DBObjectSingleTestCase<DBTableConstraint>> cases =
                fromJson(casesJson, new TypeReference<List<DBObjectSingleTestCase<DBTableConstraint>>>() {});
        cases.forEach(testCase -> {
            String actual = constraintEditor.generateDropObjectDDL(testCase.getInput().getCurrent());
            Assert.assertEquals(testCase.getOutput(), actual);
        });
    }

    @Test
    public void generateUpdateSingleConstraintDDL() {
        String casesJson = DBObjectUtilsTest.loadAsString(BASE_DIR + "/update_single_constraint_test_cases.json");
        List<DBObjectTupleTestCase<DBTableConstraint>> cases =
                fromJson(casesJson, new TypeReference<List<DBObjectTupleTestCase<DBTableConstraint>>>() {});
        cases.forEach(testCase -> {
            String actual = constraintEditor.generateUpdateObjectDDL(testCase.getInput().getPrevious(),
                    testCase.getInput().getCurrent());
            Assert.assertEquals(testCase.getOutput(), actual);
        });
    }

    @Test
    public void generateUpdateColumnListDDL() {
        String casesJson = DBObjectUtilsTest.loadAsString(BASE_DIR + "/update_constraint_list_test_cases.json");
        List<DBObjectListTestCase<DBTableConstraint>> cases =
                fromJson(casesJson, new TypeReference<List<DBObjectListTestCase<DBTableConstraint>>>() {});
        cases.forEach(testCase -> {
            String actual =
                    constraintEditor.generateUpdateObjectListDDL(testCase.getInput().getPrevious(),
                            testCase.getInput().getCurrent());
            Assert.assertEquals(testCase.getOutput(), actual);
        });
    }

    public static <T> T fromJson(String json, TypeReference<T> valueTypeRef) {
        if (json == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, valueTypeRef);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

}
