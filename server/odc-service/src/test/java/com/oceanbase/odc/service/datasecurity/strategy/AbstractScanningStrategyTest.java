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
import com.oceanbase.odc.service.datasecurity.model.SensitiveLevel;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRuleType;
import com.oceanbase.odc.service.datasecurity.recognizer.ColumnRecognizer;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

@RunWith(MockitoJUnitRunner.class)
public class AbstractScanningStrategyTest {

    @Mock
    private ColumnRecognizer mockRecognizer1;

    @Mock
    private ColumnRecognizer mockRecognizer2;

    private TestableAbstractScanningStrategy strategy;
    private DBTableColumn testColumn;

    @Before
    public void setUp() {
        strategy = new TestableAbstractScanningStrategy();
        testColumn = createTestColumn("user_phone", "varchar", "user phone number");
    }

    @Test
    public void test_findFirstMatch_withMatchingRecognizer_returnsResult() {
        // Given
        RecognitionResult expectedResult = createRecognitionResult(1L, SensitiveLevel.HIGH);
        List<ColumnRecognizer> recognizers = Arrays.asList(mockRecognizer1, mockRecognizer2);

        Mockito.when(mockRecognizer1.recognize(testColumn)).thenReturn(Optional.empty());
        Mockito.when(mockRecognizer2.recognize(testColumn)).thenReturn(Optional.of(expectedResult));

        // When
        Optional<RecognitionResult> result = strategy.findFirstMatch(recognizers, testColumn);

        // Then
        Assert.assertTrue("Should find matching result", result.isPresent());
        Assert.assertEquals("Should return expected result", expectedResult, result.get());
    }

    @Test
    public void test_findFirstMatch_withNoMatchingRecognizer_returnsEmpty() {
        // Given
        List<ColumnRecognizer> recognizers = Arrays.asList(mockRecognizer1, mockRecognizer2);

        Mockito.when(mockRecognizer1.recognize(testColumn)).thenReturn(Optional.empty());
        Mockito.when(mockRecognizer2.recognize(testColumn)).thenReturn(Optional.empty());

        // When
        Optional<RecognitionResult> result = strategy.findFirstMatch(recognizers, testColumn);

        // Then
        Assert.assertFalse("Should not find any result", result.isPresent());
    }

    @Test
    public void test_findFirstMatch_withEmptyRecognizers_returnsEmpty() {
        // Given
        List<ColumnRecognizer> recognizers = Collections.emptyList();

        // When
        Optional<RecognitionResult> result = strategy.findFirstMatch(recognizers, testColumn);

        // Then
        Assert.assertFalse("Should not find any result with empty recognizers", result.isPresent());
    }

    @Test
    public void test_findAllFirstMatches_withMultipleColumns_returnsCorrectMapping() {
        // Given
        DBTableColumn column1 = createTestColumn("user_phone", "varchar", "phone");
        DBTableColumn column2 = createTestColumn("user_email", "varchar", "email");
        List<DBTableColumn> columns = Arrays.asList(column1, column2);
        List<ColumnRecognizer> recognizers = Arrays.asList(mockRecognizer1);

        RecognitionResult result1 = createRecognitionResult(1L, SensitiveLevel.HIGH);

        // Mock recognizeBatch method for single recognizer scenario
        Map<String, Optional<RecognitionResult>> batchResults = new HashMap<>();
        batchResults.put(strategy.getColumnKey(column1), Optional.of(result1));
        batchResults.put(strategy.getColumnKey(column2), Optional.empty());
        Mockito.when(mockRecognizer1.recognizeBatch(columns)).thenReturn(batchResults);

        // When
        Map<String, Optional<RecognitionResult>> results = strategy.findAllFirstMatches(recognizers, columns);

        // Then
        Assert.assertEquals("Should return results for all columns", 2, results.size());
        Assert.assertTrue("Should find result for column1", results.get(strategy.getColumnKey(column1)).isPresent());
        Assert.assertFalse("Should not find result for column2",
                results.get(strategy.getColumnKey(column2)).isPresent());
    }

    @Test
    public void test_findAllFirstMatches_withEmptyColumns_returnsEmptyMap() {
        // Given
        List<DBTableColumn> columns = Collections.emptyList();
        List<ColumnRecognizer> recognizers = Arrays.asList(mockRecognizer1);

        // When
        Map<String, Optional<RecognitionResult>> results = strategy.findAllFirstMatches(recognizers, columns);

        // Then
        Assert.assertTrue("Should return empty map", results.isEmpty());
    }

