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
public class AIOnlyStrategyTest {

    @Mock
    private ColumnRecognizer mockBasicRecognizer;

    @Mock
    private ColumnRecognizer mockAiRecognizer;

    private AIOnlyStrategy strategy;
    private List<ColumnRecognizer> basicRecognizers;
    private List<ColumnRecognizer> aiRecognizers;
    private DBTableColumn testColumn;

    @Before
    public void setUp() {
        strategy = new AIOnlyStrategy();
        basicRecognizers = Arrays.asList(mockBasicRecognizer);
        aiRecognizers = Arrays.asList(mockAiRecognizer);
        testColumn = createTestColumn("user_phone", "varchar", "user phone number");
    }

    @Test
    public void test_scan_withAiRecognizerMatch_returnsAiResultOnly() {
        // Given
        RecognitionResult aiResult = createRecognitionResult(2L, SensitiveLevel.MEDIUM, SensitiveRuleType.AI);

        Mockito.when(mockAiRecognizer.recognize(testColumn)).thenReturn(Optional.of(aiResult));

        // When
        ScanResult result = strategy.scan(testColumn, basicRecognizers, aiRecognizers);

        // Then
        Assert.assertFalse("Should not have basic rule result", result.getBasicRuleResult().isPresent());
        Assert.assertTrue("Should have AI rule result", result.getAiRuleResult().isPresent());
        Assert.assertEquals("Should return AI result", aiResult, result.getAiRuleResult().get());

        // Verify basic recognizer is not called (AI only strategy)
        Mockito.verify(mockBasicRecognizer, Mockito.never()).recognize(Mockito.any());
    }

    @Test
    public void test_scan_withNoAiRecognizerMatch_returnsEmptyResult() {
        // Given
        Mockito.when(mockAiRecognizer.recognize(testColumn)).thenReturn(Optional.empty());

        // When
        ScanResult result = strategy.scan(testColumn, basicRecognizers, aiRecognizers);

        // Then
        Assert.assertFalse("Should not have basic rule result", result.getBasicRuleResult().isPresent());
        Assert.assertFalse("Should not have AI rule result", result.getAiRuleResult().isPresent());

        // Verify basic recognizer is not called
        Mockito.verify(mockBasicRecognizer, Mockito.never()).recognize(Mockito.any());
    }

    @Test
    public void test_scan_withEmptyAiRecognizers_returnsEmptyResult() {
        // Given
        List<ColumnRecognizer> emptyAiRecognizers = Collections.emptyList();

        // When
        ScanResult result = strategy.scan(testColumn, basicRecognizers, emptyAiRecognizers);

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

        RecognitionResult aiResult1 = createRecognitionResult(1L, SensitiveLevel.HIGH, SensitiveRuleType.AI);
        RecognitionResult aiResult3 = createRecognitionResult(3L, SensitiveLevel.LOW, SensitiveRuleType.AI);

        // Mock recognizeBatch for single recognizer scenario
        Map<String, Optional<RecognitionResult>> batchResults = new HashMap<>();
        batchResults.put(getColumnKey(column1), Optional.of(aiResult1));
        batchResults.put(getColumnKey(column2), Optional.empty());
        batchResults.put(getColumnKey(column3), Optional.of(aiResult3));
        Mockito.when(mockAiRecognizer.recognizeBatch(columns)).thenReturn(batchResults);

        // When
        Map<String, ScanResult> results = strategy.scanBatch(columns, basicRecognizers, aiRecognizers);

        // Then
        Assert.assertEquals("Should return results for all columns", 3, results.size());

        String key1 = getColumnKey(column1);
        String key2 = getColumnKey(column2);
        String key3 = getColumnKey(column3);

        Assert.assertFalse("Column1 should not have basic result", results.get(key1).getBasicRuleResult().isPresent());
        Assert.assertTrue("Column1 should have AI result", results.get(key1).getAiRuleResult().isPresent());

        Assert.assertFalse("Column2 should not have basic result", results.get(key2).getBasicRuleResult().isPresent());
        Assert.assertFalse("Column2 should not have AI result", results.get(key2).getAiRuleResult().isPresent());

        Assert.assertFalse("Column3 should not have basic result", results.get(key3).getBasicRuleResult().isPresent());
        Assert.assertTrue("Column3 should have AI result", results.get(key3).getAiRuleResult().isPresent());

        // Verify basic recognizer is never called
        Mockito.verify(mockBasicRecognizer, Mockito.never()).recognize(Mockito.any());
        Mockito.verify(mockBasicRecognizer, Mockito.never()).recognizeBatch(Mockito.any());
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
    public void test_scanBatch_withEmptyAiRecognizers_returnsEmptyResults() {
        // Given
        List<DBTableColumn> columns = Arrays.asList(testColumn);
        List<ColumnRecognizer> emptyAiRecognizers = Collections.emptyList();

        // When
        Map<String, ScanResult> results = strategy.scanBatch(columns, basicRecognizers, emptyAiRecognizers);

        // Then
        Assert.assertEquals("Should return results for all columns", 1, results.size());
        String key = getColumnKey(testColumn);
        Assert.assertFalse("Should not have basic result", results.get(key).getBasicRuleResult().isPresent());
        Assert.assertFalse("Should not have AI result", results.get(key).getAiRuleResult().isPresent());
    }

    @Test
    public void test_scanBatch_withSingleAiRecognizer_usesBatchRecognition() {
        // Given
        List<DBTableColumn> columns = Arrays.asList(testColumn);
        Map<String, Optional<RecognitionResult>> batchResults = Collections.singletonMap(
                getColumnKey(testColumn),
                Optional.of(createRecognitionResult(1L, SensitiveLevel.HIGH, SensitiveRuleType.AI)));

        Mockito.when(mockAiRecognizer.recognizeBatch(columns)).thenReturn(batchResults);

        // When
        Map<String, ScanResult> results = strategy.scanBatch(columns, basicRecognizers, aiRecognizers);

        // Then
        Assert.assertEquals("Should return results for all columns", 1, results.size());
        String key = getColumnKey(testColumn);
        Assert.assertTrue("Should have AI result", results.get(key).getAiRuleResult().isPresent());

        // Verify batch recognition is used
        Mockito.verify(mockAiRecognizer).recognizeBatch(columns);
        Mockito.verify(mockAiRecognizer, Mockito.never()).recognize(Mockito.any());
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
