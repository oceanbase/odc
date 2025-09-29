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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.service.datasecurity.model.RecognitionResult;
import com.oceanbase.odc.service.datasecurity.model.SensitiveLevel;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRule;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRuleType;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

public class PathColumnRecognizerTest {

    @Test
    public void test_recognize_true() {
        SensitiveRule rule = createPathRule(1L, Arrays.asList("*.*b*.c"), Arrays.asList("a.b.*"));
        ColumnRecognizer recognizer = new PathColumnRecognizer(rule);

        Optional<RecognitionResult> result1 = recognizer.recognize(createDBTableColumn("a", "b12", "c"));
        Assert.assertTrue("Path 'a.b12.c' should match successfully", result1.isPresent());
        Assert.assertEquals(rule.getId(), result1.get().getMatchedRuleId());

        Optional<RecognitionResult> result2 = recognizer.recognize(createDBTableColumn("a12", "34b56", "c"));
        Assert.assertTrue("Path 'a12.34b56.c' should match successfully", result2.isPresent());

        Optional<RecognitionResult> result3 = recognizer.recognize(createDBTableColumn("a12", "34b", "c"));
        Assert.assertTrue("Path 'a12.34b.c' should match successfully", result3.isPresent());
    }

    @Test
    public void test_recognize_false() {
        SensitiveRule rule = createPathRule(1L, Arrays.asList("*.*b*.c"), Arrays.asList("a.b.*"));
        ColumnRecognizer recognizer = new PathColumnRecognizer(rule);

        Assert.assertFalse("The path 'a.b.c' should be excluded as the match failed.",
                recognizer.recognize(createDBTableColumn("a", "b", "c")).isPresent());

        Assert.assertFalse("Path 'a12.b34.c56' should not match; the match failed.",
                recognizer.recognize(createDBTableColumn("a12", "b34", "c56")).isPresent());

        Assert.assertFalse("Path 'a12.b34.null' should not match; the match failed.",
                recognizer.recognize(createDBTableColumn("a12", "b34", null)).isPresent());
    }


    private DBTableColumn createDBTableColumn(String schemaName, String tableName, String columnName) {
        DBTableColumn column = new DBTableColumn();
        column.setSchemaName(schemaName);
        column.setTableName(tableName);
        column.setName(columnName);
        return column;
    }

    private SensitiveRule createPathRule(Long id, List<String> includes, List<String> excludes) {
        SensitiveRule rule = new SensitiveRule();
        rule.setId(id);
        rule.setType(SensitiveRuleType.PATH);
        rule.setPathIncludes(includes);
        rule.setPathExcludes(excludes);
        rule.setLevel(SensitiveLevel.HIGH);
        rule.setEnabled(true);
        return rule;
    }
}
