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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTreeFactories;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTreeFactory;
import com.oceanbase.odc.service.ai.Constants;
import com.oceanbase.odc.service.ai.chat.InfoExtractService;
import com.oceanbase.odc.service.ai.chat.model.Chat;
import com.oceanbase.odc.service.ai.chat.model.ChatProperties;
import com.oceanbase.odc.service.ai.chat.model.ChatStatus;
import com.oceanbase.odc.service.ai.chat.model.CopilotStats;
import com.oceanbase.odc.service.ai.chat.model.ExtractionInfo;
import com.oceanbase.odc.service.ai.chat.model.RetrieveResult;
import com.oceanbase.odc.service.ai.chat.model.SqlCopilotReq;
import com.oceanbase.odc.service.ai.chat.util.MSchemaBuilder;
import com.oceanbase.odc.service.ai.knowledgebase.dbschema.SchemaIndexService;
import com.oceanbase.odc.service.ai.knowledgebase.dbschema.SchemaKBRetrieveService;
import com.oceanbase.odc.service.ai.knowledgebase.model.Document;
import com.oceanbase.odc.service.ai.knowledgebase.utils.ChatUtils;
import com.oceanbase.odc.service.ai.knowledgebase.utils.CopilotSchemaAccessor;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.llm.sdk.EmbeddingModelWrapper;
import com.oceanbase.odc.service.llm.sdk.StreamingChatModelWrapper;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;
import com.oceanbase.tools.dbbrowser.parser.result.BasicResult;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/8/1
 */
@Slf4j
@Builder
public class Nl2SqlTask implements Callable<String> {

    private final SqlCopilotReq text2SqlReq;
    private final ChatProperties chatProperties;
    private final InfoExtractService infoExtractService;
    private final SchemaIndexService copilotIndexService;
    private final String grammer;
    private final ThreadPoolTaskExecutor copilotOperationExecutor;
    private final ConnectionSession session;
    private final String dbName;
    private final String requestId = UUID.randomUUID().toString();
    private final SchemaKBRetrieveService schemaKBRetrieveService;
    private final EmbeddingModelWrapper embeddingModel;
    private final StreamingChatModelWrapper chatModel;
    private final SseEmitter sseEmitter;

