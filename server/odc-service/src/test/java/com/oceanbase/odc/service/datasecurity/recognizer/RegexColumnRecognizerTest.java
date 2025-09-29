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

public class RegexColumnRecognizerTest {

    @Test
    public void recognize_returnTrue() {
        SensitiveRule rule = createTestRegexRule(1L);
        ColumnRecognizer recognizer = new RegexColumnRecognizer(rule);
        DBTableColumn column = createDBTableColumn("xxx", "xxx", "user_email", "email of user");

        Optional<RecognitionResult> resultOpt = recognizer.recognize(column);

        Assert.assertTrue("The regular expression should match successfully.", resultOpt.isPresent());
        Assert.assertEquals("The matching rule ID should be 1.", rule.getId(), resultOpt.get().getMatchedRuleId());
    }

    @Test
    public void recognize_returnFalse() {
        SensitiveRule rule = createTestRegexRule(1L);
        ColumnRecognizer recognizer = new RegexColumnRecognizer(rule);

        Assert.assertFalse("When Comment is null, it should not match.",
                recognizer.recognize(createDBTableColumn("xxx", "xxx", "user_email", null)).isPresent());

        Assert.assertFalse("It should fail when the SchemaName does not match.",
                recognizer.recognize(createDBTableColumn("   ", "xxx", "user_email", "email of user")).isPresent());

        Assert.assertFalse("When both ColumnName and Comment do not match, it should fail.",
                recognizer.recognize(createDBTableColumn("xxx", "xxx", "user", "some info")).isPresent());
    }


    private SensitiveRule createTestRegexRule(Long id) {
        SensitiveRule rule = new SensitiveRule();
        rule.setId(id);
        rule.setType(SensitiveRuleType.REGEX);
        rule.setEnabled(true);
        rule.setLevel(SensitiveLevel.HIGH);
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
