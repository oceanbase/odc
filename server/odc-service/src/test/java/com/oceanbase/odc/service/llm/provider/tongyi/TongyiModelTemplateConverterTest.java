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
package com.oceanbase.odc.service.llm.provider.tongyi;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.service.llm.model.ModelType;
import com.oceanbase.odc.service.llm.provider.template.ModelTemplate;
import com.oceanbase.odc.service.llm.provider.template.ModelTemplate.ModelProperties;
import com.oceanbase.odc.service.llm.provider.template.ModelTemplate.ParameterRule;

/**
 * Test cases for {@link TongyiModelTemplateConverter}
 *
 * @author ODC_release_4.3.2
 */
public class TongyiModelTemplateConverterTest {

    @Test
    public void convertToTongyiCredential_FullTemplate_AllFieldsSet() {
        // given
        ModelTemplate template = createFullModelTemplate();

        // when
        TongyiModelCredential credential = TongyiModelTemplateConverter.convertToTongyiCredential(template);

        // then
        Assert.assertNotNull(credential);
        Assert.assertEquals("qwen-max", credential.getModelName());
        Assert.assertEquals(ModelType.CHAT, credential.getModelType());
        Assert.assertFalse(credential.isDeprecated());
        Assert.assertEquals(Integer.valueOf(32768), credential.getContextSize());
        Assert.assertEquals(Float.valueOf(0.7f), credential.getTemperature());
        Assert.assertEquals(Integer.valueOf(2048), credential.getMaxToken());
        Assert.assertEquals(Float.valueOf(0.8f), credential.getTopP());
        Assert.assertEquals(Integer.valueOf(50), credential.getTopK());
        Assert.assertEquals(Integer.valueOf(1234), credential.getSeed());
        Assert.assertEquals(Float.valueOf(1.1f), credential.getRepetitionPenalty());
        Assert.assertTrue(credential.getEnableSearch());
        Assert.assertEquals("json", credential.getResponseFormat());
    }

    @Test
    public void convertToTongyiCredential_MinimalTemplate_BasicFieldsSet() {
        // given
        ModelTemplate template = createMinimalModelTemplate();

        // when
        TongyiModelCredential credential = TongyiModelTemplateConverter.convertToTongyiCredential(template);

        // then
        Assert.assertNotNull(credential);
        Assert.assertEquals("qwen-turbo", credential.getModelName());
        Assert.assertEquals(ModelType.CHAT, credential.getModelType());
        Assert.assertTrue(credential.isDeprecated());
        Assert.assertEquals(Integer.valueOf(4096), credential.getContextSize());
        Assert.assertNull(credential.getTemperature());
        Assert.assertEquals(Integer.valueOf(4096), credential.getMaxToken());
        Assert.assertNull(credential.getTopP());
        Assert.assertNull(credential.getTopK());
        Assert.assertNull(credential.getSeed());
        Assert.assertNull(credential.getRepetitionPenalty());
        Assert.assertFalse(credential.getEnableSearch());
        Assert.assertNull(credential.getResponseFormat());
    }

    @Test
    public void convertToTongyiCredential_PartialParameters_OnlySetParametersConverted() {
        // given
        ModelTemplate template = createPartialParameterTemplate();

        // when
        TongyiModelCredential credential = TongyiModelTemplateConverter.convertToTongyiCredential(template);

        // then
        Assert.assertNotNull(credential);
        Assert.assertEquals("qwen-plus", credential.getModelName());
        Assert.assertEquals(Float.valueOf(0.3f), credential.getTemperature());
        Assert.assertEquals(Integer.valueOf(1024), credential.getMaxToken());
        Assert.assertNull(credential.getTopP());
        Assert.assertNull(credential.getTopK());
        Assert.assertNull(credential.getSeed());
        Assert.assertNull(credential.getRepetitionPenalty());
        Assert.assertFalse(credential.getEnableSearch());
        Assert.assertNull(credential.getResponseFormat());
    }

    @Test
    public void convertToTongyiCredential_StringNumberValues_ConvertedCorrectly() {
        // given
        ModelTemplate template = createStringNumberTemplate();

        // when
        TongyiModelCredential credential = TongyiModelTemplateConverter.convertToTongyiCredential(template);

        // then
        Assert.assertNotNull(credential);
        Assert.assertEquals(Float.valueOf(0.5f), credential.getTemperature());
        Assert.assertEquals(Integer.valueOf(512), credential.getMaxToken());
        Assert.assertEquals(Float.valueOf(0.9f), credential.getTopP());
        Assert.assertEquals(Integer.valueOf(25), credential.getTopK());
    }

    @Test
    public void convertToTongyiCredential_BooleanValues_ConvertedCorrectly() {
        // given
        ModelTemplate template = createBooleanTemplate();

        // when
        TongyiModelCredential credential = TongyiModelTemplateConverter.convertToTongyiCredential(template);

        // then
        Assert.assertNotNull(credential);
        Assert.assertFalse(credential.getEnableSearch());
    }

    @Test
    public void convertToTongyiCredential_NullDefaultValues_ParametersNotSet() {
        // given
        ModelTemplate template = createNullDefaultValueTemplate();

        // when
        TongyiModelCredential credential = TongyiModelTemplateConverter.convertToTongyiCredential(template);

        // then
        Assert.assertNotNull(credential);
        Assert.assertNull(credential.getTemperature());
        Assert.assertEquals(Integer.valueOf(4096), credential.getMaxToken());
        Assert.assertNull(credential.getTopP());
    }

