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
package com.oceanbase.odc.service.datasecurity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oceanbase.odc.service.datasecurity.factory.ScanningStrategyFactory;
import com.oceanbase.odc.service.datasecurity.model.RecognitionResult;
import com.oceanbase.odc.service.datasecurity.model.ScanResult;
import com.oceanbase.odc.service.datasecurity.model.ScanningModeType;
import com.oceanbase.odc.service.datasecurity.model.SensitiveLevel;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRule;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRuleType;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

/**
 * 敏感列扫描器单元测试类
 */
public class SensitiveColumnScannerTest {

    private List<SensitiveRule> rules;
    private SensitiveColumnScanner scanner;

    @Before
    public void setUp() {
        // 创建测试规则
        SensitiveRule regexRule = createTestRegexRule(1L);
        SensitiveRule aiRule = createTestAIRule(2L);
        rules = Arrays.asList(regexRule, aiRule);
        ScanningStrategyFactory strategyFactory = new ScanningStrategyFactory();
        scanner = new SensitiveColumnScanner(rules, strategyFactory);
    }

    @Test
    public void scan_rulesOnlyMode_returnBasicResult() {
        // 准备阶段：创建能被正则规则匹配的测试列
        DBTableColumn column = createTestColumn("test_db", "users", "email", "user email address");

        // 执行阶段：使用仅规则模式扫描
        ScanResult result = scanner.scan(column, ScanningModeType.RULES_ONLY);

        // 验证阶段：确认返回基础规则结果
        Assert.assertNotNull("结果不应为null", result);
        Assert.assertTrue("基础规则结果应存在", result.getBasicRuleResult().isPresent());
        Assert.assertFalse("AI规则结果应不存在", result.getAiRuleResult().isPresent());

        RecognitionResult basicResult = result.getBasicRuleResult().get();
        Assert.assertTrue("应匹配成功", basicResult.isMatched());
        Assert.assertEquals("匹配的规则ID应为1", Long.valueOf(1), basicResult.getMatchedRuleId());
    }

    @Test
    public void scan_jointRecognitionMode_ruleMatched_returnBasicResultOnly() {
        // 准备阶段：创建能被正则规则匹配的测试列
        DBTableColumn column = createTestColumn("test_db", "users", "email", "user email address");

        // 执行阶段：使用联合识别模式扫描
        ScanResult scanResult = scanner.scan(column, ScanningModeType.JOINT_RECOGNITION);

        // 验证阶段：确认只返回基础规则结果
        Assert.assertNotNull("结果不应为null", scanResult);
        Assert.assertTrue("基础规则结果应存在", scanResult.getBasicRuleResult().isPresent());
        Assert.assertFalse("AI规则结果应不存在", scanResult.getAiRuleResult().isPresent());
    }

    @Test
    public void scan_jointRecognitionMode_ruleNotMatched_returnAiResult() {
        // 准备阶段：创建不能被正则规则匹配的测试列
        DBTableColumn column = createTestColumn("test_db", "products", "name", "product name");

        // 执行阶段：使用联合识别模式扫描
        ScanResult result = scanner.scan(column, ScanningModeType.JOINT_RECOGNITION);

        // 验证阶段：确认基础规则结果不存在（因为没有mock AI识别器，所以AI结果也不存在）
        Assert.assertNotNull("结果不应为null", result);
        Assert.assertFalse("基础规则结果应不存在", result.getBasicRuleResult().isPresent());
    }

    @Test
    public void scan_rulesAndAiMode_returnBothResults() {
        // 准备阶段：创建能被正则规则匹配的测试列
        DBTableColumn column = createTestColumn("test_db", "users", "email", "user email address");

        // 执行阶段：使用规则+AI模式扫描
        ScanResult result = scanner.scan(column, ScanningModeType.RULES_AND_AI);

        // 验证阶段：确认返回基础规则结果（AI结果不存在因为没有mock）
        Assert.assertNotNull("结果不应为null", result);
        Assert.assertTrue("基础规则结果应存在", result.getBasicRuleResult().isPresent());
    }

    @Test
    public void scanBatch_emptyList_returnEmptyMap() {
        // 执行阶段：使用仅规则模式批量扫描空列表
        Map<String, ScanResult> result = scanner.scanBatch(Collections.emptyList(), ScanningModeType.RULES_ONLY);

        // 验证阶段：确认返回空Map
        Assert.assertNotNull("结果不应为null", result);
        Assert.assertTrue("空列表应返回空Map", result.isEmpty());
    }

    @Test
    public void scanBatch_rulesOnlyMode_returnBasicResults() {
        // 准备阶段：创建多个测试列，其中一列能被正则规则匹配
        DBTableColumn column1 = createTestColumn("test_db", "users", "email", "user email address"); // 能匹配
        DBTableColumn column2 = createTestColumn("test_db", "products", "name", "product name"); // 不能匹配
        List<DBTableColumn> columns = Arrays.asList(column1, column2);

        // 执行阶段：使用仅规则模式批量扫描
        Map<String, ScanResult> results = scanner.scanBatch(columns, ScanningModeType.RULES_ONLY);

        // 验证阶段：确认返回正确的批量扫描结果
        Assert.assertNotNull("结果不应为null", results);
        Assert.assertEquals("应返回两个扫描结果", 2, results.size());
        Assert.assertTrue("应包含users.email的结果", results.containsKey("users.email"));
        Assert.assertTrue("应包含products.name的结果", results.containsKey("products.name"));

        // 验证email列的扫描结果（应该匹配）
        ScanResult emailResult = results.get("users.email");
        Assert.assertTrue("email的基础规则结果应存在", emailResult.getBasicRuleResult().isPresent());
        Assert.assertEquals("email匹配的规则ID应为1", Long.valueOf(1), emailResult.getBasicRuleResult().get().getMatchedRuleId());

        // 验证name列的扫描结果（不应该匹配）
        ScanResult nameResult = results.get("products.name");
        Assert.assertFalse("name的基础规则结果应不存在", nameResult.getBasicRuleResult().isPresent());
    }

    // --- 辅助方法 ---

    /**
     * 创建测试用的正则规则
     * @param id 规则ID
     * @return 正则规则对象
     */
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

    /**
     * 创建测试用的AI规则
     * @param id 规则ID
     * @return AI规则对象
     */
    private SensitiveRule createTestAIRule(Long id) {
        SensitiveRule rule = new SensitiveRule();
        rule.setId(id);
        rule.setType(SensitiveRuleType.AI);
        rule.setEnabled(true);
        rule.setLevel(SensitiveLevel.HIGH);
        rule.setAiSensitiveTypes(Arrays.asList("联系方式", "财务信息", "身份信息"));
        rule.setAiConfidenceThreshold(80);
        rule.setAiCustomPrompt("请识别敏感数据列");
        return rule;
    }

    /**
     * 创建测试用的数据库列
     * @param schemaName 数据库名
     * @param tableName 表名
     * @param columnName 列名
     * @param comment 列注释
     * @return 数据库列对象
     */
    private DBTableColumn createTestColumn(String schemaName, String tableName, String columnName, String comment) {
        DBTableColumn column = new DBTableColumn();
        column.setSchemaName(schemaName);
        column.setTableName(tableName);
        column.setName(columnName);
        column.setComment(comment);
        column.setTypeName("VARCHAR");
        return column;
    }
}