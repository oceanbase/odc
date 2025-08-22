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

import java.io.IOException;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.OverLimitException;
import com.oceanbase.odc.service.ai.Constants;
import com.oceanbase.odc.service.ai.chat.model.Chat;
import com.oceanbase.odc.service.ai.chat.model.ChatProperties;
import com.oceanbase.odc.service.ai.chat.model.ChatStatus;
import com.oceanbase.odc.service.ai.chat.model.SqlCopilotReq;
import com.oceanbase.odc.service.ai.chat.task.ModifySqlTask;
import com.oceanbase.odc.service.ai.chat.task.Nl2SqlTask;
import com.oceanbase.odc.service.ai.chat.task.OptimizeOrDebugSqlTask;
import com.oceanbase.odc.service.ai.chat.task.SqlCompletionTask;
import com.oceanbase.odc.service.ai.chat.task.SqlFormatTask;
import com.oceanbase.odc.service.ai.knowledgebase.dbschema.SchemaIndexService;
import com.oceanbase.odc.service.ai.knowledgebase.dbschema.SchemaKBRetrieveService;
import com.oceanbase.odc.service.ai.knowledgebase.utils.SseEmitterUTF8;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.llm.AIConfigService;
import com.oceanbase.odc.service.llm.LlmService;
import com.oceanbase.odc.service.llm.model.AIConfig;
import com.oceanbase.odc.service.llm.provider.LlmProviderFacades;
import com.oceanbase.odc.service.llm.provider.ModelCredential;
import com.oceanbase.odc.service.llm.sdk.EmbeddingModelWrapper;
import com.oceanbase.odc.service.llm.sdk.StreamingChatModelWrapper;
import com.oceanbase.odc.service.session.ConnectSessionService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/7/31
 */
@Slf4j
@Service
public class SqlCopilotService {
    @Autowired
    @Qualifier("copilotOperationExecutor")
    private ThreadPoolTaskExecutor copilotOperationExecutor;
    @Autowired
    @Qualifier("nl2sqlGenerateExecutor")
    private ThreadPoolTaskExecutor nl2SqlGenerateExecutor;
    @Autowired
    private InfoExtractService infoExtractService;
    @Autowired
    private SchemaIndexService copilotIndexService;
    @Autowired
    private SchemaKBRetrieveService schemaKBRetrieveService;
    @Autowired
    private ChatProperties chatProperties;
    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private ConnectSessionService sessionService;
    @Autowired
    private LlmService llmService;
    @Autowired
    private LlmProviderFacades llmProviderFacades;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private AIConfigService aiConfigService;
    @Autowired
    private SqlCopilotRateLimiter sqlCopilotRateLimiter;
    @Autowired
    private DatabaseCacheService databaseCacheService;

    public SseEmitter completion(SqlCopilotReq req) throws IOException {
        String userId = authenticationFacade.currentUserIdStr();
        if (!sqlCopilotRateLimiter.tryAcquire(userId)) {
            throw new OverLimitException(ErrorCodes.TooManyRequest, null, "User calls are too frequent.");
        }
        return switch (req.getQuestionType()) {
            case NL_2_SQL -> nl2sql(req);
            case SQL_COMPLETION -> completeSqlByInputText(req);
            case SQL_MODIFIER -> modifySqlByInputText(req);
            case SQL_DEBUGGING, SQL_OPTIMIZER -> optimizeOrDebugSql(req);
            case SQL_FORMATTING -> formatSql(req);
            default -> throw new IllegalArgumentException("Unsupported question type: " + req.getQuestionType());
        };
    }

    private SseEmitter nl2sql(SqlCopilotReq req) {
        ConnectionSession session = databaseCacheService.getSession(req.getSid(), req.getDatabaseId());
        String dbName = null;
        String grammer = Constants.MYSQL_GRAMMAR;
        if (Objects.nonNull(req.getDatabaseId())) {
            DatabaseInfo databaseInfo = databaseCacheService.get(req.getDatabaseId());
            ConnectionSessionUtil.setCurrentSchema(session, databaseInfo.getDatabaseName());
            dbName = databaseInfo.getDatabaseName();
            grammer = databaseInfo.getDialectType().name();
        }
        SseEmitter sseEmitter = new SseEmitterUTF8();
        AIConfig aiConfig = aiConfigService.getAIConfig();
        StreamingChatModelWrapper chatModel = null;
        if (StringUtils.isNotBlank(req.getModel())) {
            chatModel = getChatModel(req.getModel());
        } else {
            chatModel = getChatModel(aiConfig.getDefaultChatModel());
        }
        Nl2SqlTask task =
                buildNl2SqlTask(req, sseEmitter, chatModel, getEmbeddingModel(aiConfig.getDefaultEmbeddingModel()),
                        dbName, grammer, session);
        nl2SqlGenerateExecutor.submit(task);
        return sseEmitter;
    }

