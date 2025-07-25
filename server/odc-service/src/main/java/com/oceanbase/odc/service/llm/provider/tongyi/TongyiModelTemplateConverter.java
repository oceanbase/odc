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

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.oceanbase.odc.service.llm.model.Constants;
import com.oceanbase.odc.service.llm.provider.template.ModelTemplate;
import com.oceanbase.odc.service.llm.provider.template.ModelTemplate.ParameterRule;
import com.oceanbase.odc.service.llm.util.YamlUtil;

public class TongyiModelTemplateConverter {

    public static TongyiModelCredential convertToTongyiCredential(ModelTemplate template) {
        TongyiModelCredential credential = new TongyiModelCredential();
        credential.setModelName(template.getModel());
        credential.setDeprecated(template.isDeprecated());
        credential.setModelType(template.getModelType());

        if (template.getFeatures() != null) {
            if (template.getFeatures().stream().anyMatch(f -> f.contains("tool-call"))) {
                credential.setFunctionCallingType(Constants.SUPPORT_FUNCTION_CALLING_TYPE);
            }
        }

        // 从模型属性中获取基础信息
        if (template.getModelProperties() != null) {
            credential.setContextSize(template.getModelProperties().getContextSize());
        }

        if (template.getParameterRules() != null) {
            // 创建参数规则名称到规则的映射
            Map<String, ParameterRule> parameterRuleMap = template.getParameterRules().stream()
                    .collect(Collectors.toMap(ParameterRule::getName, Function.identity()));

            // 根据参数规则设置默认值
            setParameterValue(credential, parameterRuleMap, "temperature",
                    (rule, cred) -> cred.setTemperature(YamlUtil.getFloatValue(rule.getDefaultValue())));

            setParameterValue(credential, parameterRuleMap, "max_tokens",
                    (rule, cred) -> cred.setMaxToken(YamlUtil.getIntegerValue(rule.getDefaultValue())));

            setParameterValue(credential, parameterRuleMap, "top_p",
                    (rule, cred) -> cred.setTopP(YamlUtil.getFloatValue(rule.getDefaultValue())));

            setParameterValue(credential, parameterRuleMap, "top_k",
                    (rule, cred) -> cred.setTopK(YamlUtil.getIntegerValue(rule.getDefaultValue())));

            setParameterValue(credential, parameterRuleMap, "seed",
                    (rule, cred) -> cred.setSeed(YamlUtil.getIntegerValue(rule.getDefaultValue())));

            setParameterValue(credential, parameterRuleMap, "repetition_penalty",
                    (rule, cred) -> cred.setRepetitionPenalty(YamlUtil.getFloatValue(rule.getDefaultValue())));

            setParameterValue(credential, parameterRuleMap, "enable_search",
                    (rule, cred) -> cred.setEnableSearch(YamlUtil.getBooleanValue(rule.getDefaultValue())));

            setParameterValue(credential, parameterRuleMap, "response_format",
                    (rule, cred) -> cred.setResponseFormat(YamlUtil.getStringValue(rule.getDefaultValue())));
        }

        return credential;
    }

    private static void setParameterValue(TongyiModelCredential credential,
            Map<String, ParameterRule> parameterRuleMap,
            String parameterName,
            ParameterSetter setter) {
        ParameterRule rule = parameterRuleMap.get(parameterName);
        if (rule != null && rule.getDefaultValue() != null) {
            setter.set(rule, credential);
        }
    }

    @FunctionalInterface
    private interface ParameterSetter {
        void set(ParameterRule rule, TongyiModelCredential credential);
    }

}
