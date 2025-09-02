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
package com.oceanbase.odc.service.datasecurity.ai;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class PromptTemplateLoaderTest {

    private PromptTemplateLoader promptTemplateLoader;
    private static final String MOCK_TEMPLATE = "Sensitive types: {sensitiveTypes}\nCustom prompt: {customPrompt}";

    @Before
    public void setUp() {
        promptTemplateLoader = new PromptTemplateLoader();
        // Set mock template to avoid file loading issues in test
        ReflectionTestUtils.setField(promptTemplateLoader, "systemTemplate", MOCK_TEMPLATE);
    }

    @Test
    public void test_buildSystemPrompt_withValidSensitiveTypesAndCustomPrompt_returnsFormattedPrompt() {
        // Given
        List<String> sensitiveTypes = Arrays.asList("contact_info", "identity_info");
        String customPrompt = "Additional rules for identification";

        // When
        String result = promptTemplateLoader.buildSystemPrompt(sensitiveTypes, customPrompt);

        // Then
        Assert.assertNotNull("Result should not be null", result);
        Assert.assertTrue("Should contain formatted sensitive types",
            result.contains("contact_info, identity_info"));
        Assert.assertTrue("Should contain custom prompt",
            result.contains("Additional rules for identification"));
    }

    @Test
    public void test_buildSystemPrompt_withEmptySensitiveTypes_returnsDefaultMessage() {
        // Given
        List<String> sensitiveTypes = Collections.emptyList();
        String customPrompt = "Custom rules";

        // When
        String result = promptTemplateLoader.buildSystemPrompt(sensitiveTypes, customPrompt);

        // Then
        Assert.assertNotNull("Result should not be null", result);
        Assert.assertTrue("Should contain default message for empty types",
            result.contains("No specified category."));
        Assert.assertTrue("Should contain custom prompt", result.contains("Custom rules"));
    }

    @Test
    public void test_buildSystemPrompt_withNullSensitiveTypes_returnsDefaultMessage() {
        // Given
        List<String> sensitiveTypes = null;
        String customPrompt = "Custom rules";

        // When
        String result = promptTemplateLoader.buildSystemPrompt(sensitiveTypes, customPrompt);

        // Then
        Assert.assertNotNull("Result should not be null", result);
        Assert.assertTrue("Should contain default message for null types",
            result.contains("No specified category."));
        Assert.assertTrue("Should contain custom prompt", result.contains("Custom rules"));
    }

    @Test
    public void test_buildSystemPrompt_withEmptyCustomPrompt_returnsDefaultMessage() {
        // Given
        List<String> sensitiveTypes = Arrays.asList("email", "phone");
        String customPrompt = "";

        // When
        String result = promptTemplateLoader.buildSystemPrompt(sensitiveTypes, customPrompt);

        // Then
        Assert.assertNotNull("Result should not be null", result);
        Assert.assertTrue("Should contain formatted sensitive types",
            result.contains("email, phone"));
        Assert.assertTrue("Should contain default message for empty prompt",
            result.contains("No supplementary rule."));
    }

    @Test
    public void test_buildSystemPrompt_withNullCustomPrompt_returnsDefaultMessage() {
        // Given
        List<String> sensitiveTypes = Arrays.asList("address");
        String customPrompt = null;

        // When
        String result = promptTemplateLoader.buildSystemPrompt(sensitiveTypes, customPrompt);

        // Then
        Assert.assertNotNull("Result should not be null", result);
        Assert.assertTrue("Should contain formatted sensitive types", result.contains("address"));
        Assert.assertTrue("Should contain default message for null prompt",
            result.contains("No supplementary rule."));
    }

    @Test
    public void test_buildSystemPrompt_withWhitespaceCustomPrompt_returnsDefaultMessage() {
        // Given
        List<String> sensitiveTypes = Arrays.asList("name");
        String customPrompt = "   \t\n   ";

        // When
        String result = promptTemplateLoader.buildSystemPrompt(sensitiveTypes, customPrompt);

        // Then
        Assert.assertNotNull("Result should not be null", result);
        Assert.assertTrue("Should contain formatted sensitive types", result.contains("name"));
        Assert.assertTrue("Should contain default message for whitespace prompt",
            result.contains("No supplementary rule."));
    }

    @Test
    public void test_buildSystemPrompt_withSingleSensitiveType_returnsCorrectFormat() {
        // Given
        List<String> sensitiveTypes = Arrays.asList("credit_card");
        String customPrompt = "Strict validation required";

        // When
        String result = promptTemplateLoader.buildSystemPrompt(sensitiveTypes, customPrompt);

        // Then
        Assert.assertNotNull("Result should not be null", result);
        Assert.assertTrue("Should contain single sensitive type", result.contains("credit_card"));
        Assert.assertFalse("Should not contain comma for single type",
            result.contains("credit_card,"));
        Assert.assertTrue("Should contain custom prompt",
            result.contains("Strict validation required"));
    }

    @Test(expected = IllegalStateException.class)
    public void test_buildSystemPrompt_withNullTemplate_throwsException() {
        // Given
        ReflectionTestUtils.setField(promptTemplateLoader, "systemTemplate", null);
        List<String> sensitiveTypes = Arrays.asList("test");
        String customPrompt = "test";

        // When
        promptTemplateLoader.buildSystemPrompt(sensitiveTypes, customPrompt);

        // Then - exception should be thrown
    }

    @Test(expected = IllegalStateException.class)
    public void test_buildSystemPrompt_withEmptyTemplate_throwsException() {
        // Given
        ReflectionTestUtils.setField(promptTemplateLoader, "systemTemplate", "");
        List<String> sensitiveTypes = Arrays.asList("test");
        String customPrompt = "test";

        // When
        promptTemplateLoader.buildSystemPrompt(sensitiveTypes, customPrompt);

        // Then - exception should be thrown
    }

    @Test
    public void test_buildSystemPrompt_withMultipleSensitiveTypes_returnsCommaSeparated() {
        // Given
        List<String> sensitiveTypes = Arrays.asList("email", "phone", "address", "name");
        String customPrompt = "Multiple type validation";

        // When
        String result = promptTemplateLoader.buildSystemPrompt(sensitiveTypes, customPrompt);

        // Then
        Assert.assertNotNull("Result should not be null", result);
        Assert.assertTrue("Should contain all types comma-separated",
            result.contains("email, phone, address, name"));
        Assert.assertTrue("Should contain custom prompt",
            result.contains("Multiple type validation"));
    }

    @Test
    public void test_buildSystemPrompt_preservesOriginalTemplate_afterMultipleCalls() {
        // Given
        List<String> sensitiveTypes1 = Arrays.asList("type1");
        List<String> sensitiveTypes2 = Arrays.asList("type2");
        String customPrompt1 = "prompt1";
        String customPrompt2 = "prompt2";

        // When
        String result1 = promptTemplateLoader.buildSystemPrompt(sensitiveTypes1, customPrompt1);
        String result2 = promptTemplateLoader.buildSystemPrompt(sensitiveTypes2, customPrompt2);

        // Then
        Assert.assertNotNull("First result should not be null", result1);
        Assert.assertNotNull("Second result should not be null", result2);
        Assert.assertTrue("First result should contain type1", result1.contains("type1"));
        Assert.assertTrue("Second result should contain type2", result2.contains("type2"));
        Assert.assertFalse("First result should not contain type2", result1.contains("type2"));
        Assert.assertFalse("Second result should not contain type1", result2.contains("type1"));
    }
}