    public String call() {
        try {
            String query = text2SqlReq.getInput();
            CopilotStats stats = new CopilotStats();
            boolean needRetrieve = true;
            List<String> tableNames = new ArrayList<>();
            if (session != null) {
                tableNames = DBSchemaAccessors.create(session).showTables(dbName);
                if (CollectionUtils.isNotEmpty(tableNames)
                        && tableNames.size() < chatProperties.getText2SqlMaxTableCountToLLM()) {
                    // 当数据库中的表数量小于某个阈值时，不进行召回，直接使用数据库中的表
                    needRetrieve = false;
                }
            }
            String prompt;
            StringBuilder ddlBuilder = new StringBuilder();
            if (Objects.nonNull(text2SqlReq.getDatabaseId())) {
                if (!needRetrieve) {
                    CopilotSchemaAccessor
                            .listTableDDLWithOptions(tableNames, dbName, DBSchemaAccessors.create(session), true)
                            .forEach(ddl -> ddlBuilder.append(ddl).append('\n'));
                    prompt = buildText2SqlPrompt(ddlBuilder.toString(), grammer);
                } else {
                    long extractStartTimeStamp = System.currentTimeMillis();
                    // 获取解析和SQL预生成结果
                    ExtractionInfo extractInfo;
                    try {
                        extractInfo = infoExtractService.extractInfoFromQuery(query, chatModel);
                        log.info("extractInfo:{}, requestId={}", extractInfo, requestId);
                    } catch (Exception e) {
                        log.warn("Generate SQL Extract Failed, requestId={}", requestId, e);
                        throw e;
                    }
                    stats.setExtractDurationMillis(System.currentTimeMillis() - extractStartTimeStamp);

                    long retrieveStartTimeStamp = System.currentTimeMillis();
                    // 设置了 databaseId 则需从向量、KV 中召回用户数据库元数据信息
                    Future<List<Document>> vectorRetrieveFuture = copilotOperationExecutor.submit(
                            () -> schemaKBRetrieveService.retrieveDocumentByVectorIndex(query,
                                    extractInfo.getTableNames(),
                                    extractInfo.getColumnNames(), text2SqlReq.getDatabaseId(),
                                    embeddingModel));
                    Future<List<Document>> kvRetrieveFuture = copilotOperationExecutor.submit(
                            () -> schemaKBRetrieveService.retrieveDocumentByKeyValueIndex(extractInfo.getTableNames(),
                                    extractInfo.getColumnNames(),
                                    text2SqlReq.getDatabaseId()));

                    try {
                        List<Document> docsFromVector = vectorRetrieveFuture.get();
                        List<Document> docsFromKV = kvRetrieveFuture.get();
                        // 排序
                        List<RetrieveResult> retrieveResults = new ArrayList<>();
                        retrieveResults.add(
                                new RetrieveResult(docsFromVector, chatProperties.getVectorIndexRetrieveWeight()));
                        retrieveResults.add(
                                new RetrieveResult(docsFromKV, chatProperties.getKvIndexRetrieveWeight()));
                        tableNames = copilotIndexService.sortTopKTable(retrieveResults,
                                chatProperties.getRetrieveTopK());
                    } catch (Exception e) {
                        log.warn("Failed to retrieve results when databaseId is not null, requestId={}", requestId, e);
                    }
                    stats.setRetrieveDurationMillis(System.currentTimeMillis() - retrieveStartTimeStamp);

                    // 组装prompt
                    long buildPromptStartTimeStamp = System.currentTimeMillis();
                    for (String tableName : tableNames) {
                        String mSchema = getMSchemaStr(dbName, tableName);
                        ddlBuilder.append(mSchema).append('\n');
                    }
                    prompt = buildText2SqlPrompt(ddlBuilder.toString().replace("```", ""), grammer);
                    stats.setBuildPromptDurationMillis(System.currentTimeMillis() - buildPromptStartTimeStamp);
                }
            } else {
                long buildPromptStartTimeStamp = System.currentTimeMillis();
                // 未指定数据库，使用默认的MySQL语法
                prompt = buildText2SqlPrompt("", Constants.MYSQL_GRAMMAR);
                stats.setBuildPromptDurationMillis(System.currentTimeMillis() - buildPromptStartTimeStamp);
            }

            // 生成SQL
            log.info("Generate SQL, requestId={}, cost time detail, {}", requestId, stats);
            if (log.isDebugEnabled()) {
                log.debug("prompt: {}", prompt);
            }
            String sql = ChatUtils.removeSurroundCharacter(getLLMOutputByUserQueryAndSystem(query, prompt));
            if (session == null || chatProperties.getText2SqlFeedbackTimes() < 1) {
                // 未传 databaseId 与 sid
                return complete(sql);
            }
            try {
                // 多条 sql 直接返回，非 SELECT 直接返回
                List<String> sqls = SqlUtils.split(session.getDialectType(), sql, ";");
                if (CollectionUtils.size(sqls) != 1) {
                    return complete(sql);
                }
                AbstractSyntaxTreeFactory factory =
                        AbstractSyntaxTreeFactories.getAstFactory(session.getDialectType(), 0);
                if (factory != null) {
                    BasicResult r = factory.buildAst(sql).getParseResult();
                    if (r.getSqlType() != SqlType.SELECT) {
                        return complete(sql);
                    }
                }
            } catch (Exception e) {
                log.error("parse sql failed, requestId={}, sql={}", requestId, sql, e);
                throw e;
            }

            // fix SQL
            String explainRes = explain(session, sql);
            if ("success".equalsIgnoreCase(explainRes)) {
                log.info("explain success, requestId={}", requestId);
                return complete(sql);
            }
            for (int i = 0; i < chatProperties.getText2SqlFeedbackTimes(); i++) {
                String user = buildSqlFixUser(text2SqlReq.getInput(), sql, explainRes);
                String system = buildSqlFixPrompt(ddlBuilder.toString());
                if (log.isDebugEnabled()) {
                    log.debug("fix-sql prompt: {}", system);
                    log.debug("fix-sql user query: {}", user);
                }
                sql = ChatUtils.removeSurroundCharacter(getLLMOutputByUserQueryAndSystem(user, system));
                explainRes = explain(session, sql);
                if ("success".equalsIgnoreCase(explainRes)) {
                    log.info("explain success, requestId={}", requestId);
                    break;
                } else {
                    log.warn("explain failed, requestId={}, explainRes={}", requestId, explainRes);
                }
            }
            return complete(sql);
        } catch (Exception e) {
            log.error("Generate SQL Failed, requestId={}", requestId, e);
            fail(e);
            return null;
        } finally {
            // 避免 session 泄漏
            if (Objects.isNull(text2SqlReq.getSid()) && session != null) {
                session.expire();
            }
        }
    }

    private String complete(String sql) throws IOException {
        if (sseEmitter != null) {
            sseEmitter.send(new Chat(sql, ChatStatus.COMPLETED));
            sseEmitter.complete();
        }
        return sql;
    }

    private void fail(Exception e) {
        if (sseEmitter != null) {
            try {
                sseEmitter.send(new Chat(e.getMessage(), ChatStatus.FAILED));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } finally {
                sseEmitter.complete();
            }
        }
    }

    private String getLLMOutputByUserQueryAndSystem(String query, String system) {
        return chatModel.chat(ChatUtils.buildChatMessages(system, query));
    }

    private String buildText2SqlPrompt(String schemaInfo, String grammar) {
        return MessageFormat.format(Constants.GENERATE_SQL_WITH_DDL_PROMPT, grammar, schemaInfo);
    }

    private String buildSqlFixPrompt(String schemaInfo) {
        return MessageFormat.format(Constants.FIX_SQL_SYSTEM, grammer, schemaInfo);
    }

    private String buildSqlFixUser(String query, String sql, String error) {
        return MessageFormat.format(Constants.FIX_SQL_USER, query, sql, error);
    }

    private String explain(ConnectionSession session, String sql) {
        String explain = "explain " + sql;
        try {
            if (log.isDebugEnabled()) {
                log.debug("explain sql: {}", explain);
            }
            session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY).execute(explain);
        } catch (Exception e) {
            log.error("execute failed, sql: {}, message: {}", explain, e.getMessage(), e);
            return e.getMessage();
        }
        return "success";
    }

    private String getMSchemaStr(String dbName, String tableName) {
        DBSchemaAccessor accessor = DBSchemaAccessors.create(session);
        List<DBTableConstraint> constraints = accessor.listTableConstraints(dbName, tableName);
        List<DBTableColumn> fields = accessor.listTableColumns(dbName, tableName);
        String comment = accessor.getTableOptions(dbName, tableName).getComment();
        return MSchemaBuilder.build(tableName, comment, fields, constraints);
    }

}
