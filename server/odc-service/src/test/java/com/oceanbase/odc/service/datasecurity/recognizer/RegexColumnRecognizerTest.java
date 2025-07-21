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
package com.oceanbase.odc.service.datasecurity.recognizer;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.service.datasecurity.model.RecognitionResult;
import com.oceanbase.odc.service.datasecurity.model.SensitiveLevel;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRule;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRuleType;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

/**
 * @author gaoda.xy
 * @date 2023/5/24 16:21
 */
public class RegexColumnRecognizerTest {

    @Test
    public void recognize_returnTrue() {
        // 【修改】通过辅助方法创建规则，并用规则创建识别器
        SensitiveRule rule = createTestRegexRule(1L);
        ColumnRecognizer recognizer = new RegexColumnRecognizer(rule);
        DBTableColumn column = createDBTableColumn("xxx", "xxx", "user_email", "email of user");

        // 【修改】调用新的 recognize 方法并检查 Optional 返回值
        Optional<RecognitionResult> resultOpt = recognizer.recognize(column);

        // 【修改】断言结果存在，并验证内容
        Assert.assertTrue("正则表达式应匹配成功", resultOpt.isPresent());
        Assert.assertEquals("匹配的规则ID应为 1", rule.getId(), resultOpt.get().getMatchedRuleId());
    }

    @Test
    public void recognize_returnFalse() {
        // 【修改】通过辅助方法创建规则
        SensitiveRule rule = createTestRegexRule(1L);
        ColumnRecognizer recognizer = new RegexColumnRecognizer(rule);

        // 【修改】断言方式改为检查 !Optional.isPresent()
        // 原作者的每个测试用例都予以保留
        Assert.assertFalse("Comment 为 null 时不应匹配",
                recognizer.recognize(createDBTableColumn("xxx", "xxx", "user_email", null)).isPresent());

        Assert.assertFalse("SchemaName 不匹配时应失败",
                recognizer.recognize(createDBTableColumn("   ", "xxx", "user_email", "email of user")).isPresent());

        Assert.assertFalse("ColumnName 和 Comment 都不匹配时应失败",
                recognizer.recognize(createDBTableColumn("xxx", "xxx", "user", "some info")).isPresent());
    }


    // --- 辅助方法 ---

    // 【修改】原 createRegexColumnRecognizer 替换为 createTestRegexRule
    private SensitiveRule createTestRegexRule(Long id) {
        SensitiveRule rule = new SensitiveRule();
        rule.setId(id);
        rule.setType(SensitiveRuleType.REGEX);
        rule.setEnabled(true);
        rule.setLevel(SensitiveLevel.HIGH);
        // 保留原作者的正则表达式
        rule.setDatabaseRegexExpression("^\\S+$");
        rule.setTableRegexExpression("^\\S+$");
        rule.setColumnRegexExpression("^\\S*email\\S*$");
        rule.setColumnCommentRegexExpression("^[\\S\\s]*email[\\S\\s]*$");
        return rule;
    }

    private DBTableColumn createDBTableColumn(String schemaName, String tableName, String columnName, String comment) {
        DBTableColumn column = new DBTableColumn();
        column.setSchemaName(schemaName);
        column.setTableName(tableName);
        column.setName(columnName);
        column.setComment(comment);
        return column;
    }
}