    @Test
    public void test_findAllFirstMatches_withEmptyRecognizers_returnsEmptyResults() {
        // Given
        List<DBTableColumn> columns = Arrays.asList(testColumn);
        List<ColumnRecognizer> recognizers = Collections.emptyList();

        // When
        Map<String, Optional<RecognitionResult>> results = strategy.findAllFirstMatches(recognizers, columns);

        // Then
        Assert.assertEquals("Should return results for all columns", 1, results.size());
        Assert.assertFalse("Should not find any result", results.get(strategy.getColumnKey(testColumn)).isPresent());
    }

    @Test
    public void test_findAllFirstMatches_withMultipleRecognizers_usesIndividualRecognize() {
        // Given
        DBTableColumn column1 = createTestColumn("user_phone", "varchar", "phone");
        DBTableColumn column2 = createTestColumn("user_email", "varchar", "email");
        List<DBTableColumn> columns = Arrays.asList(column1, column2);
        List<ColumnRecognizer> recognizers = Arrays.asList(mockRecognizer1, mockRecognizer2);

        RecognitionResult result1 = createRecognitionResult(1L, SensitiveLevel.HIGH);

        // Mock individual recognize calls for multiple recognizers scenario
        Mockito.when(mockRecognizer1.recognize(column1)).thenReturn(Optional.empty());
        Mockito.when(mockRecognizer2.recognize(column1)).thenReturn(Optional.of(result1));
        Mockito.when(mockRecognizer1.recognize(column2)).thenReturn(Optional.empty());
        Mockito.when(mockRecognizer2.recognize(column2)).thenReturn(Optional.empty());

        // When
        Map<String, Optional<RecognitionResult>> results = strategy.findAllFirstMatches(recognizers, columns);

        // Then
        Assert.assertEquals("Should return results for all columns", 2, results.size());
        Assert.assertTrue("Should find result for column1", results.get(strategy.getColumnKey(column1)).isPresent());
        Assert.assertFalse("Should not find result for column2",
                results.get(strategy.getColumnKey(column2)).isPresent());

        // Verify that recognizeBatch was not called for multiple recognizers
        Mockito.verify(mockRecognizer1, Mockito.never()).recognizeBatch(Mockito.any());
        Mockito.verify(mockRecognizer2, Mockito.never()).recognizeBatch(Mockito.any());
    }

    @Test
    public void test_getColumnKey_returnsCorrectFormat() {
        // Given
        DBTableColumn column = createTestColumn("user_phone", "varchar", "phone");
        column.setSchemaName("test_schema");
        column.setTableName("test_table");

        // When
        String key = strategy.getColumnKey(column);

        // Then
        Assert.assertEquals("Should return correct column key format", "test_schema.test_table.user_phone", key);
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

    private RecognitionResult createRecognitionResult(Long ruleId, SensitiveLevel level) {
        return RecognitionResult.builder()
                .matched(true)
                .matchedRuleId(ruleId)
                .level(level)
                .sourceRuleType(SensitiveRuleType.REGEX)
                .build();
    }

    // Testable implementation of AbstractScanningStrategy for testing protected methods
    private static class TestableAbstractScanningStrategy extends AbstractScanningStrategy {
        @Override
        public com.oceanbase.odc.service.datasecurity.model.ScanResult scan(
                DBTableColumn column,
                List<ColumnRecognizer> basicRecognizers,
                List<ColumnRecognizer> aiRecognizers) {
            return null; // Not used in these tests
        }

        @Override
        public Map<String, com.oceanbase.odc.service.datasecurity.model.ScanResult> scanBatch(
                List<DBTableColumn> columns,
                List<ColumnRecognizer> basicRecognizers,
                List<ColumnRecognizer> aiRecognizers) {
            return null; // Not used in these tests
        }

        // Expose protected methods for testing
        @Override
        public Optional<RecognitionResult> findFirstMatch(List<ColumnRecognizer> recognizers, DBTableColumn column) {
            return super.findFirstMatch(recognizers, column);
        }

        @Override
        public Map<String, Optional<RecognitionResult>> findAllFirstMatches(List<ColumnRecognizer> recognizers,
                List<DBTableColumn> columns) {
            return super.findAllFirstMatches(recognizers, columns);
        }

        @Override
        public String getColumnKey(DBTableColumn column) {
            return super.getColumnKey(column);
        }
    }
}
