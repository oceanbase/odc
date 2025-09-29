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
public class JointRecognitionStrategyTest {

    @Mock
    private ColumnRecognizer mockBasicRecognizer;

    @Mock
    private ColumnRecognizer mockAiRecognizer;

    private JointRecognitionStrategy strategy;
    private List<ColumnRecognizer> basicRecognizers;
    private List<ColumnRecognizer> aiRecognizers;
    private DBTableColumn testColumn;

    @Before
    public void setUp() {
        strategy = new JointRecognitionStrategy();
        basicRecognizers = Arrays.asList(mockBasicRecognizer);
        aiRecognizers = Arrays.asList(mockAiRecognizer);
        testColumn = createTestColumn("user_phone", "varchar", "user phone number");
    }

    @Test
    public void test_scan_withBasicRecognizerMatch_returnsBasicResultAndSkipsAi() {
        // Given
        RecognitionResult basicResult = createRecognitionResult(1L, SensitiveLevel.HIGH, SensitiveRuleType.REGEX);

        Mockito.when(mockBasicRecognizer.recognize(testColumn)).thenReturn(Optional.of(basicResult));

        // When
        ScanResult result = strategy.scan(testColumn, basicRecognizers, aiRecognizers);

        // Then
        Assert.assertTrue("Should have basic rule result", result.getBasicRuleResult().isPresent());
        Assert.assertFalse("Should not have AI rule result", result.getAiRuleResult().isPresent());
        Assert.assertEquals("Should return basic result", basicResult, result.getBasicRuleResult().get());

        // Verify AI recognizer is not called when basic recognizer matches
        Mockito.verify(mockAiRecognizer, Mockito.never()).recognize(Mockito.any());
    }

    @Test
    public void test_scan_withNoBasicMatchButAiMatch_returnsAiResult() {
        // Given
        RecognitionResult aiResult = createRecognitionResult(2L, SensitiveLevel.MEDIUM, SensitiveRuleType.AI);

        Mockito.when(mockBasicRecognizer.recognize(testColumn)).thenReturn(Optional.empty());
        Mockito.when(mockAiRecognizer.recognize(testColumn)).thenReturn(Optional.of(aiResult));

        // When
        ScanResult result = strategy.scan(testColumn, basicRecognizers, aiRecognizers);

        // Then
        Assert.assertFalse("Should not have basic rule result", result.getBasicRuleResult().isPresent());
        Assert.assertTrue("Should have AI rule result", result.getAiRuleResult().isPresent());
        Assert.assertEquals("Should return AI result", aiResult, result.getAiRuleResult().get());

        // Verify AI recognizer is called as fallback
        Mockito.verify(mockAiRecognizer).recognize(testColumn);
    }

    @Test
    public void test_scan_withNoMatches_returnsEmptyResult() {
        // Given
        Mockito.when(mockBasicRecognizer.recognize(testColumn)).thenReturn(Optional.empty());
        Mockito.when(mockAiRecognizer.recognize(testColumn)).thenReturn(Optional.empty());

        // When
        ScanResult result = strategy.scan(testColumn, basicRecognizers, aiRecognizers);

        // Then
        Assert.assertFalse("Should not have basic rule result", result.getBasicRuleResult().isPresent());
        Assert.assertFalse("Should not have AI rule result", result.getAiRuleResult().isPresent());

        // Verify both recognizers are called
        Mockito.verify(mockBasicRecognizer).recognize(testColumn);
        Mockito.verify(mockAiRecognizer).recognize(testColumn);
    }

    @Test
    public void test_scanBatch_withMixedResults_optimizesAiCalls() {
        // Given
        DBTableColumn column1 = createTestColumn("user_phone", "varchar", "phone");
        DBTableColumn column2 = createTestColumn("user_email", "varchar", "email");
        DBTableColumn column3 = createTestColumn("user_name", "varchar", "name");
        DBTableColumn column4 = createTestColumn("user_id", "bigint", "id");
        List<DBTableColumn> columns = Arrays.asList(column1, column2, column3, column4);

        // Basic recognizer matches column1 and column3
        RecognitionResult basicResult1 = createRecognitionResult(1L, SensitiveLevel.HIGH, SensitiveRuleType.REGEX);
        RecognitionResult basicResult3 = createRecognitionResult(3L, SensitiveLevel.LOW, SensitiveRuleType.GROOVY);

        // Mock recognizeBatch for basic recognizer (single recognizer scenario)
        Map<String, Optional<RecognitionResult>> basicBatchResults = new HashMap<>();
        basicBatchResults.put(getColumnKey(column1), Optional.of(basicResult1));
        basicBatchResults.put(getColumnKey(column2), Optional.empty());
        basicBatchResults.put(getColumnKey(column3), Optional.of(basicResult3));
        basicBatchResults.put(getColumnKey(column4), Optional.empty());
        Mockito.when(mockBasicRecognizer.recognizeBatch(columns)).thenReturn(basicBatchResults);

        // AI recognizer only matches column2 (column4 has no match)
        RecognitionResult aiResult2 = createRecognitionResult(2L, SensitiveLevel.MEDIUM, SensitiveRuleType.AI);

        // Mock recognizeBatch for AI recognizer on remaining columns (column2, column4)
        List<DBTableColumn> remainingColumns = Arrays.asList(column2, column4);
        Map<String, Optional<RecognitionResult>> aiBatchResults = new HashMap<>();
        aiBatchResults.put(getColumnKey(column2), Optional.of(aiResult2));
        aiBatchResults.put(getColumnKey(column4), Optional.empty());
        Mockito.when(mockAiRecognizer.recognizeBatch(remainingColumns)).thenReturn(aiBatchResults);

        // When
        Map<String, ScanResult> results = strategy.scanBatch(columns, basicRecognizers, aiRecognizers);

        // Then
        Assert.assertEquals("Should return results for all columns", 4, results.size());

        String key1 = getColumnKey(column1);
        String key2 = getColumnKey(column2);
        String key3 = getColumnKey(column3);
        String key4 = getColumnKey(column4);

        // Column1: basic match, no AI call
        Assert.assertTrue("Column1 should have basic result", results.get(key1).getBasicRuleResult().isPresent());
        Assert.assertFalse("Column1 should not have AI result", results.get(key1).getAiRuleResult().isPresent());

        // Column2: no basic match, AI match
        Assert.assertFalse("Column2 should not have basic result", results.get(key2).getBasicRuleResult().isPresent());
        Assert.assertTrue("Column2 should have AI result", results.get(key2).getAiRuleResult().isPresent());

        // Column3: basic match, no AI call
        Assert.assertTrue("Column3 should have basic result", results.get(key3).getBasicRuleResult().isPresent());
        Assert.assertFalse("Column3 should not have AI result", results.get(key3).getAiRuleResult().isPresent());

        // Column4: no matches
        Assert.assertFalse("Column4 should not have basic result", results.get(key4).getBasicRuleResult().isPresent());
        Assert.assertFalse("Column4 should not have AI result", results.get(key4).getAiRuleResult().isPresent());

        // Verify AI recognizer is only called for columns without basic matches
        Mockito.verify(mockAiRecognizer).recognizeBatch(remainingColumns);
        Mockito.verify(mockAiRecognizer, Mockito.never()).recognize(Mockito.any());
    }

