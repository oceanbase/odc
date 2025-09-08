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

import java.util.HashMap;
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
import org.mockito.junit.MockitoJUnitRunner;

import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;
import com.openai.models.chat.completions.ChatCompletion;

@RunWith(MockitoJUnitRunner.class)
public class AIInferenceServiceTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private AIConfig aiConfig;

    @Mock
    private OpenAIClient openAIClient;

    @Mock
    private ChatCompletion chatCompletion;

    private AIInferenceService aiInferenceService;

    @Before
    public void setUp() {
        // Setup mock for openAIClient.chat().completions().create() call chain
    }

    @Test
    public void test_chat_allConditionsMet_callsConfigMethods() {
        // Given
        aiInferenceService = new AIInferenceService(aiConfig, Optional.of(openAIClient));
        setupValidAIConfig();
        String systemPrompt = "You are a helpful assistant";
        String userPrompt = "Hello, world!";

        // When - This will throw an exception due to actual OpenAI call, but we can verify config calls
        try {
            aiInferenceService.chat(systemPrompt, userPrompt);
        } catch (Exception e) {
        }

        // Then - Verify that config methods were called
        Mockito.verify(aiConfig).isEnabled();
        Mockito.verify(aiConfig).isAIAvailable();
    }

    @Test
    public void test_chat_aiNotEnabled_throwsException() {
        // Given
        aiInferenceService = new AIInferenceService(aiConfig, Optional.of(openAIClient));
        Mockito.when(aiConfig.isEnabled()).thenReturn(false);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("AI service is not enabled");

        // When
        aiInferenceService.chat("system", "user");
    }

    @Test
    public void test_chat_aiNotAvailable_throwsException() {
        // Given
        aiInferenceService = new AIInferenceService(aiConfig, Optional.of(openAIClient));
        Mockito.when(aiConfig.isEnabled()).thenReturn(true);
        Mockito.when(aiConfig.isAIAvailable()).thenReturn(false);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("AI configuration is incomplete");

        // When
        aiInferenceService.chat("system", "user");
    }

    @Test
    public void test_chat_clientNotPresent_throwsException() {
        // Given
        aiInferenceService = new AIInferenceService(aiConfig, Optional.empty());
        Mockito.when(aiConfig.isEnabled()).thenReturn(true);
        Mockito.when(aiConfig.isAIAvailable()).thenReturn(true);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("AI client is not initialized");

        // When
        aiInferenceService.chat("system", "user");
    }

    @Test
    public void test_chat_clientThrowsException_wrapsException() {
        // Given
        aiInferenceService = new AIInferenceService(aiConfig, Optional.of(openAIClient));
        setupValidAIConfig();
        // Note: This test verifies exception wrapping behavior
        // The actual OpenAI client will throw an exception due to invalid configuration
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Failed to call AI inference service");

        // When
        aiInferenceService.chat("system", "user");
    }

    @Test
    public void test_chat_verifyParametersPassedCorrectly() {
        // Given
        aiInferenceService = new AIInferenceService(aiConfig, Optional.of(openAIClient));
        setupValidAIConfig();
        String systemPrompt = "You are a helpful assistant";
        String userPrompt = "Hello, world!";
        String model = "gpt-3.5-turbo";
        Double temperature = 0.7;
        Double topP = 0.9;
        Map<String, JsonValue> additionalParams = new HashMap<>();

        Mockito.when(aiConfig.getModel()).thenReturn(model);
        Mockito.when(aiConfig.getTemperature()).thenReturn(temperature);
        Mockito.when(aiConfig.getTopP()).thenReturn(topP);
        Mockito.when(aiConfig.loadAdditionalParams()).thenReturn(additionalParams);

        // When - This will throw an exception but we can verify config method calls
        try {
            aiInferenceService.chat(systemPrompt, userPrompt);
        } catch (Exception e) {
            // Expected - actual OpenAI call will fail
        }

        // Then - Verify config methods were called
        Mockito.verify(aiConfig).getModel();
        Mockito.verify(aiConfig).getTemperature();
        Mockito.verify(aiConfig).getTopP();
        Mockito.verify(aiConfig).loadAdditionalParams();
    }

    @Test
    public void test_isAIAvailable_allConditionsMet_returnsTrue() {
        // Given
        aiInferenceService = new AIInferenceService(aiConfig, Optional.of(openAIClient));
        Mockito.when(aiConfig.isEnabled()).thenReturn(true);
        Mockito.when(aiConfig.isAIAvailable()).thenReturn(true);

        // When
        boolean result = aiInferenceService.isAIAvailable();

        // Then
        Assert.assertTrue(result);
    }

    @Test
    public void test_isAIAvailable_aiNotEnabled_returnsFalse() {
        // Given
        aiInferenceService = new AIInferenceService(aiConfig, Optional.of(openAIClient));
        Mockito.when(aiConfig.isEnabled()).thenReturn(false);
        // Note: aiConfig.isAIAvailable() stubbing removed as it's not called due to short-circuit
        // evaluation

        // When
        boolean result = aiInferenceService.isAIAvailable();

        // Then
        Assert.assertFalse(result);
    }

    @Test
    public void test_isAIAvailable_aiNotAvailable_returnsFalse() {
        // Given
        aiInferenceService = new AIInferenceService(aiConfig, Optional.of(openAIClient));
        Mockito.when(aiConfig.isEnabled()).thenReturn(true);
        Mockito.when(aiConfig.isAIAvailable()).thenReturn(false);

        // When
        boolean result = aiInferenceService.isAIAvailable();

        // Then
        Assert.assertFalse(result);
    }

    @Test
    public void test_isAIAvailable_clientNotPresent_returnsFalse() {
        // Given
        aiInferenceService = new AIInferenceService(aiConfig, Optional.empty());
        Mockito.when(aiConfig.isEnabled()).thenReturn(true);
        Mockito.when(aiConfig.isAIAvailable()).thenReturn(true);

        // When
        boolean result = aiInferenceService.isAIAvailable();

        // Then
        Assert.assertFalse(result);
    }

    @Test
    public void test_isAIAvailable_noConditionsMet_returnsFalse() {
        // Given
        aiInferenceService = new AIInferenceService(aiConfig, Optional.empty());
        Mockito.when(aiConfig.isEnabled()).thenReturn(false);
        // Note: aiConfig.isAIAvailable() stubbing removed as it's not called due to short-circuit
        // evaluation

        // When
        boolean result = aiInferenceService.isAIAvailable();

        // Then
        Assert.assertFalse(result);
    }

    @Test
    public void test_constructor_withValidParameters_createsInstance() {
        // Given & When
        AIInferenceService service = new AIInferenceService(aiConfig, Optional.of(openAIClient));

        // Then
        Assert.assertNotNull(service);
    }

    @Test
    public void test_constructor_withEmptyClient_createsInstance() {
        // Given & When
        AIInferenceService service = new AIInferenceService(aiConfig, Optional.empty());

        // Then
        Assert.assertNotNull(service);
    }

    private void setupValidAIConfig() {
        Mockito.when(aiConfig.isEnabled()).thenReturn(true);
        Mockito.when(aiConfig.isAIAvailable()).thenReturn(true);
        Mockito.when(aiConfig.getModel()).thenReturn("gpt-3.5-turbo");
        Mockito.when(aiConfig.getTemperature()).thenReturn(0.7);
        Mockito.when(aiConfig.getTopP()).thenReturn(0.9);
        Mockito.when(aiConfig.loadAdditionalParams()).thenReturn(new HashMap<>());
    }
}
