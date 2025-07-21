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
package com.oceanbase.odc.service.datasecurity.recognizer;

import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.datasecurity.ai.AIInferenceService;
import com.oceanbase.odc.service.datasecurity.ai.PromptTemplateLoader;
import com.oceanbase.odc.service.datasecurity.model.RecognitionResult;
import com.oceanbase.odc.service.datasecurity.model.SensitiveLevel;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRule;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRuleType;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.openai.models.chat.completions.ChatCompletion;

/**
 * AI列识别器单元测试类
 */
public class AIColumnRecognizerTest {

    @Mock
    private PromptTemplateLoader promptTemplateLoader;

    @Mock
    private AIInferenceService aiInferenceService;

    @Mock
    private SpringContextUtil springContextUtil;

    private SensitiveRule aiRule;
    private AIColumnRecognizer aiColumnRecognizer;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        
        // Mock SpringContextUtil
        mockStatic(SpringContextUtil.class);
        when(SpringContextUtil.getBean(PromptTemplateLoader.class)).thenReturn(promptTemplateLoader);
        when(SpringContextUtil.getBean(AIInferenceService.class)).thenReturn(aiInferenceService);
        
        // 创建测试用的AI规则
        aiRule = createTestAIRule(1L);
        aiColumnRecognizer = new AIColumnRecognizer(aiRule);
    }

    @Test
    public void recognize_singleColumn_returnEmpty() {
        // 准备阶段：创建测试列
        DBTableColumn column = createTestColumn("test_db", "users", "email", "用户邮箱");

        // 执行阶段：调用单列识别方法
        Optional<RecognitionResult> result = aiColumnRecognizer.recognize(column);

        // 验证阶段：确认单列识别返回空结果
        Assert.assertFalse("单列识别应返回空结果", result.isPresent());
    }

    @Test
    public void recognizeBatch_emptyList_returnEmptyMap() {
        // 执行阶段：调用批量识别方法，传入空列表
        Map<String, RecognitionResult> result = aiColumnRecognizer.recognizeBatch(Collections.emptyList());

        // 验证阶段：确认返回空Map
        Assert.assertNotNull("结果不应为null", result);
        Assert.assertTrue("空列表应返回空Map", result.isEmpty());
    }

    @Test
    public void recognizeBatch_nullList_returnEmptyMap() {
        // 执行阶段：调用批量识别方法，传入null
        Map<String, RecognitionResult> result = aiColumnRecognizer.recognizeBatch(null);

        // 验证阶段：确认返回空Map
        Assert.assertNotNull("结果不应为null", result);
        Assert.assertTrue("null列表应返回空Map", result.isEmpty());
    }

    @Test
    public void recognizeBatch_singleColumn_returnResult() throws Exception {
        // 准备阶段：创建测试数据和模拟AI服务响应
        DBTableColumn column = createTestColumn("test_db", "users", "email", "用户邮箱");
        List<DBTableColumn> columns = Arrays.asList(column);
        
        String prompt = "test prompt";
        when(promptTemplateLoader.buildPrompt(anyString(), anyList(), anyString())).thenReturn(prompt);
        
        String aiResponse = "[{\"sensitive\": true, \"riskLevel\": \"HIGH\", \"confidence\": 95, \"sensitiveType\": \"联系方式\"}]";
        ChatCompletion chatCompletion = mock(ChatCompletion.class);
        when(chatCompletion.choices()).thenReturn(Arrays.asList(mock(com.openai.models.chat.completions.ChatCompletion.Choice.class)));
        when(chatCompletion.choices().get(0).message()).thenReturn(mock(com.openai.models.chat.completions.ChatCompletionMessage.class));
        when(chatCompletion.choices().get(0).message().content()).thenReturn(Optional.of(aiResponse));
        when(aiInferenceService.chat(prompt)).thenReturn(chatCompletion);

        // 执行阶段：调用批量识别方法
        Map<String, RecognitionResult> result = aiColumnRecognizer.recognizeBatch(columns);

        // 验证阶段：确认返回正确的识别结果
        Assert.assertNotNull("结果不应为null", result);
        Assert.assertEquals("应返回一个识别结果", 1, result.size());
        Assert.assertTrue("应包含指定列的结果", result.containsKey("users.email"));
        
        RecognitionResult recognitionResult = result.get("users.email");
        Assert.assertTrue("应识别为敏感列", recognitionResult.isMatched());
        Assert.assertEquals("规则ID应匹配", aiRule.getId(), recognitionResult.getMatchedRuleId());
        Assert.assertEquals("规则类型应为AI", SensitiveRuleType.AI, recognitionResult.getSourceRuleType());
        Assert.assertEquals("风险等级应为HIGH", SensitiveLevel.HIGH, recognitionResult.getLevel());
        Assert.assertEquals("置信度应为95", Double.valueOf(95), recognitionResult.getConfidence());
        Assert.assertEquals("敏感类型应为联系方式", "联系方式", recognitionResult.getSensitiveType());
    }

    @Test
    public void recognizeBatch_multipleColumns_returnResults() throws Exception {
        // 准备阶段：创建多个测试列和模拟AI服务响应
        DBTableColumn column1 = createTestColumn("test_db", "users", "email", "用户邮箱");
        DBTableColumn column2 = createTestColumn("test_db", "employees", "salary", "员工薪资");
        DBTableColumn column3 = createTestColumn("test_db", "products", "name", "产品名称");
        List<DBTableColumn> columns = Arrays.asList(column1, column2, column3);
        
        String prompt = "test prompt";
        when(promptTemplateLoader.buildPrompt(anyString(), anyList(), anyString())).thenReturn(prompt);
        
        // 模拟AI响应：前两列为敏感列，第三列（name）为非敏感列
        String aiResponse = "["
                + "{\"sensitive\": true, \"riskLevel\": \"HIGH\", \"confidence\": 95, \"sensitiveType\": \"联系方式\"},"
                + "{\"sensitive\": true, \"riskLevel\": \"HIGH\", \"confidence\": 90, \"sensitiveType\": \"财务信息\"},"
                + "{\"sensitive\": false, \"riskLevel\": \"LOW\", \"confidence\": 20, \"sensitiveType\": null}"
                + "]";
        ChatCompletion chatCompletion = mock(ChatCompletion.class);
        when(chatCompletion.choices()).thenReturn(Arrays.asList(mock(com.openai.models.chat.completions.ChatCompletion.Choice.class)));
        when(chatCompletion.choices().get(0).message()).thenReturn(mock(com.openai.models.chat.completions.ChatCompletionMessage.class));
        when(chatCompletion.choices().get(0).message().content()).thenReturn(Optional.of(aiResponse));
        when(aiInferenceService.chat(prompt)).thenReturn(chatCompletion);

        // 执行阶段：调用批量识别方法
        Map<String, RecognitionResult> result = aiColumnRecognizer.recognizeBatch(columns);

        // 验证阶段：确认返回正确的识别结果数量（只有敏感列会被返回）
        Assert.assertNotNull("结果不应为null", result);
        Assert.assertEquals("应返回两个识别结果（只有敏感列会被返回）", 2, result.size());
        Assert.assertTrue("应包含users.email的结果", result.containsKey("users.email"));
        Assert.assertTrue("应包含employees.salary的结果", result.containsKey("employees.salary"));
        Assert.assertFalse("不应包含products.name的结果（非敏感列）", result.containsKey("products.name"));
        
        // 验证email列的识别结果
        RecognitionResult emailResult = result.get("users.email");
        Assert.assertTrue("email应识别为敏感列", emailResult.isMatched());
        Assert.assertEquals("email敏感类型应为联系方式", "联系方式", emailResult.getSensitiveType());
        
        // 验证salary列的识别结果
        RecognitionResult salaryResult = result.get("employees.salary");
        Assert.assertTrue("salary应识别为敏感列", salaryResult.isMatched());
        Assert.assertEquals("salary敏感类型应为财务信息", "财务信息", salaryResult.getSensitiveType());
    }

    // --- 辅助方法 ---

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