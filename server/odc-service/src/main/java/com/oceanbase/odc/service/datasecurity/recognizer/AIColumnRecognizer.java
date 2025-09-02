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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.oceanbase.odc.service.common.util.SpringContextUtil;

import lombok.extern.slf4j.Slf4j;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.service.datasecurity.ai.AIInferenceService;
import com.oceanbase.odc.service.datasecurity.ai.AIParam;
import com.oceanbase.odc.service.datasecurity.ai.PromptTemplateLoader;
import com.oceanbase.odc.service.datasecurity.model.RecognitionResult;
import com.oceanbase.odc.service.datasecurity.model.SensitiveLevel;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRule;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRuleType;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.openai.models.chat.completions.ChatCompletion;
import lombok.Data;

/**
 * AI Column Recognizer
 */
@Slf4j
public class AIColumnRecognizer implements ColumnRecognizer {

    private final SensitiveRule aiRule; // 直接保存整个规则对象
    private static final int BATCH_SIZE = AIParam.DEFAULT_BATCH_SIZE_IN_TABLE; // 单表内列数超过此值时进行分批处理
    private static final ObjectMapper objectMapper = new ObjectMapper(); // 用于解析JSON
    private static final Pattern JSON_PATTERN = Pattern
        .compile("(?s)```json\\s*([\\{\\[].*[\\}\\]])\\s*```|([\\{\\[].*[\\}\\]])");

    public AIColumnRecognizer(SensitiveRule rule) {
        this.aiRule = rule;
    }

    @Override
    public Optional<RecognitionResult> recognize(DBTableColumn column) {
        // 通过调用批量识别方法来实现单个识别
        Map<String, Optional<RecognitionResult>> batchResult = recognizeBatch(Collections.singletonList(column));
        String columnKey = getColumnKey(column);
        return batchResult.getOrDefault(columnKey, Optional.empty());
    }

    @Override
    public Map<String, Optional<RecognitionResult>> recognizeBatch(List<DBTableColumn> columns) {
        if (columns == null || columns.isEmpty()) {
            return Collections.emptyMap();
        }
        // 1. 获取依赖的服务
        PromptTemplateLoader promptTemplateLoader = SpringContextUtil.getBean(PromptTemplateLoader.class);
        AIInferenceService aiService = SpringContextUtil.getBean(AIInferenceService.class);

        Map<String, Optional<RecognitionResult>> finalAiResults = new HashMap<>();

        // 2. 如果列数超过批次大小，则分批处理；否则直接处理
        if (columns.size() > BATCH_SIZE) {
            List<List<DBTableColumn>> batches = Lists.partition(columns, BATCH_SIZE);
            try {
                // 3. 遍历每一个小批次，分别调用 AI
                for (List<DBTableColumn> batch : batches) {
                    processBatch(batch, promptTemplateLoader, aiService, finalAiResults);
                }
            } catch (BadRequestException e) {
                // 重新抛出BadRequestException，让调用方处理
                throw e;
            } catch (Exception e) {
                log.error("Failed to process AI column recognition batch", e);
                return finalAiResults;
            }
        } else {
            // 直接处理单批次
            try {
                processBatch(columns, promptTemplateLoader, aiService, finalAiResults);
            } catch (BadRequestException e) {
                // 重新抛出BadRequestException，让调用方处理
                throw e;
            } catch (Exception e) {
                log.error("Failed to process AI column recognition", e);
                return finalAiResults;
            }
        }
        return finalAiResults;
    }

    /**
     * Process a single batch of column data
     */
    private void processBatch(List<DBTableColumn> batch, PromptTemplateLoader promptTemplateLoader,
        AIInferenceService aiService, Map<String, Optional<RecognitionResult>> finalAiResults) throws IOException {
        // a. 构建系统提示词
        String systemPrompt = promptTemplateLoader.buildSystemPrompt(aiRule.getAiSensitiveTypes(),
            aiRule.getAiCustomPrompt());

        // b. 构建用户提示词（列数据的JSON数组）
        String userPrompt = buildUserPrompt(batch);

        // c. 调用 AI
        ChatCompletion completion = aiService.chat(systemPrompt, userPrompt);
        String rawContent = completion.choices().get(0).message().content().orElse("[]");

        // c. 使用正则表达式从AI的返回结果中安全地提取JSON数组字符串
        Matcher matcher = JSON_PATTERN.matcher(rawContent);
        String jsonArrayResponse = null;
        if (matcher.find()) {
            // group(1) 对应被 ```json [...] ``` 包裹的内容, group(2) 对应裸露的 [...]
            // 使用 Optional 来优雅地处理可能为null的捕获组
            jsonArrayResponse = Optional.ofNullable(matcher.group(1)).orElse(matcher.group(2));
        }

        if (jsonArrayResponse == null) {
            throw new BadRequestException(ErrorCodes.AIResponseFormatError,
                new Object[] { "No valid JSON array found in AI response" },
                "AI response does not contain valid JSON format: " + rawContent);
        }

        // d. 解析提取出的、更纯净的 JSON 数组
        List<AiResponseDto> batchResults;
        try {
            batchResults = objectMapper.readValue(jsonArrayResponse,
                new TypeReference<List<AiResponseDto>>() {
                });
        } catch (Exception e) {
            throw new BadRequestException(ErrorCodes.AIResponseFormatError,
                new Object[] { "Failed to parse JSON: " + e.getMessage() },
                "Failed to parse AI response JSON: " + jsonArrayResponse, e);
        }

        // d. 将这批次的结果存入最终的 map，添加边界检查防止数组越界
        int maxIndex = Math.min(batch.size(), batchResults.size());
        for (int i = 0; i < maxIndex; i++) {
            DBTableColumn column = batch.get(i);
            String columnKey = getColumnKey(column);
            AiResponseDto dto = batchResults.get(i);

            if (dto.isSensitive()) {
                RecognitionResult result = RecognitionResult.builder()
                    .matched(true)
                    .matchedRuleId(this.aiRule.getId())
                    .level(dto.getRiskLevel())
                    .sourceRuleType(SensitiveRuleType.AI)
                    .sensitiveType(dto.getSensitiveCategory())
                    .build();
                finalAiResults.put(columnKey, Optional.of(result));
            } else {
                finalAiResults.put(columnKey, Optional.empty());
            }
        }

        // Log warning if AI response count doesn't match input count
        if (batchResults.size() != batch.size()) {
            log.warn("AI response count ({}) does not match input column count ({})",
                batchResults.size(), batch.size());
        }
    }

    /**
     * Build user prompt (JSON array of column data)
     */
    private String buildUserPrompt(List<DBTableColumn> batch) throws IOException {
        // 将一批列的元数据转换为 JSON 数组字符串
        List<Map<String, String>> columnMetadataList = batch.stream().map(c -> {
            Map<String, String> meta = new HashMap<>();
            meta.put("schemaName", c.getSchemaName());
            meta.put("tableName", c.getTableName());
            meta.put("columnName", c.getName());
            meta.put("comment", c.getComment());
            meta.put("dataType", c.getTypeName());
            return meta;
        }).collect(Collectors.toList());
        return objectMapper.writeValueAsString(columnMetadataList);
    }

    // Inner class for holding AI response JSON data
    @Data
    private static class AiResponseDto {
        private boolean sensitive;
        private SensitiveLevel riskLevel;
        private String sensitiveCategory;
    }
}