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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.datasecurity.ai.AIInferenceService;
import com.oceanbase.odc.service.datasecurity.ai.PromptTemplateLoader;
import com.oceanbase.odc.service.datasecurity.model.RecognitionResult;
import com.oceanbase.odc.service.datasecurity.model.SensitiveLevel;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRule;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRuleType;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.openai.models.chat.completions.ChatCompletion;


@RunWith(MockitoJUnitRunner.class)
public class AIColumnRecognizerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private PromptTemplateLoader promptTemplateLoader;

    @Mock
    private AIInferenceService aiInferenceService;

    private AIColumnRecognizer recognizer;
    private SensitiveRule aiRule;

    @Before
    public void setUp() {
        aiRule = createAIRule();
        recognizer = new AIColumnRecognizer(aiRule);
    }

    @Test
    public void test_recognize_singleColumn_returnsSensitive() {
        // Given
        DBTableColumn column = createTestColumn("user_phone", "varchar", "user phone number");
        String systemPrompt = "System prompt for AI";
        String aiResponse =
                "```json\n[{\"sensitive\": true, \"riskLevel\": \"HIGH\", \"sensitiveCategory\": \"contact_info\"}]\n```";

        try (MockedStatic<SpringContextUtil> mockedSpringContext = Mockito.mockStatic(SpringContextUtil.class)) {
            setupMocks(mockedSpringContext, systemPrompt, aiResponse);

            // When
            Optional<RecognitionResult> result = recognizer.recognize(column);

            // Then
            Assert.assertTrue(result.isPresent());
            Assert.assertTrue(result.get().isMatched());
            Assert.assertEquals(SensitiveLevel.HIGH, result.get().getLevel());
            Assert.assertEquals("contact_info", result.get().getSensitiveType());
            Assert.assertEquals(SensitiveRuleType.AI, result.get().getSourceRuleType());
        }
    }

    @Test
    public void test_recognize_singleColumn_returnsNotSensitive() {
        // Given
        DBTableColumn column = createTestColumn("id", "bigint", "primary key");
        String systemPrompt = "System prompt for AI";
        String aiResponse = "```json\n[{\"sensitive\": false, \"riskLevel\": null, \"sensitiveCategory\": null}]\n```";

        try (MockedStatic<SpringContextUtil> mockedSpringContext = Mockito.mockStatic(SpringContextUtil.class)) {
            setupMocks(mockedSpringContext, systemPrompt, aiResponse);

            // When
            Optional<RecognitionResult> result = recognizer.recognize(column);

            // Then
            Assert.assertFalse(result.isPresent());
        }
    }

    @Test
    public void test_recognizeBatch_multipleColumns_returnsMixedResults() {
        // Given
        List<DBTableColumn> columns = Arrays.asList(
                createTestColumn("user_phone", "varchar", "user phone number"),
                createTestColumn("id", "bigint", "primary key"),
                createTestColumn("email", "varchar", "user email address"));
        String systemPrompt = "System prompt for AI";
        String aiResponse = "```json\n[" +
                "{\"sensitive\": true, \"riskLevel\": \"HIGH\", \"sensitiveCategory\": \"contact_info\"}," +
                "{\"sensitive\": false, \"riskLevel\": null, \"sensitiveCategory\": null}," +
                "{\"sensitive\": true, \"riskLevel\": \"MEDIUM\", \"sensitiveCategory\": \"contact_info\"}" +
                "]```";

        try (MockedStatic<SpringContextUtil> mockedSpringContext = Mockito.mockStatic(SpringContextUtil.class)) {
            setupMocks(mockedSpringContext, systemPrompt, aiResponse);

            // When
            Map<String, Optional<RecognitionResult>> results = recognizer.recognizeBatch(columns);

            // Then
            Assert.assertEquals(3, results.size());

            // Check phone column
            String phoneKey = getColumnKey(columns.get(0));
            Assert.assertTrue(results.get(phoneKey).isPresent());
            Assert.assertEquals(SensitiveLevel.HIGH, results.get(phoneKey).get().getLevel());

            // Check id column
            String idKey = getColumnKey(columns.get(1));
            Assert.assertFalse(results.get(idKey).isPresent());

            // Check email column
            String emailKey = getColumnKey(columns.get(2));
            Assert.assertTrue(results.get(emailKey).isPresent());
            Assert.assertEquals(SensitiveLevel.MEDIUM, results.get(emailKey).get().getLevel());
        }
    }

    @Test
    public void test_recognizeBatch_emptyList_returnsEmptyMap() {
        // When
        Map<String, Optional<RecognitionResult>> results = recognizer.recognizeBatch(Collections.emptyList());

        // Then
        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void test_recognizeBatch_nullList_returnsEmptyMap() {
        // When
        Map<String, Optional<RecognitionResult>> results = recognizer.recognizeBatch(null);

        // Then
        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void test_recognizeBatch_aiServiceThrowsException_returnsEmptyResults() {
        // Given
        List<DBTableColumn> columns = Arrays.asList(createTestColumn("test", "varchar", "test"));
        String systemPrompt = "System prompt for AI";

        try (MockedStatic<SpringContextUtil> mockedSpringContext = Mockito.mockStatic(SpringContextUtil.class)) {
            mockedSpringContext.when(() -> SpringContextUtil.getBean(PromptTemplateLoader.class))
                    .thenReturn(promptTemplateLoader);
            mockedSpringContext.when(() -> SpringContextUtil.getBean(AIInferenceService.class))
                    .thenReturn(aiInferenceService);

            Mockito.when(promptTemplateLoader.buildSystemPrompt(Mockito.anyList(), Mockito.anyString()))
                    .thenReturn(systemPrompt);
            Mockito.when(aiInferenceService.chat(Mockito.anyString(), Mockito.anyString()))
                    .thenThrow(new RuntimeException("AI service error"));

            // When
            Map<String, Optional<RecognitionResult>> results = recognizer.recognizeBatch(columns);

            // Then
            Assert.assertTrue(results.isEmpty());
        }
    }

    @Test
    public void test_recognizeBatch_invalidJsonResponse_throwsException() {
        // Given
        List<DBTableColumn> columns = Arrays.asList(createTestColumn("test", "varchar", "test"));
        String systemPrompt = "System prompt for AI";
        String invalidResponse = "This is not a valid JSON response";

        try (MockedStatic<SpringContextUtil> mockedSpringContext = Mockito.mockStatic(SpringContextUtil.class)) {
            setupMocks(mockedSpringContext, systemPrompt, invalidResponse);

            thrown.expect(BadRequestException.class);
            thrown.expectMessage("AI response does not contain valid JSON format");

            // When
            recognizer.recognizeBatch(columns);
        }
    }

    @Test
    public void test_recognizeBatch_malformedJsonResponse_throwsException() {
        // Given
        List<DBTableColumn> columns = Arrays.asList(createTestColumn("test", "varchar", "test"));
        String systemPrompt = "System prompt for AI";
        String malformedResponse =
                "```json\n[{\"sensitive\": true, \"riskLevel\": \"HIGH\" missing_comma \"field\": \"value\"}]```"; // Invalid
                                                                                                                   // JSON
                                                                                                                   // syntax

        try (MockedStatic<SpringContextUtil> mockedSpringContext = Mockito.mockStatic(SpringContextUtil.class)) {
            setupMocks(mockedSpringContext, systemPrompt, malformedResponse);

            thrown.expect(BadRequestException.class);
            thrown.expectMessage("Failed to parse AI response JSON");

            // When
            recognizer.recognizeBatch(columns);
        }
    }

    @Test
    public void test_recognizeBatch_responseWithoutJsonWrapper_parsesCorrectly() {
        // Given
        DBTableColumn column = createTestColumn("user_phone", "varchar", "user phone number");
        String systemPrompt = "System prompt for AI";
        String aiResponse = "[{\"sensitive\": true, \"riskLevel\": \"HIGH\", \"sensitiveCategory\": \"contact_info\"}]"; // No
                                                                                                                         // ```json
                                                                                                                         // wrapper

        try (MockedStatic<SpringContextUtil> mockedSpringContext = Mockito.mockStatic(SpringContextUtil.class)) {
            setupMocks(mockedSpringContext, systemPrompt, aiResponse);

            // When
            Optional<RecognitionResult> result = recognizer.recognize(column);

            // Then
            Assert.assertTrue(result.isPresent());
            Assert.assertTrue(result.get().isMatched());
            Assert.assertEquals(SensitiveLevel.HIGH, result.get().getLevel());
        }
    }

    @Test
    public void test_recognizeBatch_mismatchedResponseCount_handlesGracefully() {
        // Given
        List<DBTableColumn> columns = Arrays.asList(
                createTestColumn("col1", "varchar", "test1"),
                createTestColumn("col2", "varchar", "test2"),
                createTestColumn("col3", "varchar", "test3"));
        String systemPrompt = "System prompt for AI";
        // AI returns only 2 results for 3 columns
        String aiResponse = "```json\n[" +
                "{\"sensitive\": true, \"riskLevel\": \"HIGH\", \"sensitiveCategory\": \"contact_info\"}," +
                "{\"sensitive\": false, \"riskLevel\": null, \"sensitiveCategory\": null}" +
                "]```";

        try (MockedStatic<SpringContextUtil> mockedSpringContext = Mockito.mockStatic(SpringContextUtil.class)) {
            setupMocks(mockedSpringContext, systemPrompt, aiResponse);

            // When
            Map<String, Optional<RecognitionResult>> results = recognizer.recognizeBatch(columns);

            // Then
            Assert.assertEquals(2, results.size()); // Only 2 results processed

            String col1Key = getColumnKey(columns.get(0));
            String col2Key = getColumnKey(columns.get(1));
            String col3Key = getColumnKey(columns.get(2));

            Assert.assertTrue(results.containsKey(col1Key));
            Assert.assertTrue(results.containsKey(col2Key));
            Assert.assertFalse(results.containsKey(col3Key)); // Third column not processed
        }
    }

    // Helper methods
    private void setupMocks(MockedStatic<SpringContextUtil> mockedSpringContext, String systemPrompt,
            String aiResponse) {
        mockedSpringContext.when(() -> SpringContextUtil.getBean(PromptTemplateLoader.class))
                .thenReturn(promptTemplateLoader);
        mockedSpringContext.when(() -> SpringContextUtil.getBean(AIInferenceService.class))
                .thenReturn(aiInferenceService);

        Mockito.when(promptTemplateLoader.buildSystemPrompt(Mockito.anyList(), Mockito.anyString()))
                .thenReturn(systemPrompt);

        // Mock the chain: completion.choices().get(0).message().content().orElse("[]")
        // Use Mockito's deep stubbing with RETURNS_DEEP_STUBS
        ChatCompletion mockCompletion = Mockito.mock(ChatCompletion.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(aiInferenceService.chat(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(mockCompletion);
        Mockito.when(mockCompletion.choices().get(0).message().content())
                .thenReturn(Optional.of(aiResponse));
    }

    private DBTableColumn createTestColumn(String columnName, String typeName, String comment) {
        DBTableColumn column = new DBTableColumn();
        column.setSchemaName("test_schema");
        column.setTableName("test_table");
        column.setName(columnName);
        column.setTypeName(typeName);
        column.setComment(comment);
        return column;
    }

    private SensitiveRule createAIRule() {
        SensitiveRule rule = new SensitiveRule();
        rule.setId(1L);
        rule.setType(SensitiveRuleType.AI);
        rule.setLevel(SensitiveLevel.HIGH);
        rule.setEnabled(true);
        rule.setAiSensitiveTypes(Arrays.asList("contact_info", "identity_info"));
        rule.setAiCustomPrompt("Custom AI prompt for testing");
        return rule;
    }

    private String getColumnKey(DBTableColumn column) {
        return column.getSchemaName() + "." + column.getTableName() + "." + column.getName();
    }
}
