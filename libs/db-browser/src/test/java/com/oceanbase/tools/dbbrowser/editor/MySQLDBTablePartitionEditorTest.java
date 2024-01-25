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
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLDBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.util.DBObjectListTestCase;
import com.oceanbase.tools.dbbrowser.editor.util.DBObjectSingleTestCase;
import com.oceanbase.tools.dbbrowser.editor.util.DBObjectTupleTestCase;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;

public class MySQLDBTablePartitionEditorTest {

    private DBTablePartitionEditor partitionEditor;
    private final String BASE_DIR = "src/test/resources/table/operator/ob/mysql/partition";

    @Before
    public void setUp() {
        partitionEditor = new MySQLDBTablePartitionEditor();
    }

    @Test
    public void generateCreateObjectDDL() {
        String casesJson = DBObjectUtilsTest.loadAsString(BASE_DIR + "/create_partition_object_test_cases.json");
        List<DBObjectSingleTestCase<DBTablePartition>> cases =
                MySQLConstraintEditorTest.fromJson(casesJson,
                        new TypeReference<List<DBObjectSingleTestCase<DBTablePartition>>>() {});
        cases.forEach(testCase -> {
            String actual = partitionEditor.generateCreateObjectDDL(testCase.getInput().getCurrent());
            Assert.assertEquals(testCase.getOutput(), actual);
        });
    }

    @Test
    public void generateCreateDefinitionDDL() {
        String casesJson = DBObjectUtilsTest.loadAsString(BASE_DIR + "/create_partition_definition_test_cases.json");
        List<DBObjectSingleTestCase<DBTablePartition>> cases =
                MySQLConstraintEditorTest.fromJson(casesJson,
                        new TypeReference<List<DBObjectSingleTestCase<DBTablePartition>>>() {});
        cases.forEach(testCase -> {
            String actual = partitionEditor.generateCreateDefinitionDDL(testCase.getInput().getCurrent());
            Assert.assertEquals(testCase.getOutput(), actual);
        });
    }

    @Test
    public void generateDropObjectDDL() {
        String casesJson = DBObjectUtilsTest.loadAsString(BASE_DIR + "/drop_partition_test_cases.json");
        List<DBObjectSingleTestCase<DBTablePartition>> cases =
                MySQLConstraintEditorTest.fromJson(casesJson,
                        new TypeReference<List<DBObjectSingleTestCase<DBTablePartition>>>() {});
        cases.forEach(testCase -> {
            String actual = partitionEditor.generateDropObjectDDL(testCase.getInput().getCurrent());
            Assert.assertEquals(testCase.getOutput(), actual);
        });
    }

    @Test
    public void generateUpdateSinglePartitionDDL() {
        String casesJson = DBObjectUtilsTest.loadAsString(BASE_DIR + "/update_single_partition_test_cases.json");
        List<DBObjectTupleTestCase<DBTablePartition>> cases =
                MySQLConstraintEditorTest.fromJson(casesJson,
                        new TypeReference<List<DBObjectTupleTestCase<DBTablePartition>>>() {});
        cases.forEach(testCase -> {
            String actual = partitionEditor.generateUpdateObjectDDL(testCase.getInput().getPrevious(),
                    testCase.getInput().getCurrent());
            Assert.assertEquals(testCase.getOutput(), actual);
        });
    }

    @Test
    public void generateUpdateShadowTableSinglePartitionDDL() {
        String casesJson = DBObjectUtilsTest.loadAsString(BASE_DIR + "/update_shadow_table_partition_test_cases.json");
        List<DBObjectTupleTestCase<DBTablePartition>> cases =
                MySQLConstraintEditorTest.fromJson(casesJson,
                        new TypeReference<List<DBObjectTupleTestCase<DBTablePartition>>>() {});
        cases.forEach(testCase -> {
            String actual = partitionEditor.generateShadowTableUpdateObjectDDL(testCase.getInput().getPrevious(),
                    testCase.getInput().getCurrent());
            Assert.assertEquals(testCase.getOutput(), actual);
        });
    }

    @Test
    public void generateUpdatePartitionListDDL() {
        String casesJson = DBObjectUtilsTest.loadAsString(BASE_DIR + "/update_partition_list_test_cases.json");
        List<DBObjectListTestCase<DBTablePartition>> cases =
                MySQLConstraintEditorTest.fromJson(casesJson,
                        new TypeReference<List<DBObjectListTestCase<DBTablePartition>>>() {});
        cases.forEach(testCase -> {
            String actual =
                    partitionEditor.generateUpdateObjectListDDL(testCase.getInput().getPrevious(),
                            testCase.getInput().getCurrent());
            Assert.assertEquals(testCase.getOutput(), actual);
        });
    }

    @Test
    public void generateAddPartitionDefinitionDDL_normalInput_generateSucceed() {
        String casesJson = DBObjectUtilsTest.loadAsString(BASE_DIR + "/update_multi_partition_test_cases.json");
        List<DBObjectTupleTestCase<DBTablePartition>> cases = MySQLConstraintEditorTest.fromJson(casesJson,
                new TypeReference<List<DBObjectTupleTestCase<DBTablePartition>>>() {});
        cases.forEach(testCase -> {
            String actual = partitionEditor.generateAddPartitionDefinitionDDL(testCase.getInput().getCurrent());
            Assert.assertEquals(testCase.getOutput(), actual);
        });
    }

}
