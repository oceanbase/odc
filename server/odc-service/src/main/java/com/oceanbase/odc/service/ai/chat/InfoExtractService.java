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
package com.oceanbase.odc.service.ai.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.ai.Constants;
import com.oceanbase.odc.service.ai.chat.model.ChatProperties;
import com.oceanbase.odc.service.ai.chat.model.ExtractionInfo;
import com.oceanbase.odc.service.ai.knowledgebase.utils.ChatUtils;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.llm.sdk.StreamingChatModelWrapper;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/8/4
 */
@Service
@Slf4j
public class InfoExtractService {

    private static String JSON_REGEX = "\\{[^\\}]+\\}";
    private static Pattern JSON_PATTERN = Pattern.compile(JSON_REGEX);
    @Autowired
    private ChatProperties chatProperties;
    @Autowired
    private DatabaseService databaseService;

    public ExtractionInfo extractInfoFromQuery(String query, StreamingChatModelWrapper chatModel) {
        String llmResponse = chatModel.chat(ChatUtils.buildChatMessages(Constants.EXTRACT_PROMPT, query));
        log.info("extract info = {}", llmResponse);
        Matcher matcher = JSON_PATTERN.matcher(llmResponse);
        if (matcher.find()) {
            llmResponse = matcher.group(0);
        }
        ExtractionInfo extractInfo = JsonUtils.fromJson(llmResponse, ExtractionInfo.class);
        if (extractInfo == null) {
            return new ExtractionInfo();
        }
        String skeleton = extractSkeleton(query, new ArrayList<String>() {
            {
                if (extractInfo.getColumnNames() != null) {
                    addAll(extractInfo.getColumnNames());
                }
                if (extractInfo.getTableNames() != null) {
                    addAll(extractInfo.getTableNames());
                }
            }
        });
        extractInfo.setQuestionSkeleton(skeleton);
        return extractInfo;
    }

    private String extractSkeleton(String userInput, List<String> keywords) {
        for (String keyword : keywords) {
            if (StringUtils.isNotBlank(keyword)) {
                userInput = StringUtils.replace(userInput, keyword, "_");
            }
        }
        return userInput;
    }
}
