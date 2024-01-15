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
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLLessThan400DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.util.DBObjectTupleTestCase;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;

/**
 * @author jingtian
 * @date 2024/1/12
 * @since
 */
public class OBMySQLLessThan400DBTablePartitionEditorTest {
    private DBTablePartitionEditor partitionEditor;
    private final String BASE_DIR = "src/test/resources/table/operator/ob";

    @Before
    public void setUp() {
        partitionEditor = new OBMySQLLessThan400DBTablePartitionEditor();
    }

    @Test
    public void generateUpdateSinglePartitionDDL() {
        String casesJson =
                DBObjectUtilsTest.loadAsString(BASE_DIR + "/update_single_partition_test_cases_less_than_400.json");
        List<DBObjectTupleTestCase<DBTablePartition>> cases =
                MySQLConstraintEditorTest.fromJson(casesJson,
                        new TypeReference<List<DBObjectTupleTestCase<DBTablePartition>>>() {});
        cases.forEach(testCase -> {
            String actual = partitionEditor.generateUpdateObjectDDL(testCase.getInput().getPrevious(),
                    testCase.getInput().getCurrent());
            Assert.assertEquals(testCase.getOutput(), actual);
        });
    }
}
