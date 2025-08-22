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
import java.util.Objects;
import java.util.concurrent.Callable;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.oceanbase.odc.service.ai.Constants;
import com.oceanbase.odc.service.ai.chat.DatabaseCacheService;
import com.oceanbase.odc.service.ai.chat.model.Chat;
import com.oceanbase.odc.service.ai.chat.model.ChatStatus;
import com.oceanbase.odc.service.ai.chat.model.SqlCopilotReq;
import com.oceanbase.odc.service.ai.knowledgebase.utils.ChatUtils;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;
import com.oceanbase.odc.service.llm.sdk.StreamingChatModelWrapper;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/8/12
 */
@Slf4j
@Builder
public class SqlFormatTask implements Callable<String> {

    private final SseEmitter sseEmitter;
    private final StreamingChatModelWrapper chatModel;
    private final SqlCopilotReq req;
    private final DatabaseCacheService databaseCacheService;
    private final User creator;
    private final String grammer;

    @Override
    public String call() throws Exception {
        SecurityContextUtils.setCurrentUser(creator);
        try {
            String prompt = null;
            String promptTemplate = Constants.SQL_FORMATTING_PROMPT;

            if (Objects.nonNull(req.getDatabaseId())) {
                prompt = MessageFormat.format(promptTemplate, grammer);
            } else {
                prompt = MessageFormat.format(promptTemplate, Constants.MYSQL_GRAMMAR);
            }

            String query = getSelectedFileContent(req);

            // 调用大模型格式化SQL
            String formattedSql = chatModel.chat(ChatUtils.buildChatMessages(prompt, query));

            // 发送结果并完成
            if (sseEmitter != null) {
                sseEmitter.send(new Chat(ChatUtils.removeSurroundCharacter(formattedSql), ChatStatus.COMPLETED));
                sseEmitter.complete();
            }

            return formattedSql;
        } catch (Exception e) {
            log.error("Failed to format SQL", e);
            if (sseEmitter != null) {
                sseEmitter.send(new Chat(e.getMessage(), ChatStatus.FAILED));
                sseEmitter.complete();
            }
            return null;
        } finally {
            SecurityContextUtils.clear();
        }
    }

    private String getSelectedFileContent(SqlCopilotReq req) {
        return req.getStartPosition() >= req.getEndPosition() ? req.getFileContent()
                : req.getFileContent().substring(req.getStartPosition(),
                        Math.min(req.getEndPosition() + 1, req.getFileContent().length()));
    }
}
