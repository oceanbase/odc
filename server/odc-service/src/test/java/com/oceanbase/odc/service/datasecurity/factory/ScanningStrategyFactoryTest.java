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
package com.oceanbase.odc.service.datasecurity.factory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.oceanbase.odc.service.datasecurity.model.ScanResult;
import com.oceanbase.odc.service.datasecurity.model.ScanningModeType;
import com.oceanbase.odc.service.datasecurity.recognizer.ColumnRecognizer;
import com.oceanbase.odc.service.datasecurity.strategy.AIOnlyStrategy;
import com.oceanbase.odc.service.datasecurity.strategy.JointRecognitionStrategy;
import com.oceanbase.odc.service.datasecurity.strategy.RulesOnlyStrategy;
import com.oceanbase.odc.service.datasecurity.strategy.ScanningStrategy;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

@RunWith(MockitoJUnitRunner.class)
public class ScanningStrategyFactoryTest {

    private ScanningStrategyFactory factory;
    private DBTableColumn testColumn;
    private List<ColumnRecognizer> emptyRecognizers;

    @Before
    public void setUp() {
        factory = new ScanningStrategyFactory();
        testColumn = createTestColumn("user_phone", "varchar", "user phone number");
        emptyRecognizers = Collections.emptyList();
    }

    @Test
    public void test_getStrategy_withRulesOnly_returnsRulesOnlyStrategy() {
        // When
        ScanningStrategy strategy = factory.getStrategy(ScanningModeType.RULES_ONLY);

        // Then
        Assert.assertNotNull("Should return a strategy", strategy);
        Assert.assertTrue("Should return RulesOnlyStrategy", strategy instanceof RulesOnlyStrategy);
    }

    @Test
    public void test_getStrategy_withAiOnly_returnsAiOnlyStrategy() {
        // When
        ScanningStrategy strategy = factory.getStrategy(ScanningModeType.AI_ONLY);

        // Then
        Assert.assertNotNull("Should return a strategy", strategy);
        Assert.assertTrue("Should return AIOnlyStrategy", strategy instanceof AIOnlyStrategy);
    }

    @Test
    public void test_getStrategy_withJointRecognition_returnsJointRecognitionStrategy() {
        // When
        ScanningStrategy strategy = factory.getStrategy(ScanningModeType.JOINT_RECOGNITION);

        // Then
        Assert.assertNotNull("Should return a strategy", strategy);
        Assert.assertTrue("Should return JointRecognitionStrategy", strategy instanceof JointRecognitionStrategy);
    }

    @Test
    public void test_getStrategy_withNullMode_returnsNoOpStrategy() {
        // When
        ScanningStrategy strategy = factory.getStrategy(null);

        // Then
        Assert.assertNotNull("Should return a strategy", strategy);

        // Test that it behaves like NoOpStrategy
        ScanResult result = strategy.scan(testColumn, emptyRecognizers, emptyRecognizers);
        Assert.assertFalse("NoOp strategy should not have basic result", result.getBasicRuleResult().isPresent());
        Assert.assertFalse("NoOp strategy should not have AI result", result.getAiRuleResult().isPresent());
    }

    @Test
    public void test_getStrategy_returnsSameInstanceForSameMode() {
        // When
        ScanningStrategy strategy1 = factory.getStrategy(ScanningModeType.RULES_ONLY);
        ScanningStrategy strategy2 = factory.getStrategy(ScanningModeType.RULES_ONLY);

        // Then
        Assert.assertSame("Should return same instance for same mode", strategy1, strategy2);
    }

