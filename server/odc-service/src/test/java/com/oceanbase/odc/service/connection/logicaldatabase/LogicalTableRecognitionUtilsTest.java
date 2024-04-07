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
package com.oceanbase.odc.service.connection.logicaldatabase;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.common.util.YamlUtils;
import com.oceanbase.odc.service.connection.logicaldatabase.model.DataNode;
import com.oceanbase.odc.service.connection.logicaldatabase.model.LogicalTable;

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
                    LogicalTableRecognitionUtils.generatePatternExpressions(testCase.getDataNodes());
            Assert.assertEquals(testCase.getLogicalTables(), actual);
        }
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class LogicalTableIdentificationTestCase {
        private List<DataNode> dataNodes;
        private List<LogicalTable> logicalTables;
    }
}