    @Test
    public void test_scanBatch_withAllBasicMatches_skipsAiCompletely() {
        // Given
        DBTableColumn column1 = createTestColumn("user_phone", "varchar", "phone");
        DBTableColumn column2 = createTestColumn("user_email", "varchar", "email");
        List<DBTableColumn> columns = Arrays.asList(column1, column2);

        RecognitionResult basicResult1 = createRecognitionResult(1L, SensitiveLevel.HIGH, SensitiveRuleType.REGEX);
        RecognitionResult basicResult2 = createRecognitionResult(2L, SensitiveLevel.MEDIUM, SensitiveRuleType.GROOVY);

        // Mock recognizeBatch for basic recognizer (all matches)
        Map<String, Optional<RecognitionResult>> basicBatchResults = new HashMap<>();
        basicBatchResults.put(getColumnKey(column1), Optional.of(basicResult1));
        basicBatchResults.put(getColumnKey(column2), Optional.of(basicResult2));
        Mockito.when(mockBasicRecognizer.recognizeBatch(columns)).thenReturn(basicBatchResults);

        // When
        Map<String, ScanResult> results = strategy.scanBatch(columns, basicRecognizers, aiRecognizers);

        // Then
        Assert.assertEquals("Should return results for all columns", 2, results.size());

        // Verify AI recognizer is never called
        Mockito.verify(mockAiRecognizer, Mockito.never()).recognize(Mockito.any());
        Mockito.verify(mockAiRecognizer, Mockito.never()).recognizeBatch(Mockito.any());
    }

    @Test
    public void test_scanBatch_withNoBasicMatches_callsAiForAll() {
        // Given
        DBTableColumn column1 = createTestColumn("user_phone", "varchar", "phone");
        DBTableColumn column2 = createTestColumn("user_email", "varchar", "email");
        List<DBTableColumn> columns = Arrays.asList(column1, column2);

        // Mock recognizeBatch for basic recognizer (no matches)
        Map<String, Optional<RecognitionResult>> basicBatchResults = new HashMap<>();
        basicBatchResults.put(getColumnKey(column1), Optional.empty());
        basicBatchResults.put(getColumnKey(column2), Optional.empty());
        Mockito.when(mockBasicRecognizer.recognizeBatch(columns)).thenReturn(basicBatchResults);

        RecognitionResult aiResult1 = createRecognitionResult(1L, SensitiveLevel.HIGH, SensitiveRuleType.AI);

        // Mock recognizeBatch for AI recognizer (all columns since no basic matches)
        Map<String, Optional<RecognitionResult>> aiBatchResults = new HashMap<>();
        aiBatchResults.put(getColumnKey(column1), Optional.of(aiResult1));
        aiBatchResults.put(getColumnKey(column2), Optional.empty());
        Mockito.when(mockAiRecognizer.recognizeBatch(columns)).thenReturn(aiBatchResults);

        // When
        Map<String, ScanResult> results = strategy.scanBatch(columns, basicRecognizers, aiRecognizers);

        // Then
        Assert.assertEquals("Should return results for all columns", 2, results.size());

        // Verify AI recognizer is called for all columns
        Mockito.verify(mockAiRecognizer).recognizeBatch(columns);
        Mockito.verify(mockAiRecognizer, Mockito.never()).recognize(Mockito.any());
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
    public void test_scanBatch_withEmptyRecognizers_returnsEmptyResults() {
        // Given
        List<DBTableColumn> columns = Arrays.asList(testColumn);
        List<ColumnRecognizer> emptyBasicRecognizers = Collections.emptyList();
        List<ColumnRecognizer> emptyAiRecognizers = Collections.emptyList();

        // When
        Map<String, ScanResult> results = strategy.scanBatch(columns, emptyBasicRecognizers, emptyAiRecognizers);

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
