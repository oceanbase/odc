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

import java.io.IOException;
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
public class ModifySqlTask implements Callable<String> {

    private final SseEmitter sseEmitter;
    private final StreamingChatModelWrapper chatModel;
    private final SqlCopilotReq req;
    private final DatabaseCacheService databaseCacheService;
    private final User creator;
    private final String grammar;

    @Override
    public String call() throws Exception {
        SecurityContextUtils.setCurrentUser(creator);
        try {
            // 区分选中内容和上下文
            String preContent = req.getFileContent().substring(0, req.getStartPosition());
            String selectContent = req.getFileContent().substring(req.getStartPosition(),
                    Math.min(req.getEndPosition() + 1, req.getFileContent().length()));
            String afterContent = req.getFileContent().substring(
                    Math.min(req.getEndPosition() + 1, req.getFileContent().length()));

            // 拼接prompt
            String allDDLText = "";
            String grammar = null;
            if (Objects.nonNull(req.getDatabaseId())) {
                try {
                    allDDLText = databaseCacheService.getSplicedTableDDLText(req.getDatabaseId(), selectContent,
                            req.getSid());
                } catch (Exception e) {
                    log.warn("Failed to get ddl text for database id: {}", req.getDatabaseId(), e);
                }
            }

            String context = preContent + '\n' + afterContent;
            String prompt = MessageFormat.format(Constants.MODIFY_SQL_WITH_DDL_PROMPT,
                    allDDLText.replace("```", ""), context.replace("```", ""), grammar);
            String query = selectContent + '\n' + req.getInput();

            // 调用大模型生成修改后的SQL
            String sql = chatModel.chat(ChatUtils.buildChatMessages(prompt, query));

            // 发送结果并完成
            if (sseEmitter != null) {
                sseEmitter.send(new Chat(ChatUtils.removeSurroundCharacter(sql), ChatStatus.COMPLETED));
                sseEmitter.complete();
            }

            return sql;
        } catch (Exception e) {
            log.error("Failed to modify SQL", e);
            if (sseEmitter != null) {
                try {
                    sseEmitter.send(new Chat(e.getMessage(), ChatStatus.FAILED));
                } catch (IOException ex) {
                    log.error("Failed to send error message", ex);
                } finally {
                    sseEmitter.completeWithError(e);
                }
            }
            return null;
        } finally {
            SecurityContextUtils.clear();
        }
    }
}
