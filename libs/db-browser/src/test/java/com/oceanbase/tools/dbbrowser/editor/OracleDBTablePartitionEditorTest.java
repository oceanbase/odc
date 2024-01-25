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
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleDBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.util.DBObjectSingleTestCase;
import com.oceanbase.tools.dbbrowser.editor.util.DBObjectTupleTestCase;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;

public class OracleDBTablePartitionEditorTest {

    private DBTablePartitionEditor partitionEditor;
    private final String BASE_DIR = "src/test/resources/table/operator/ob/oracle/partition";

    @Before
    public void setUp() {
        partitionEditor = new OracleDBTablePartitionEditor();
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
    public void generateAddPartitionDefinitionDDL_onePartiElt_generateSucceed() {
        String casesJson = DBObjectUtilsTest.loadAsString(BASE_DIR + "/update_multi_partition_test_cases.json");
        List<DBObjectTupleTestCase<DBTablePartition>> cases = MySQLConstraintEditorTest.fromJson(casesJson,
                new TypeReference<List<DBObjectTupleTestCase<DBTablePartition>>>() {});
        cases.forEach(testCase -> {
            DBTablePartition partition = testCase.getInput().getCurrent();
            String actual = partitionEditor.generateAddPartitionDefinitionDDL(partition.getSchemaName(),
                    partition.getTableName(), partition.getPartitionOption(), partition.getPartitionDefinitions());
            Assert.assertEquals(testCase.getOutput(), actual);
        });
    }

}
