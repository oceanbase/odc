/*
 * Copyright (c) 2024 OceanBase.
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
package com.oceanbase.odc.service.connection.logicaldatabase.core;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.common.util.YamlUtils;
import com.oceanbase.odc.service.connection.logicaldatabase.core.LogicalTableRecognitionUtils;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.DataNode;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.LogicalTable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class LogicalTableRecognitionUtilsTest {
    private static final String TEST_RESOURCE_FILE_PATH = "logicaldatabase/logical_table_identification.yaml";

    @Test
    public void testGeneratePatternExpressions() {
        List<LogicalTableIdentificationTestCase> testCases =
                YamlUtils.fromYamlList(TEST_RESOURCE_FILE_PATH, LogicalTableIdentificationTestCase.class);
        for (LogicalTableIdentificationTestCase testCase : testCases) {
            List<LogicalTable> actual =
                    LogicalTableRecognitionUtils.recognizeLogicalTablesWithExpression(testCase.getDataNodes());
            List<InnerLogicalTable> expected = testCase.getLogicalTables();

            Assert.assertEquals(String.format("test case id = %d", testCase.getId()), expected.size(), actual.size());

            for (int i = 0; i < expected.size(); i++) {
                InnerLogicalTable expectedTable = expected.get(i);
                LogicalTable actualTable = actual.get(i);
                Assert.assertEquals(String.format("test case id = %d", testCase.getId()), expectedTable.getName(),
                        actualTable.getName());
                Assert.assertEquals(String.format("test case id = %d", testCase.getId()),
                        expectedTable.getTableNamePattern(), actualTable.getTableNamePattern());
                Assert.assertEquals(String.format("test case id = %d", testCase.getId()),
                        expectedTable.getDatabaseNamePattern(), actualTable.getDatabaseNamePattern());
                Assert.assertEquals(String.format("test case id = %d", testCase.getId()),
                        expectedTable.getFullNameExpression(), actualTable.getFullNameExpression());

                Assert.assertEquals(expectedTable.getActualDataNodes().size(), actualTable.getActualDataNodes().size());
                for (int j = 0; j < expectedTable.getActualDataNodes().size(); j++) {
                    DataNode expectedDataNode = expectedTable.getActualDataNodes().get(j);
                    DataNode actualDataNode = actualTable.getActualDataNodes().get(j);
                    Assert.assertEquals(String.format("test case id = %d", testCase.getId()),
                            expectedDataNode.getFullName(), actualDataNode.getFullName());
                }
            }
        }

    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class LogicalTableIdentificationTestCase {
        private Long id;
        private List<DataNode> dataNodes;
        private List<InnerLogicalTable> logicalTables;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class InnerLogicalTable {
        private String name;

        private String fullNameExpression;

        private String databaseNamePattern;

        private String tableNamePattern;

        private List<DataNode> actualDataNodes;
    }
}