    @Test
    public void test_getStrategy_returnsDifferentInstancesForDifferentModes() {
        // When
        ScanningStrategy rulesOnlyStrategy = factory.getStrategy(ScanningModeType.RULES_ONLY);
        ScanningStrategy aiOnlyStrategy = factory.getStrategy(ScanningModeType.AI_ONLY);
        ScanningStrategy jointStrategy = factory.getStrategy(ScanningModeType.JOINT_RECOGNITION);

        // Then
        Assert.assertNotSame("Rules and AI strategies should be different", rulesOnlyStrategy, aiOnlyStrategy);
        Assert.assertNotSame("Rules and Joint strategies should be different", rulesOnlyStrategy, jointStrategy);
        Assert.assertNotSame("AI and Joint strategies should be different", aiOnlyStrategy, jointStrategy);
    }

    @Test
    public void test_allStrategies_implementScanningStrategy() {
        // Given
        ScanningModeType[] allModes =
                {ScanningModeType.RULES_ONLY, ScanningModeType.AI_ONLY, ScanningModeType.JOINT_RECOGNITION};

        // When & Then
        for (ScanningModeType mode : allModes) {
            ScanningStrategy strategy = factory.getStrategy(mode);
            Assert.assertNotNull("Strategy should not be null for mode: " + mode, strategy);
            Assert.assertTrue("Strategy should implement ScanningStrategy for mode: " + mode,
                    strategy instanceof ScanningStrategy);
        }
    }

    @Test
    public void test_allStrategies_canHandleBasicOperations() {
        // Given
        ScanningModeType[] allModes =
                {ScanningModeType.RULES_ONLY, ScanningModeType.AI_ONLY, ScanningModeType.JOINT_RECOGNITION};
        List<DBTableColumn> columns = Arrays.asList(testColumn);

        // When & Then
        for (ScanningModeType mode : allModes) {
            ScanningStrategy strategy = factory.getStrategy(mode);

            // Test single scan
            ScanResult singleResult = strategy.scan(testColumn, emptyRecognizers, emptyRecognizers);
            Assert.assertNotNull("Single scan result should not be null for mode: " + mode, singleResult);

            // Test batch scan
            Map<String, ScanResult> batchResults = strategy.scanBatch(columns, emptyRecognizers, emptyRecognizers);
            Assert.assertNotNull("Batch scan results should not be null for mode: " + mode, batchResults);
            Assert.assertEquals("Batch scan should return result for each column for mode: " + mode,
                    1, batchResults.size());
        }
    }

    @Test
    public void test_noOpStrategy_handlesBatchScanCorrectly() {
        // Given
        ScanningStrategy noOpStrategy = factory.getStrategy(null);
        DBTableColumn column1 = createTestColumn("col1", "varchar", "comment1");
        DBTableColumn column2 = createTestColumn("col2", "varchar", "comment2");
        List<DBTableColumn> columns = Arrays.asList(column1, column2);

        // When
        Map<String, ScanResult> results = noOpStrategy.scanBatch(columns, emptyRecognizers, emptyRecognizers);

        // Then
        Assert.assertEquals("Should return results for all columns", 2, results.size());

        for (ScanResult result : results.values()) {
            Assert.assertFalse("NoOp strategy should not have basic result", result.getBasicRuleResult().isPresent());
            Assert.assertFalse("NoOp strategy should not have AI result", result.getAiRuleResult().isPresent());
        }
    }

    @Test
    public void test_noOpStrategy_handlesNullColumnsGracefully() {
        // Given
        ScanningStrategy noOpStrategy = factory.getStrategy(null);
        DBTableColumn columnWithNulls = new DBTableColumn();
        columnWithNulls.setName(null);
        columnWithNulls.setSchemaName(null);
        columnWithNulls.setTableName(null);
        List<DBTableColumn> columns = Arrays.asList(columnWithNulls);

        // When
        Map<String, ScanResult> results = noOpStrategy.scanBatch(columns, emptyRecognizers, emptyRecognizers);

        // Then
        Assert.assertEquals("Should return results for all columns", 1, results.size());
        Assert.assertTrue("Should contain key for unknown column",
                results.containsKey("unknown_schema.unknown_table.unknown_column"));
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
}
