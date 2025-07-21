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

    private static final String TEMPLATE_PATH = "/ai-prompt-templete/sensitive_column_recognize_prompt_templete.txt";

    // 定义新的三个占位符
    private static final String COLUMN_PLACEHOLDER = "{DBTableColumn}";
    private static final String TYPES_PLACEHOLDER = "{sensitiveTypes}";
    private static final String PROMPT_PLACEHOLDER = "{customPrompt}";

    private String template;

    @PostConstruct
    public void init() {
        try (var inputStream = PromptTemplateLoader.class.getResourceAsStream(TEMPLATE_PATH)) {
            if (Objects.isNull(inputStream)) {
                throw new IllegalStateException("AI prompt template file not found: " + TEMPLATE_PATH);
            }
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream))) {
                this.template = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        } catch (Exception e) {
            // 在实际项目中，这里应该使用日志系统
            e.printStackTrace();
            throw new IllegalStateException("Failed to load AI prompt template", e);
        }
    }

    /**
     * 【新】根据列元数据、敏感类型列表和用户提示，构建最终的 AI 提示词。
     *
     * @param column         数据库表列的元数据对象
     * @param sensitiveTypes 用户指定的敏感类型列表 (例如 ["联系方式", "身份信息"])
     * @param customPrompt   用户为该规则自定义的补充说明提示
     * @return 填充了所有信息的完整提示词字符串
     */
    public String buildPrompt(DBTableColumn column, List<String> sensitiveTypes, String customPrompt) {
        if (this.template == null || this.template.isEmpty()) {
            throw new IllegalStateException("Prompt template is not available. Check loading status.");
        }
        if (column == null) {
            throw new IllegalArgumentException("Input column cannot be null.");
        }

        // 1. 格式化列元数据
        String formattedColumn = formatColumnMetadata(column);

        // 2. 格式化敏感类型列表
        String formattedTypes = (sensitiveTypes == null || sensitiveTypes.isEmpty())
                ? "None specified"
                : String.join(", ", sensitiveTypes);

        // 3. 格式化用户自定义提示
        String formattedPrompt = (customPrompt == null || customPrompt.trim().isEmpty())
                ? "None"
                : customPrompt;

        // 4. 依次替换模板中的三个占位符
        return this.template
                .replace(COLUMN_PLACEHOLDER, formattedColumn)
                .replace(TYPES_PLACEHOLDER, formattedTypes)
                .replace(PROMPT_PLACEHOLDER, formattedPrompt);
    }

    /**
     * 根据列元数据列表、敏感类型列表和用户提示，构建批量处理的 AI 提示词
     *
     * @param columnsJson    数据库表列的元数据对象列表的JSON字符串
     * @param sensitiveTypes 用户指定的敏感类型列表 (例如 ["联系方式", "身份信息"])
     * @param customPrompt   用户为该规则自定义的补充说明提示
     * @return 填充了所有信息的完整提示词字符串
     */
    public String buildPrompt(String columnsJson, List<String> sensitiveTypes, String customPrompt) {
        if (this.template == null || this.template.isEmpty()) {
            throw new IllegalStateException("Prompt template is not available. Check loading status.");
        }
        if (columnsJson == null) {
            throw new IllegalArgumentException("Input columnsJson cannot be null.");
        }

        // 1. 格式化敏感类型列表
        String formattedTypes = (sensitiveTypes == null || sensitiveTypes.isEmpty())
                ? "None specified"
                : String.join(", ", sensitiveTypes);

        // 2. 格式化用户自定义提示
        String formattedPrompt = (customPrompt == null || customPrompt.trim().isEmpty())
                ? "None"
                : customPrompt;

        // 3. 替换模板中的占位符
        return this.template
                .replace(COLUMN_PLACEHOLDER, columnsJson)
                .replace(TYPES_PLACEHOLDER, formattedTypes)
                .replace(PROMPT_PLACEHOLDER, formattedPrompt);
    }

    /**
     * 将列的元数据格式化为对 AI 模型友好的字符串。
     * (此方法逻辑不变)
     */
    private String formatColumnMetadata(DBTableColumn column) {
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("Schema Name: ").append(formatValue(column.getSchemaName())).append("\n");
        contextBuilder.append("Table Name: ").append(formatValue(column.getTableName())).append("\n");
        contextBuilder.append("Column Name: ").append(formatValue(column.getName())).append("\n");
        contextBuilder.append("Data Type: ").append(formatValue(column.getTypeName())).append("\n");
        contextBuilder.append("Column Comment: ").append(formatValue(column.getComment()));
        return contextBuilder.toString();
    }

    private String formatValue(String value) {
        return (value == null || value.trim().isEmpty()) ? "N/A" : value;
    }

}