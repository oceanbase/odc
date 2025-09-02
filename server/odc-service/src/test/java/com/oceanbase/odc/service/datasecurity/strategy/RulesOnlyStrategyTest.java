/*
 * Copyright (c) 2025 OceanBase.
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
package com.oceanbase.odc.service.datasecurity.strategy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.oceanbase.odc.service.datasecurity.model.RecognitionResult;
import com.oceanbase.odc.service.datasecurity.model.ScanResult;
import com.oceanbase.odc.service.datasecurity.model.SensitiveLevel;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRuleType;
import com.oceanbase.odc.service.datasecurity.recognizer.ColumnRecognizer;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;


@RunWith(MockitoJUnitRunner.class)
public class RulesOnlyStrategyTest {

    @Mock
    private ColumnRecognizer mockBasicRecognizer;

    @Mock
    private ColumnRecognizer mockAiRecognizer;

    private RulesOnlyStrategy strategy;
    private List<ColumnRecognizer> basicRecognizers;
    private List<ColumnRecognizer> aiRecognizers;
    private DBTableColumn testColumn;

    @Before
    public void setUp() {
        strategy = new RulesOnlyStrategy();
        basicRecognizers = Arrays.asList(mockBasicRecognizer);
        aiRecognizers = Arrays.asList(mockAiRecognizer);
        testColumn = createTestColumn("user_phone", "varchar", "user phone number");
    }

    @Test
    public void test_scan_withBasicRecognizerMatch_returnsBasicResultOnly() {
        // Given
        RecognitionResult basicResult = createRecognitionResult(1L, SensitiveLevel.HIGH, SensitiveRuleType.REGEX);

        Mockito.when(mockBasicRecognizer.recognize(testColumn)).thenReturn(Optional.of(basicResult));

        // When
        ScanResult result = strategy.scan(testColumn, basicRecognizers, aiRecognizers);

        // Then
        Assert.assertTrue("Should have basic rule result", result.getBasicRuleResult().isPresent());
        Assert.assertFalse("Should not have AI rule result", result.getAiRuleResult().isPresent());
        Assert.assertEquals("Should return basic result", basicResult, result.getBasicRuleResult().get());

        // Verify AI recognizer is not called (rules only strategy)
        Mockito.verify(mockAiRecognizer, Mockito.never()).recognize(Mockito.any());
    }

    @Test
    public void test_scan_withNoBasicRecognizerMatch_returnsEmptyResult() {
        // Given
        Mockito.when(mockBasicRecognizer.recognize(testColumn)).thenReturn(Optional.empty());

        // When
        ScanResult result = strategy.scan(testColumn, basicRecognizers, aiRecognizers);

        // Then
        Assert.assertFalse("Should not have basic rule result", result.getBasicRuleResult().isPresent());
        Assert.assertFalse("Should not have AI rule result", result.getAiRuleResult().isPresent());

        // Verify AI recognizer is not called
        Mockito.verify(mockAiRecognizer, Mockito.never()).recognize(Mockito.any());
    }

    @Test
    public void test_scan_withEmptyBasicRecognizers_returnsEmptyResult() {
        // Given
        List<ColumnRecognizer> emptyBasicRecognizers = Collections.emptyList();

        // When
        ScanResult result = strategy.scan(testColumn, emptyBasicRecognizers, aiRecognizers);

        // Then
        Assert.assertFalse("Should not have basic rule result", result.getBasicRuleResult().isPresent());
        Assert.assertFalse("Should not have AI rule result", result.getAiRuleResult().isPresent());
    }

    @Test
    public void test_scanBatch_withMixedResults_returnsCorrectMapping() {
        // Given
        DBTableColumn column1 = createTestColumn("user_phone", "varchar", "phone");
        DBTableColumn column2 = createTestColumn("user_email", "varchar", "email");
        DBTableColumn column3 = createTestColumn("user_name", "varchar", "name");
        List<DBTableColumn> columns = Arrays.asList(column1, column2, column3);

        RecognitionResult result1 = createRecognitionResult(1L, SensitiveLevel.HIGH, SensitiveRuleType.REGEX);
        RecognitionResult result3 = createRecognitionResult(3L, SensitiveLevel.LOW, SensitiveRuleType.GROOVY);

        // Mock recognizeBatch for single recognizer scenario
        Map<String, Optional<RecognitionResult>> batchResults = new HashMap<>();
        batchResults.put(getColumnKey(column1), Optional.of(result1));
        batchResults.put(getColumnKey(column2), Optional.empty());
        batchResults.put(getColumnKey(column3), Optional.of(result3));
        Mockito.when(mockBasicRecognizer.recognizeBatch(columns)).thenReturn(batchResults);

        // When
        Map<String, ScanResult> results = strategy.scanBatch(columns, basicRecognizers, aiRecognizers);

        // Then
        Assert.assertEquals("Should return results for all columns", 3, results.size());

        String key1 = getColumnKey(column1);
        String key2 = getColumnKey(column2);
        String key3 = getColumnKey(column3);

        Assert.assertTrue("Column1 should have basic result", results.get(key1).getBasicRuleResult().isPresent());
        Assert.assertFalse("Column1 should not have AI result", results.get(key1).getAiRuleResult().isPresent());

        Assert.assertFalse("Column2 should not have basic result", results.get(key2).getBasicRuleResult().isPresent());
        Assert.assertFalse("Column2 should not have AI result", results.get(key2).getAiRuleResult().isPresent());

        Assert.assertTrue("Column3 should have basic result", results.get(key3).getBasicRuleResult().isPresent());
        Assert.assertFalse("Column3 should not have AI result", results.get(key3).getAiRuleResult().isPresent());

        // Verify AI recognizer is never called
        Mockito.verify(mockAiRecognizer, Mockito.never()).recognize(Mockito.any());
        Mockito.verify(mockAiRecognizer, Mockito.never()).recognizeBatch(Mockito.any());
    }

    @Test
    public void test_scanBatch_withEmptyColumns_returnsEmptyMap() {
        // Given
        List<DBTableColumn> emptyColumns = Collections.emptyList();

        // When
        Map<String, ScanResult> results = strategy.scanBatch(emptyColumns, basicRecognizers, aiRecognizers);

        // Then
        Assert.assertTrue("Should return empty map", results.isEmpty());
    }

    @Test
    public void test_scanBatch_withEmptyBasicRecognizers_returnsEmptyResults() {
        // Given
        List<DBTableColumn> columns = Arrays.asList(testColumn);
        List<ColumnRecognizer> emptyBasicRecognizers = Collections.emptyList();

        // When
        Map<String, ScanResult> results = strategy.scanBatch(columns, emptyBasicRecognizers, aiRecognizers);

        // Then
        Assert.assertEquals("Should return results for all columns", 1, results.size());
        String key = getColumnKey(testColumn);
        Assert.assertFalse("Should not have basic result", results.get(key).getBasicRuleResult().isPresent());
        Assert.assertFalse("Should not have AI result", results.get(key).getAiRuleResult().isPresent());
    }

    private DBTableColumn createTestColumn(String columnName, String typeName, String comment) {
        DBTableColumn column = new DBTableColumn();
        column.setName(columnName);
        column.setTypeName(typeName);
        column.setComment(comment);
        column.setSchemaName("test_schema");
        column.setTableName("test_table");
        return column;
    }

    private RecognitionResult createRecognitionResult(Long ruleId, SensitiveLevel level, SensitiveRuleType ruleType) {
        return RecognitionResult.builder()
            .matched(true)
            .matchedRuleId(ruleId)
            .level(level)
            .sourceRuleType(ruleType)
            .build();
    }

    private String getColumnKey(DBTableColumn column) {
        return String.format("%s.%s.%s",
            column.getSchemaName() != null ? column.getSchemaName() : "unknown_schema",
            column.getTableName() != null ? column.getTableName() : "unknown_table",
            column.getName() != null ? column.getName() : "unknown_column");
    }
}