    public SseEmitter completeSqlByInputText(SqlCopilotReq req) throws IOException {
        SseEmitter sseEmitter = new SseEmitterUTF8();
        try {
            StreamingChatModelWrapper chatModel = null;
            if (StringUtils.isNotBlank(req.getModel())) {
                chatModel = getChatModel(req.getModel());
            } else {
                AIConfig aiConfig = aiConfigService.getAIConfig();
                chatModel = getChatModel(aiConfig.getDefaultChatModel());
            }

            // 获取光标最近N行上下文，N为copilotProperties.getSessionContextMaxLines()
            String input = req.getFileContent();
            int cursorPos = req.getCursorPosition();
            String preCursorInput = input.substring(0, cursorPos);
            String delimiter = req.getDelimiter();
            if (delimiter == null || delimiter.isEmpty()) {
                delimiter = ";";
            }
            if (preCursorInput.endsWith(delimiter)) {
                sseEmitter.complete();
                return sseEmitter;
            }
            String[] preCursorLines = preCursorInput.split("\n");
            // 判断光标前一行是否为注释，如果是，则根据注释内容生成SQL
            if (preCursorLines.length > 0
                    && preCursorLines[preCursorLines.length - 1].startsWith(Constants.ANNOTATION)
                    && preCursorInput.endsWith("\n")) {
                return nl2sql(req);
            }
            SqlCompletionTask task = SqlCompletionTask.builder()
                    .grammar(getGrammarByDatabaseId(req.getDatabaseId()))
                    .chatModel(chatModel)
                    .chatProperties(chatProperties)
                    .req(req)
                    .sseEmitter(sseEmitter)
                    .databaseCacheService(databaseCacheService)
                    .build();
            nl2SqlGenerateExecutor.submit(task);
            return sseEmitter;
        } catch (Exception e) {
            log.warn("Failed to complete SQL by input text", e);
            sseEmitter.send(new Chat(e.getMessage(), ChatStatus.FAILED));
            sseEmitter.complete();
            return sseEmitter;
        }
    }

    public SseEmitter modifySqlByInputText(SqlCopilotReq req) throws IOException {
        SseEmitter sseEmitter = new SseEmitterUTF8();
        try {
            StreamingChatModelWrapper chatModel = null;
            if (StringUtils.isNotBlank(req.getModel())) {
                chatModel = getChatModel(req.getModel());
            } else {
                AIConfig aiConfig = aiConfigService.getAIConfig();
                chatModel = getChatModel(aiConfig.getDefaultChatModel());
            }
            ModifySqlTask task = ModifySqlTask.builder()
                    .chatModel(chatModel)
                    .req(req)
                    .sseEmitter(sseEmitter)
                    .databaseCacheService(databaseCacheService)
                    .grammar(getGrammarByDatabaseId(req.getDatabaseId()))
                    .creator(authenticationFacade.currentUser())
                    .build();
            nl2SqlGenerateExecutor.submit(task);
            return sseEmitter;
        } catch (Exception e) {
            log.warn("Failed to modify SQL by input text", e);
            sseEmitter.send(new Chat(e.getMessage(), ChatStatus.FAILED));
            sseEmitter.complete();
            return sseEmitter;
        }
    }

    private SseEmitter optimizeOrDebugSql(SqlCopilotReq req) throws IOException {
        SseEmitter sseEmitter = new SseEmitterUTF8();
        try {
            StreamingChatModelWrapper chatModel = null;
            if (StringUtils.isNotBlank(req.getModel())) {
                chatModel = getChatModel(req.getModel());
            } else {
                AIConfig aiConfig = aiConfigService.getAIConfig();
                chatModel = getChatModel(aiConfig.getDefaultChatModel());
            }
            OptimizeOrDebugSqlTask task = OptimizeOrDebugSqlTask.builder()
                    .chatModel(chatModel)
                    .req(req)
                    .sseEmitter(sseEmitter)
                    .databaseCacheService(databaseCacheService)
                    .creator(authenticationFacade.currentUser())
                    .grammar(getGrammarByDatabaseId(req.getDatabaseId()))
                    .build();
            nl2SqlGenerateExecutor.submit(task);
            return sseEmitter;
        } catch (Exception e) {
            log.warn("Failed to optimize or debug SQL", e);
            sseEmitter.send(new Chat(e.getMessage(), ChatStatus.FAILED));
            sseEmitter.complete();
            return sseEmitter;
        }
    }

