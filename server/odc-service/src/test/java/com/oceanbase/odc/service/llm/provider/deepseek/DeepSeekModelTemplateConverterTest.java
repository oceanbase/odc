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
package com.oceanbase.odc.service.llm.provider.deepseek;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.service.llm.model.ModelType;
import com.oceanbase.odc.service.llm.provider.template.ModelTemplate;
import com.oceanbase.odc.service.llm.provider.template.ModelTemplate.ModelProperties;
import com.oceanbase.odc.service.llm.provider.template.ModelTemplate.ParameterRule;

/**
 * Test cases for {@link DeepSeekModelTemplateConverter}
 *
 * @author liuyizhuo.lyz
 */
public class DeepSeekModelTemplateConverterTest {

    @Test
    public void convertToDeepSeekCredential_FullTemplate_AllFieldsSet() {
        // given
        ModelTemplate template = createFullModelTemplate();

        // when
        DeepSeekModelCredential credential = DeepSeekModelTemplateConverter.convertToDeepSeekCredential(template);

        // then
        Assert.assertNotNull(credential);
        Assert.assertEquals("deepseek-chat", credential.getModelName());
        Assert.assertEquals(ModelType.CHAT, credential.getModelType());
        Assert.assertFalse(credential.isDeprecated());
        Assert.assertEquals(Integer.valueOf(128000), credential.getContextSize());
        Assert.assertEquals(Float.valueOf(0.8f), credential.getTemperature());
        Assert.assertEquals(Integer.valueOf(4096), credential.getMaxToken());
        Assert.assertEquals(Float.valueOf(0.9f), credential.getTopP());
        Assert.assertTrue(credential.getLogprobs());
        Assert.assertEquals(Integer.valueOf(5), credential.getTopLogprobs());
        Assert.assertEquals(Float.valueOf(0.5f), credential.getFrequencyPenalty());
        Assert.assertEquals("json_object", credential.getResponseFormat());
    }

    @Test
    public void convertToDeepSeekCredential_MinimalTemplate_BasicFieldsSet() {
        // given
        ModelTemplate template = createMinimalModelTemplate();

        // when
        DeepSeekModelCredential credential = DeepSeekModelTemplateConverter.convertToDeepSeekCredential(template);

        // then
        Assert.assertNotNull(credential);
        Assert.assertEquals("deepseek-coder", credential.getModelName());
        Assert.assertEquals(ModelType.CHAT, credential.getModelType());
        Assert.assertTrue(credential.isDeprecated());
        Assert.assertNull(credential.getContextSize());
        Assert.assertNull(credential.getTemperature());
        Assert.assertNull(credential.getMaxToken());
        Assert.assertNull(credential.getTopP());
        Assert.assertNull(credential.getLogprobs());
        Assert.assertNull(credential.getTopLogprobs());
        Assert.assertNull(credential.getFrequencyPenalty());
        Assert.assertNull(credential.getResponseFormat());
    }

    @Test
    public void convertToDeepSeekCredential_PartialParameters_OnlySetParametersConverted() {
        // given
        ModelTemplate template = createPartialParameterTemplate();

        // when
        DeepSeekModelCredential credential = DeepSeekModelTemplateConverter.convertToDeepSeekCredential(template);

        // then
        Assert.assertNotNull(credential);
        Assert.assertEquals("deepseek-v2", credential.getModelName());
        Assert.assertEquals(Float.valueOf(0.7f), credential.getTemperature());
        Assert.assertEquals(Integer.valueOf(2048), credential.getMaxToken());
        Assert.assertNull(credential.getTopP());
        Assert.assertNull(credential.getLogprobs());
        Assert.assertNull(credential.getTopLogprobs());
        Assert.assertNull(credential.getFrequencyPenalty());
        Assert.assertNull(credential.getResponseFormat());
    }

    @Test
    public void convertToDeepSeekCredential_StringNumberValues_ConvertedCorrectly() {
        // given
        ModelTemplate template = createStringNumberTemplate();

        // when
        DeepSeekModelCredential credential = DeepSeekModelTemplateConverter.convertToDeepSeekCredential(template);

        // then
        Assert.assertNotNull(credential);
        Assert.assertEquals(Float.valueOf(1.2f), credential.getTemperature());
        Assert.assertEquals(Integer.valueOf(1024), credential.getMaxToken());
        Assert.assertEquals(Float.valueOf(0.85f), credential.getTopP());
        Assert.assertEquals(Integer.valueOf(10), credential.getTopLogprobs());
        Assert.assertEquals(Float.valueOf(-0.5f), credential.getFrequencyPenalty());
    }

    @Test
    public void convertToDeepSeekCredential_BooleanValues_ConvertedCorrectly() {
        // given
        ModelTemplate template = createBooleanTemplate();

        // when
        DeepSeekModelCredential credential = DeepSeekModelTemplateConverter.convertToDeepSeekCredential(template);

        // then
        Assert.assertNotNull(credential);
        Assert.assertFalse(credential.getLogprobs());
    }

    @Test
    public void convertToDeepSeekCredential_NullDefaultValues_ParametersNotSet() {
        // given
        ModelTemplate template = createNullDefaultValueTemplate();

        // when
        DeepSeekModelCredential credential = DeepSeekModelTemplateConverter.convertToDeepSeekCredential(template);

        // then
        Assert.assertNotNull(credential);
        Assert.assertNull(credential.getTemperature());
        Assert.assertNull(credential.getMaxToken());
        Assert.assertNull(credential.getTopP());
        Assert.assertNull(credential.getFrequencyPenalty());
    }

