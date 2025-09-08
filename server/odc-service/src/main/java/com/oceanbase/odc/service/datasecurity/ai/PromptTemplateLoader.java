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

import lombok.extern.slf4j.Slf4j;
import lombok.var;

/**
 * @author fenyf
 * @date 2025/8/10 12:41
 */
@Slf4j
@Component
public class PromptTemplateLoader {
    private static final String SYSTEM_TEMPLATE_PATH =
            "/ai-prompt-template/sensitive_column_recognize_system_prompt.txt";
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
            log.error("Failed to load AI system prompt template: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to load AI system prompt template", e);
        }
    }

    public String buildSystemPrompt(List<String> sensitiveTypes, String customPrompt) {
        if (this.systemTemplate == null || this.systemTemplate.isEmpty()) {
            throw new IllegalStateException("System prompt template is not available. Check loading status.");
        }

        String formattedTypes = (sensitiveTypes == null || sensitiveTypes.isEmpty())
                ? "No specified category."
                : String.join(", ", sensitiveTypes);

        String formattedPrompt = (customPrompt == null || customPrompt.trim().isEmpty())
                ? "No supplementary rule."
                : customPrompt;

        return this.systemTemplate
                .replace(TYPES_PLACEHOLDER, formattedTypes)
                .replace(PROMPT_PLACEHOLDER, formattedPrompt);
    }
}
