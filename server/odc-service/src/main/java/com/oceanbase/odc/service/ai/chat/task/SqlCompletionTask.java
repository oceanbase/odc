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
package com.oceanbase.odc.service.ai.chat.task;

import java.text.MessageFormat;
import java.util.concurrent.Callable;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.oceanbase.odc.service.ai.Constants;
import com.oceanbase.odc.service.ai.chat.DatabaseCacheService;
import com.oceanbase.odc.service.ai.chat.model.Chat;
import com.oceanbase.odc.service.ai.chat.model.ChatProperties;
import com.oceanbase.odc.service.ai.chat.model.ChatStatus;
import com.oceanbase.odc.service.ai.chat.model.SqlCopilotReq;
import com.oceanbase.odc.service.ai.knowledgebase.utils.ChatUtils;
import com.oceanbase.odc.service.llm.sdk.StreamingChatModelWrapper;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/8/4
 */
@Slf4j
@Builder
public class SqlCompletionTask implements Callable<String> {
    private final SseEmitter sseEmitter;
    private final StreamingChatModelWrapper chatModel;
    private final SqlCopilotReq req;
    private final ChatProperties chatProperties;
    private final String grammar;
    private final DatabaseCacheService databaseCacheService;

    @Override
    public String call() throws Exception {
        try {
            String input = req.getFileContent();
            int cursorPos = req.getCursorPosition();
            int maxLines = chatProperties.getSessionContextMaxLines();
            String preCursorInput = input.substring(0, cursorPos);
            String delimiter = req.getDelimiter();
            if (delimiter == null || delimiter.isEmpty()) {
                delimiter = ";";
            }
            if (preCursorInput.endsWith(delimiter)) {
                if (sseEmitter != null) {
                    sseEmitter.complete();
                }
                return null;
            }
            String[] preCursorLines = preCursorInput.split("\n");
            StringBuilder nearCursorInputBuilder = new StringBuilder();
            for (int i = Math.max(0, preCursorLines.length - maxLines); i < preCursorLines.length; i++) {
                if (preCursorLines[i].length() > chatProperties.getSessionContextMaxLengthPerLine()) {
                    continue;
                }
                nearCursorInputBuilder.append(preCursorLines[i]);
                if (i != preCursorLines.length - 1) {
                    nearCursorInputBuilder.append("\n");
                }
            }
            nearCursorInputBuilder.append(Constants.CURSOR);
            String[] afterCursorLines = input.substring(cursorPos).split("\n");
            for (int i = 0; i < Math.min(afterCursorLines.length, maxLines); i++) {
                if (afterCursorLines[i].length() > chatProperties.getSessionContextMaxLengthPerLine()) {
                    continue;
                }
                nearCursorInputBuilder.append(afterCursorLines[i]).append("\n");
            }
            String nearCursorInput = nearCursorInputBuilder.toString();
            // 将相关表的DDL拼入上下文
            String ddlText = "";
            try {
                ddlText = databaseCacheService.getSplicedTableDDLText(req.getDatabaseId(), nearCursorInput,
                        req.getSid());
            } catch (Exception e) {
                log.warn("Failed to get table ddl text", e);
            }
            StringBuilder contextBuilder = new StringBuilder(ddlText);
            contextBuilder.append(nearCursorInput);
            String prompt = MessageFormat.format(Constants.COMPLETE_SQL_PROMPT, grammar);
            // 将上下文交给大模型自动补全
            String sql = chatModel.chat(ChatUtils.buildChatMessages(prompt, contextBuilder.toString()));
            sseEmitter.send(new Chat(ChatUtils.removeSurroundCharacter(sql), ChatStatus.COMPLETED));
            sseEmitter.complete();
            return sql;
        } catch (Exception e) {
            log.error("Failed to complete sql", e);
            if (sseEmitter != null) {
                sseEmitter.send(new Chat(e.getMessage(), ChatStatus.FAILED));
                sseEmitter.completeWithError(e);
            }
            return null;
        }
    }
}
