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

import java.util.Collections;
import java.util.HashMap;
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
        // 从指定路径的YAML文件中读取测试用例列表
        List<DBSchemaExtractorTestCase> testCases =
            YamlUtils.fromYamlList(TEST_RESOURCE_FILE_PATH, DBSchemaExtractorTestCase.class);
        // 遍历测试用例列表
        for (DBSchemaExtractorTestCase testCase : testCases) {
            // 调用被测方法，获取实际结果
            Map<DBSchemaIdentity, Set<SqlType>> actual = DBSchemaExtractor.listDBSchemasWithSqlTypes(
                testCase.getSqls().stream().map(SqlTuple::newTuple).collect(Collectors.toList()),
                testCase.getDialectType(), testCase.getDefaultSchema());
            // 断言实际结果和期望结果的大小相同
            Assert.assertEquals(testCase.getExpected().size(), actual.size());
            // 断言实际结果包含期望结果中的所有元素
            for (DBSchemaIdentity expected : testCase.getExpected()) {
                Assert.assertTrue(actual.containsKey(expected));
            }
            // 打印测试通过的信息
            System.out.println("Test case: " + testCase.getId() + " passed.");
        }
    }

    @Test
    public void listDBSchemasWithSqlTypes_anonymousBlock_listSucceed() {
        String pl = "DECLARE\n"
                + "    i VARCHAR2(300);\n"
                + "BEGIN\n"
                + "    select ps_auto_refresh_publish_pkg.getloopup_meaning('YES_NO', 'Y') into i from dual;\n"
                + "    dbms_output.put_line(i);\n"
                + "END;";
        Map<DBSchemaIdentity, Set<SqlType>> actual = DBSchemaExtractor.listDBSchemasWithSqlTypes(
                Collections.singletonList(SqlTuple.newTuple(pl)), DialectType.OB_ORACLE, "aps");
        Map<DBSchemaIdentity, Set<SqlType>> expect = new HashMap<>();
        DBSchemaIdentity dbSchemaIdentity = new DBSchemaIdentity();
        dbSchemaIdentity.setSchema("PS_AUTO_REFRESH_PUBLISH_PKG");
        expect.put(dbSchemaIdentity, Collections.singleton(SqlType.OTHERS));
        Assert.assertEquals(expect, actual);
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
