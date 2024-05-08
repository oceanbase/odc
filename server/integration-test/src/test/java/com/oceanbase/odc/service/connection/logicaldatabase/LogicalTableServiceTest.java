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
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.YamlUtils;
import com.oceanbase.odc.service.connection.logicaldatabase.model.DataNode;
import com.oceanbase.odc.service.connection.logicaldatabase.parser.LogicalTableService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: Lebie
 * @Date: 2024/4/23 11:49
 * @Description: []
 */
public class LogicalTableServiceTest extends ServiceTestEnv {
    private static final String TEST_RESOURCE_VALID_EXPRESSION_FILE_PATH =
            "connection/logicaldatabase/valid_logical_table_expression_resolve.yaml";
    private static final String TEST_RESOURCE_INVALID_EXPRESSION_FILE_PATH =
            "connection/logicaldatabase/invalid_logical_table_expression_resolve.yaml";

    @Autowired
    private LogicalTableService logicalTableService;

    @Test
    public void testResolve_ValidExpression() {
        List<LogicalTableExpressionResolveTestCase> testCases =
                YamlUtils.fromYamlList(TEST_RESOURCE_VALID_EXPRESSION_FILE_PATH,
                        LogicalTableExpressionResolveTestCase.class);
        for (LogicalTableExpressionResolveTestCase testCase : testCases) {
            List<DataNode> actual = logicalTableService.resolve(testCase.getExpression());
            List<DataNode> expected = testCase.getDataNodes();
            Assert.assertEquals(String.format("test case id = %d", testCase.getId()), expected.size(), actual.size());
            for (int i = 0; i < expected.size(); i++) {
                DataNode expectedDataNode = expected.get(i);
                DataNode actualDataNode = actual.get(i);
                Assert.assertEquals(String.format("test case id = %d", testCase.getId()),
                        expectedDataNode.getSchemaName(),
                        actualDataNode.getSchemaName());
                Assert.assertEquals(String.format("test case id = %d", testCase.getId()),
                        expectedDataNode.getTableName(),
                        actualDataNode.getTableName());
            }
        }
    }

    @Test
    public void testResolve_InvalidExpression() {
        List<LogicalTableExpressionResolveTestCase> testCases =
                YamlUtils.fromYamlList(TEST_RESOURCE_INVALID_EXPRESSION_FILE_PATH,
                        LogicalTableExpressionResolveTestCase.class);
        for (LogicalTableExpressionResolveTestCase testCase : testCases) {
            try {
                logicalTableService.resolve(testCase.getExpression());
            } catch (Exception ex) {
                Assert.assertTrue(
                        String.format("test case id = %d, expected = %s, actual = %s", testCase.getId(),
                                testCase.getErrorMessageAbstract(), ex.getMessage()),
                        StringUtils.containsIgnoreCase(ex.getMessage(), testCase.getErrorMessageAbstract()));
                continue;
            }
            Assert.fail(String.format("test case id = %d, exception expected but not thrown", testCase.getId()));
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class LogicalTableExpressionResolveTestCase {
        private Long id;
        private String expression;
        private List<DataNode> dataNodes;
        private String errorMessageAbstract;
    }
}
