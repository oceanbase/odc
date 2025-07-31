/*
 * Copyright (c) 2025 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oceanbase.odc.service.datasecurity.ai;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

import lombok.var;

/**
 * AI 提示词 (Prompt) 模板加载器和构建器（重构版）。
 * <p>
 * 该类负责加载结构化的 AI 提示词模板，并根据列元数据、指定的敏感类型和用户自定义提示来构建最终的提示词。
 * </p>
 */
@Component
public class PromptTemplateLoader {

    private static final String SYSTEM_TEMPLATE_PATH = "/ai-prompt-template/sensitive_column_recognize_system_prompt.txt";

    // 定义占位符
    private static final String TYPES_PLACEHOLDER = "{sensitiveTypes}";
    private static final String PROMPT_PLACEHOLDER = "{customPrompt}";

    private String systemTemplate;

    @PostConstruct
    public void init() {
        try (var inputStream = PromptTemplateLoader.class.getResourceAsStream(SYSTEM_TEMPLATE_PATH)) {
            if (Objects.isNull(inputStream)) {
                throw new IllegalStateException("AI system prompt template file not found: " + SYSTEM_TEMPLATE_PATH);
            }
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream))) {
                this.systemTemplate = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        } catch (Exception e) {
            // 在实际项目中，这里应该使用日志系统
            e.printStackTrace();
            throw new IllegalStateException("Failed to load AI system prompt template", e);
        }
    }

    /**
     * 构建系统提示词
     *
     * @param sensitiveTypes 用户指定的敏感类型列表 (例如 ["联系方式", "身份信息"])
     * @param customPrompt   用户为该规则自定义的补充说明提示
     * @return 填充了敏感类型和自定义提示的系统提示词字符串
     */
    public String buildSystemPrompt(List<String> sensitiveTypes, String customPrompt) {
        if (this.systemTemplate == null || this.systemTemplate.isEmpty()) {
            throw new IllegalStateException("System prompt template is not available. Check loading status.");
        }

        // 1. 格式化敏感类型列表
        String formattedTypes = (sensitiveTypes == null || sensitiveTypes.isEmpty())
            ? "No specified category."
            : String.join(", ", sensitiveTypes);

        // 2. 格式化用户自定义提示
        String formattedPrompt = (customPrompt == null || customPrompt.trim().isEmpty())
            ? "No supplementary rule."
            : customPrompt;

        // 3. 替换模板中的占位符
        return this.systemTemplate
            .replace(TYPES_PLACEHOLDER, formattedTypes)
            .replace(PROMPT_PLACEHOLDER, formattedPrompt);
    }
}