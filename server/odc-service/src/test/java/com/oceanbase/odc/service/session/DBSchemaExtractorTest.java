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
package com.oceanbase.odc.service.session;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.common.util.YamlUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.service.session.util.DBSchemaExtractor;
import com.oceanbase.odc.service.session.util.DBSchemaExtractor.DBSchemaIdentity;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author gaoda.xy
 * @date 2024/5/7 15:08
 */
public class DBSchemaExtractorTest {

    private static final String TEST_RESOURCE_FILE_PATH = "session/test_db_schema_extractor.yaml";

    @Test
    public void test() {
        List<DBSchemaExtractorTestCase> testCases =
                YamlUtils.fromYamlList(TEST_RESOURCE_FILE_PATH, DBSchemaExtractorTestCase.class);
        for (DBSchemaExtractorTestCase testCase : testCases) {
            Map<DBSchemaIdentity, Set<SqlType>> actual = DBSchemaExtractor.listDBSchemasWithSqlTypes(
                    testCase.getSqls().stream().map(SqlTuple::newTuple).collect(Collectors.toList()),
                    testCase.getDialectType(), testCase.getDefaultSchema());
            Assert.assertEquals(testCase.getExpected().size(), actual.size());
            for (DBSchemaIdentity expected : testCase.getExpected()) {
                Assert.assertTrue(actual.containsKey(expected));
            }
            System.out.println("Test case: " + testCase.getId() + " passed.");
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class DBSchemaExtractorTestCase {
        private Integer id;
        private DialectType dialectType;
        private String defaultSchema;
        private List<String> sqls;
        private Set<DBSchemaIdentity> expected;
    }

}
