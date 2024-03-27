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

package com.oceanbase.odc.service.connection.logicaldatabase;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oceanbase.odc.common.util.YamlUtils;

import junit.framework.TestCase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class LogicalTableUtilsTest {
    private static final String TEST_RESOURCE_FILE_PATH = "logicaldatabase/logical-table-identification.yaml";

    @Test
    public void testGeneratePatternExpressions() {
        List<LogicalTableIdentification> logicalTableIdentifications =
                YamlUtils.fromYamlList(TEST_RESOURCE_FILE_PATH, LogicalTableIdentification.class);
        for (LogicalTableIdentification logicalTableIdentification : logicalTableIdentifications) {
            List<String> input = logicalTableIdentification.getFullTableNames();
            Map<String, List<String>> result = LogicalTableUtils.generatePatternExpressions(input);
            List<Expression2Tables> actual = result.entrySet().stream().map(entry -> new Expression2Tables(entry.getKey(), entry.getValue())).collect(Collectors.toList());
            List<Expression2Tables> expected = logicalTableIdentification.getExpression2Tables();
            Assert.assertEquals(expected, actual);
        }

    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class LogicalTableIdentification {
        @JsonProperty("fullTableNames")
        private List<String> fullTableNames;
        @JsonProperty("expression2Tables")
        private List<Expression2Tables> expression2Tables;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Expression2Tables {
        @JsonProperty("logicalExpression")
        private String logicalExpression;
        @JsonProperty("tables")
        private List<String> tables;
    }
}