    private SseEmitter formatSql(SqlCopilotReq req) throws IOException {
        SseEmitter sseEmitter = new SseEmitterUTF8();
        try {
            StreamingChatModelWrapper chatModel = null;
            if (StringUtils.isNotBlank(req.getModel())) {
                chatModel = getChatModel(req.getModel());
            } else {
                AIConfig aiConfig = aiConfigService.getAIConfig();
                chatModel = getChatModel(aiConfig.getDefaultChatModel());
            }

            SqlFormatTask task = SqlFormatTask.builder()
                    .chatModel(chatModel)
                    .req(req)
                    .sseEmitter(sseEmitter)
                    .databaseCacheService(databaseCacheService)
                    .creator(authenticationFacade.currentUser())
                    .grammer(getGrammarByDatabaseId(req.getDatabaseId()))
                    .build();

            nl2SqlGenerateExecutor.submit(task);
            return sseEmitter;
        } catch (Exception e) {
            log.warn("Failed to format SQL", e);
            sseEmitter.send(new Chat(e.getMessage(), ChatStatus.FAILED));
            sseEmitter.complete();
            return sseEmitter;
        }
    }

    private Nl2SqlTask buildNl2SqlTask(SqlCopilotReq req, SseEmitter sseEmitter, StreamingChatModelWrapper chatModel,
            EmbeddingModelWrapper embeddingModel,
            String dbName, String grammer, ConnectionSession session) {
        return Nl2SqlTask.builder()
                .copilotOperationExecutor(copilotOperationExecutor)
                .text2SqlReq(req)
                .copilotIndexService(copilotIndexService)
                .schemaKBRetrieveService(schemaKBRetrieveService)
                .infoExtractService(infoExtractService)
                .chatProperties(chatProperties)
                .sseEmitter(sseEmitter)
                .dbName(dbName)
                .grammer(grammer)
                .session(session)
                .chatModel(chatModel)
                .embeddingModel(embeddingModel)
                .build();
    }

    private StreamingChatModelWrapper getChatModel(String modelId) {
        if (StringUtils.isBlank(modelId)) {
            throw new BadRequestException("Llm model is required");
        }
        String[] split = modelId.split("/", 2);
        Verify.equals(2, split.length, "Invalid embedding model format");

        ModelCredential credential = llmService.getModelCredentialSkipPermissionCheck(split[0],
                split[1], authenticationFacade.currentOrganizationId());
        if (credential == null) {
            throw new BadRequestException(String.format("AI is not enabled or model %s not found", modelId));
        }
        return llmProviderFacades.getProviderFacade(split[0]).generateStreamingChatModel(split[1], credential);
    }


    private EmbeddingModelWrapper getEmbeddingModel(String modelId) {
        if (StringUtils.isBlank(modelId)) {
            throw new BadRequestException("Embedding model is required");
        }
        String[] split = modelId.split("/", 2);
        Verify.equals(2, split.length, "Invalid embedding model format");

        ModelCredential credential = llmService.getModelCredentialSkipPermissionCheck(split[0],
                split[1], authenticationFacade.currentOrganizationId());
        if (credential == null) {
            throw new BadRequestException(String.format("AI is not enabled or model %s not found", modelId));
        }
        return llmProviderFacades.getProviderFacade(split[0]).generateEmbeddingModel(split[1], credential);
    }

    private String getGrammarByDatabaseId(Long databaseId) {
        DatabaseInfo databaseInfo = databaseCacheService.get(databaseId);
        PreConditions.notNull(databaseInfo, "databaseInfo");
        DialectType dialectType = databaseInfo.getDialectType();
        return dialectType.name();
    }

    @Data
    @AllArgsConstructor
    public static class DatabaseInfo {
        private Long databaseId;
        private DialectType dialectType;
        private String databaseName;
    }

}
