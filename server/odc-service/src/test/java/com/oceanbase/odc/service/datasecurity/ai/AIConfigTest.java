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
package com.oceanbase.odc.service.datasecurity.ai;

import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;

@RunWith(MockitoJUnitRunner.class)
public class AIConfigTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private AIConfig aiConfig;

    @Before
    public void setUp() {
        aiConfig = new AIConfig();
    }

    @Test
    public void test_isAIAvailable_enabledAndApiKeySet_returnsTrue() {
        // Given
        ReflectionTestUtils.setField(aiConfig, "enabled", true);
        ReflectionTestUtils.setField(aiConfig, "apiKey", "test-api-key");

        // When
        boolean result = aiConfig.isAIAvailable();

        // Then
        Assert.assertTrue(result);
    }

    @Test
    public void test_isAIAvailable_disabledWithApiKey_returnsFalse() {
        // Given
        ReflectionTestUtils.setField(aiConfig, "enabled", false);
        ReflectionTestUtils.setField(aiConfig, "apiKey", "test-api-key");

        // When
        boolean result = aiConfig.isAIAvailable();

        // Then
        Assert.assertFalse(result);
    }

    @Test
    public void test_isAIAvailable_enabledWithoutApiKey_returnsFalse() {
        // Given
        ReflectionTestUtils.setField(aiConfig, "enabled", true);
        ReflectionTestUtils.setField(aiConfig, "apiKey", "");

        // When
        boolean result = aiConfig.isAIAvailable();

        // Then
        Assert.assertFalse(result);
    }

    @Test
    public void test_isAIAvailable_enabledWithNullApiKey_returnsFalse() {
        // Given
        ReflectionTestUtils.setField(aiConfig, "enabled", true);
        ReflectionTestUtils.setField(aiConfig, "apiKey", null);

        // When
        boolean result = aiConfig.isAIAvailable();

        // Then
        Assert.assertFalse(result);
    }

    @Test
    public void test_isAIAvailable_enabledWithWhitespaceApiKey_returnsFalse() {
        // Given
        ReflectionTestUtils.setField(aiConfig, "enabled", true);
        ReflectionTestUtils.setField(aiConfig, "apiKey", "   ");

        // When
        boolean result = aiConfig.isAIAvailable();

        // Then
        Assert.assertFalse(result);
    }

    @Test
    public void test_loadAdditionalParams_defaultValues_returnsCorrectMap() {
        // Given - using default values

        // When
        Map<String, JsonValue> params = aiConfig.loadAdditionalParams();

        // Then
        Assert.assertNotNull(params);
        Assert.assertEquals(3, params.size());
        Assert.assertTrue(params.containsKey("enable_thinking"));
        Assert.assertTrue(params.containsKey("top_k"));
        Assert.assertTrue(params.containsKey("min_p"));
    }

    @Test
    public void test_loadAdditionalParams_customValues_returnsCorrectMap() {
        // Given
        ReflectionTestUtils.setField(aiConfig, "enableThinking", false);
        ReflectionTestUtils.setField(aiConfig, "topK", 50);
        ReflectionTestUtils.setField(aiConfig, "minP", 10);

        // When
        Map<String, JsonValue> params = aiConfig.loadAdditionalParams();

        // Then
        Assert.assertNotNull(params);
        Assert.assertEquals(3, params.size());
        Assert.assertTrue(params.containsKey("enable_thinking"));
        Assert.assertTrue(params.containsKey("top_k"));
        Assert.assertTrue(params.containsKey("min_p"));
    }

    @Test
    public void test_openAIClient_validApiKey_returnsClient() {
        // Given
        ReflectionTestUtils.setField(aiConfig, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(aiConfig, "baseUrl", "https://api.openai.com");

        // When
        OpenAIClient client = aiConfig.openAIClient();

        // Then
        Assert.assertNotNull(client);
    }

    @Test
    public void test_openAIClient_nullApiKey_throwsException() {
        // Given
        ReflectionTestUtils.setField(aiConfig, "apiKey", null);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("API key is not configured");

        // When
        aiConfig.openAIClient();
    }

    @Test
    public void test_openAIClient_emptyApiKey_throwsException() {
        // Given
        ReflectionTestUtils.setField(aiConfig, "apiKey", "");
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("API key is not configured");

        // When
        aiConfig.openAIClient();
    }

    @Test
    public void test_openAIClient_whitespaceApiKey_throwsException() {
        // Given
        ReflectionTestUtils.setField(aiConfig, "apiKey", "   ");
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("API key is not configured");

        // When
        aiConfig.openAIClient();
    }

    @Test
    public void test_gettersAndSetters_workCorrectly() {
        // Test enabled
        aiConfig.setEnabled(true);
        Assert.assertTrue(aiConfig.isEnabled());

        // Test apiKey
        aiConfig.setApiKey("test-key");
        Assert.assertEquals("test-key", aiConfig.getApiKey());

        // Test baseUrl
        aiConfig.setBaseUrl("https://test.com");
        Assert.assertEquals("https://test.com", aiConfig.getBaseUrl());

        // Test model
        aiConfig.setModel("gpt-4");
        Assert.assertEquals("gpt-4", aiConfig.getModel());

        // Test temperature
        aiConfig.setTemperature(0.8);
        Assert.assertEquals(Double.valueOf(0.8), aiConfig.getTemperature());

        // Test topP
        aiConfig.setTopP(0.9);
        Assert.assertEquals(Double.valueOf(0.9), aiConfig.getTopP());

        // Test enableThinking
        aiConfig.setEnableThinking(false);
        Assert.assertFalse(aiConfig.getEnableThinking());

        // Test topK
        aiConfig.setTopK(100);
        Assert.assertEquals(Integer.valueOf(100), aiConfig.getTopK());

        // Test minP
        aiConfig.setMinP(20);
        Assert.assertEquals(Integer.valueOf(20), aiConfig.getMinP());
    }
}
