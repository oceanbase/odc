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
import com.oceanbase.odc.service.ai.chat.model.QuestionType;
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
public class OptimizeOrDebugSqlTask implements Callable<String> {

    private final SseEmitter sseEmitter;
    private final StreamingChatModelWrapper chatModel;
    private final SqlCopilotReq req;
    private final DatabaseCacheService databaseCacheService;
    private final User creator;
    private String grammar;

    @Override
    public String call() throws Exception {
        SecurityContextUtils.setCurrentUser(creator);
        try {
            String prompt = null;
            String promptTemplate =
                    QuestionType.SQL_OPTIMIZER.equals(req.getQuestionType())
                            ? Constants.SQL_OPTIMIZER_WITH_DDL_PROMPT
                            : Constants.SQL_DEBUGGING_WITH_DDL_PROMPT;

            if (Objects.nonNull(req.getDatabaseId())) {
                String ddlText = "";
                try {
                    ddlText = databaseCacheService.getSplicedTableDDLText(req.getDatabaseId(), req.getFileContent(),
                            req.getSid());
                } catch (Exception e) {
                    log.warn("Failed to get ddl text for database id: {}", req.getDatabaseId(), e);
                }
                prompt = MessageFormat.format(promptTemplate, ddlText.replace("```", ""), grammar);
            } else {
                prompt = MessageFormat.format(promptTemplate, "", grammar);
            }

            String query = req.getInput() + "\n" + getSelectedFileContent(req);

            // 调用大模型生成优化或调试后的SQL
            String sql = chatModel.chat(ChatUtils.buildChatMessages(prompt, query));

            // 发送结果并完成
            if (sseEmitter != null) {
                sseEmitter.send(new Chat(ChatUtils.removeSurroundCharacter(sql), ChatStatus.COMPLETED));
                sseEmitter.complete();
            }

            return sql;
        } catch (Exception e) {
            log.error("Failed to optimize or debug SQL", e);
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

    private String getSelectedFileContent(SqlCopilotReq req) {
        return req.getStartPosition() >= req.getEndPosition() ? req.getFileContent()
                : req.getFileContent().substring(req.getStartPosition(),
                        Math.min(req.getEndPosition() + 1, req.getFileContent().length()));
    }
}