    @Test
    public void convertToDeepSeekCredential_NullModelProperties_ContextSizeNotSet() {
        // given
        ModelTemplate template = createTemplateWithNullModelProperties();

        // when
        DeepSeekModelCredential credential = DeepSeekModelTemplateConverter.convertToDeepSeekCredential(template);

        // then
        Assert.assertNotNull(credential);
        Assert.assertNull(credential.getContextSize());
    }

    @Test
    public void convertToDeepSeekCredential_EmptyParameterRules_NoParametersSet() {
        // given
        ModelTemplate template = createEmptyParameterRulesTemplate();

        // when
        DeepSeekModelCredential credential = DeepSeekModelTemplateConverter.convertToDeepSeekCredential(template);

        // then
        Assert.assertNotNull(credential);
        Assert.assertEquals("deepseek-v3", credential.getModelName());
        Assert.assertNull(credential.getTemperature());
        Assert.assertNull(credential.getMaxToken());
        Assert.assertNull(credential.getTopP());
        Assert.assertNull(credential.getLogprobs());
        Assert.assertNull(credential.getTopLogprobs());
        Assert.assertNull(credential.getFrequencyPenalty());
        Assert.assertNull(credential.getResponseFormat());
    }

    private ModelTemplate createFullModelTemplate() {
        ModelTemplate template = new ModelTemplate();
        template.setModel("deepseek-chat");
        template.setModelType(ModelType.CHAT);
        template.setDeprecated(false);

        ModelProperties properties = new ModelProperties();
        properties.setContextSize(128000);
        template.setModelProperties(properties);

        List<ParameterRule> parameterRules = new ArrayList<>();
        parameterRules.add(createParameterRule("temperature", 0.8f));
        parameterRules.add(createParameterRule("max_tokens", 4096));
        parameterRules.add(createParameterRule("top_p", 0.9f));
        parameterRules.add(createParameterRule("logprobs", true));
        parameterRules.add(createParameterRule("top_logprobs", 5));
        parameterRules.add(createParameterRule("frequency_penalty", 0.5f));
        parameterRules.add(createParameterRule("response_format", "json_object"));
        template.setParameterRules(parameterRules);

        return template;
    }

    private ModelTemplate createMinimalModelTemplate() {
        ModelTemplate template = new ModelTemplate();
        template.setModel("deepseek-coder");
        template.setModelType(ModelType.CHAT);
        template.setDeprecated(true);
        template.setParameterRules(new ArrayList<>());
        return template;
    }

    private ModelTemplate createPartialParameterTemplate() {
        ModelTemplate template = new ModelTemplate();
        template.setModel("deepseek-v2");
        template.setModelType(ModelType.CHAT);
        template.setDeprecated(false);

        List<ParameterRule> parameterRules = new ArrayList<>();
        parameterRules.add(createParameterRule("temperature", 0.7f));
        parameterRules.add(createParameterRule("max_tokens", 2048));
        template.setParameterRules(parameterRules);

        return template;
    }

    private ModelTemplate createStringNumberTemplate() {
        ModelTemplate template = new ModelTemplate();
        template.setModel("deepseek-test");
        template.setModelType(ModelType.CHAT);
        template.setDeprecated(false);

        List<ParameterRule> parameterRules = new ArrayList<>();
        parameterRules.add(createParameterRule("temperature", "1.2"));
        parameterRules.add(createParameterRule("max_tokens", "1024"));
        parameterRules.add(createParameterRule("top_p", "0.85"));
        parameterRules.add(createParameterRule("top_logprobs", "10"));
        parameterRules.add(createParameterRule("frequency_penalty", "-0.5"));
        template.setParameterRules(parameterRules);

        return template;
    }

    private ModelTemplate createBooleanTemplate() {
        ModelTemplate template = new ModelTemplate();
        template.setModel("deepseek-bool");
        template.setModelType(ModelType.CHAT);
        template.setDeprecated(false);

        List<ParameterRule> parameterRules = new ArrayList<>();
        parameterRules.add(createParameterRule("logprobs", false));
        template.setParameterRules(parameterRules);

        return template;
    }

    private ModelTemplate createNullDefaultValueTemplate() {
        ModelTemplate template = new ModelTemplate();
        template.setModel("deepseek-null");
        template.setModelType(ModelType.CHAT);
        template.setDeprecated(false);

        List<ParameterRule> parameterRules = new ArrayList<>();
        parameterRules.add(createParameterRule("temperature", null));
        parameterRules.add(createParameterRule("max_tokens", null));
        parameterRules.add(createParameterRule("top_p", null));
        parameterRules.add(createParameterRule("frequency_penalty", null));
        template.setParameterRules(parameterRules);

        return template;
    }

    private ModelTemplate createTemplateWithNullModelProperties() {
        ModelTemplate template = new ModelTemplate();
        template.setModel("deepseek-no-props");
        template.setModelType(ModelType.CHAT);
        template.setDeprecated(false);
        template.setModelProperties(null);
        template.setParameterRules(new ArrayList<>());

        return template;
    }

    private ModelTemplate createEmptyParameterRulesTemplate() {
        ModelTemplate template = new ModelTemplate();
        template.setModel("deepseek-v3");
        template.setModelType(ModelType.CHAT);
        template.setDeprecated(false);
        template.setParameterRules(new ArrayList<>());

        return template;
    }

    private ParameterRule createParameterRule(String name, Object defaultValue) {
        ParameterRule rule = new ParameterRule();
        rule.setName(name);
        rule.setDefaultValue(defaultValue);
        return rule;
    }
}