    @Test
    public void convertToTongyiCredential_NullModelProperties_ContextSizeNotSet() {
        // given
        ModelTemplate template = createTemplateWithNullModelProperties();

        // when
        TongyiModelCredential credential = TongyiModelTemplateConverter.convertToTongyiCredential(template);

        // then
        Assert.assertNotNull(credential);
        Assert.assertEquals(Integer.valueOf(4096), credential.getContextSize());
    }

    @Test
    public void convertToTongyiCredential_EmptyParameterRules_NoParametersSet() {
        // given
        ModelTemplate template = createEmptyParameterRulesTemplate();

        // when
        TongyiModelCredential credential = TongyiModelTemplateConverter.convertToTongyiCredential(template);

        // then
        Assert.assertNotNull(credential);
        Assert.assertEquals("qwen-long", credential.getModelName());
        Assert.assertNull(credential.getTemperature());
        Assert.assertEquals(Integer.valueOf(4096), credential.getMaxToken());
        Assert.assertNull(credential.getTopP());
        Assert.assertNull(credential.getTopK());
        Assert.assertNull(credential.getSeed());
        Assert.assertNull(credential.getRepetitionPenalty());
        Assert.assertFalse(credential.getEnableSearch());
        Assert.assertNull(credential.getResponseFormat());
    }

    private ModelTemplate createFullModelTemplate() {
        ModelTemplate template = new ModelTemplate();
        template.setModel("qwen-max");
        template.setModelType(ModelType.CHAT);
        template.setDeprecated(false);

        ModelProperties properties = new ModelProperties();
        properties.setContextSize(32768);
        template.setModelProperties(properties);

        List<ParameterRule> parameterRules = new ArrayList<>();
        parameterRules.add(createParameterRule("temperature", 0.7f));
        parameterRules.add(createParameterRule("max_tokens", 2048));
        parameterRules.add(createParameterRule("top_p", 0.8f));
        parameterRules.add(createParameterRule("top_k", 50));
        parameterRules.add(createParameterRule("seed", 1234));
        parameterRules.add(createParameterRule("repetition_penalty", 1.1f));
        parameterRules.add(createParameterRule("enable_search", true));
        parameterRules.add(createParameterRule("response_format", "json"));
        template.setParameterRules(parameterRules);

        return template;
    }

    private ModelTemplate createMinimalModelTemplate() {
        ModelTemplate template = new ModelTemplate();
        template.setModel("qwen-turbo");
        template.setModelType(ModelType.CHAT);
        template.setDeprecated(true);
        template.setParameterRules(new ArrayList<>());
        return template;
    }

    private ModelTemplate createPartialParameterTemplate() {
        ModelTemplate template = new ModelTemplate();
        template.setModel("qwen-plus");
        template.setModelType(ModelType.CHAT);
        template.setDeprecated(false);

        List<ParameterRule> parameterRules = new ArrayList<>();
        parameterRules.add(createParameterRule("temperature", 0.3f));
        parameterRules.add(createParameterRule("max_tokens", 1024));
        template.setParameterRules(parameterRules);

        return template;
    }

    private ModelTemplate createStringNumberTemplate() {
        ModelTemplate template = new ModelTemplate();
        template.setModel("qwen-test");
        template.setModelType(ModelType.CHAT);
        template.setDeprecated(false);

        List<ParameterRule> parameterRules = new ArrayList<>();
        parameterRules.add(createParameterRule("temperature", "0.5"));
        parameterRules.add(createParameterRule("max_tokens", "512"));
        parameterRules.add(createParameterRule("top_p", "0.9"));
        parameterRules.add(createParameterRule("top_k", "25"));
        template.setParameterRules(parameterRules);

        return template;
    }

    private ModelTemplate createBooleanTemplate() {
        ModelTemplate template = new ModelTemplate();
        template.setModel("qwen-bool");
        template.setModelType(ModelType.CHAT);
        template.setDeprecated(false);

        List<ParameterRule> parameterRules = new ArrayList<>();
        parameterRules.add(createParameterRule("enable_search", false));
        template.setParameterRules(parameterRules);

        return template;
    }

    private ModelTemplate createNullDefaultValueTemplate() {
        ModelTemplate template = new ModelTemplate();
        template.setModel("qwen-null");
        template.setModelType(ModelType.CHAT);
        template.setDeprecated(false);

        List<ParameterRule> parameterRules = new ArrayList<>();
        parameterRules.add(createParameterRule("temperature", null));
        parameterRules.add(createParameterRule("max_tokens", null));
        parameterRules.add(createParameterRule("top_p", null));
        template.setParameterRules(parameterRules);

        return template;
    }

    private ModelTemplate createTemplateWithNullModelProperties() {
        ModelTemplate template = new ModelTemplate();
        template.setModel("qwen-no-props");
        template.setModelType(ModelType.CHAT);
        template.setDeprecated(false);
        template.setModelProperties(null);
        template.setParameterRules(new ArrayList<>());

        return template;
    }

    private ModelTemplate createEmptyParameterRulesTemplate() {
        ModelTemplate template = new ModelTemplate();
        template.setModel("qwen-long");
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
