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
package com.oceanbase.odc.service.ai.knowledgebase.dbschema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.ai.knowledgebase.utils.CopilotSchemaAccessor;
import com.oceanbase.odc.service.common.util.ConditionalOnProperty;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.db.browser.DBTableEditors;
import com.oceanbase.odc.service.llm.AIConfigService;
import com.oceanbase.odc.service.llm.LlmService;
import com.oceanbase.odc.service.llm.model.AIConfig;
import com.oceanbase.odc.service.llm.provider.LlmProviderFacades;
import com.oceanbase.odc.service.llm.provider.ModelCredential;
import com.oceanbase.odc.service.llm.sdk.EmbeddingModelWrapper;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/7/21
 */
@Component
@Slf4j
@ConditionalOnProperty(value = "odc.ai.kb.enable-build-kb", havingValues = "true")
public class SchemaKBBuildManager {
    private static final String LOCK_KEY_PREFIX = "build-copilot-index-lock-";

    @Qualifier("schemaKBBuildExecutor")
    @Autowired(required = false)
    private ThreadPoolTaskExecutor schemaKBBuildExecutor;
    @Qualifier("submitSchemaBuildTaskExecutor")
    @Autowired(required = false)
    private ThreadPoolTaskExecutor submitSchemaBuildTaskExecutor;
    @Autowired(required = false)
    @Qualifier("vectordbJdbcLockRepository")
    private JdbcLockRegistry vectordbJdbcLockRepository;

    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private SchemaKBProperties schemaKBProperties;
    @Autowired
    private SchemaIndexService schemaIndexService;
    @Autowired
    private AIConfigService aiConfigService;
    @Autowired
    private LlmProviderFacades llmProviderFacades;
    @Autowired
    private LlmService llmService;
    @Autowired
    private ConnectSessionService sessionService;

    public void submitBuildSchemaKBTaskForAllOrganizations() {
        List<AIConfig> organizationConfigs = aiConfigService.listAllOrganizationConfigForEmbedding();
        organizationConfigs
                .forEach(aiConfig -> submitBuildSchemaKBTaskForOrganization(aiConfig.getOrganizationId(), aiConfig));
    }

    public void submitBuildSchemaKBTaskForOrganization(Long organizationId, AIConfig aiConfig) {
        EmbeddingModelWrapper embeddingModel = getEmbeddingModel(aiConfig);
        if (embeddingModel == null) {
            log.warn("Embedding model is null, organizationId={}", organizationId);
            return;
        }
        List<Long> datasourceIds = connectionService.innerListIdByOrganizationId(organizationId);
        datasourceIds
                .forEach(datasourceId -> submitBuildSchemaKBTaskForDatasource(datasourceId, embeddingModel));
    }

    public void submitBuildSchemaKBTaskForDatasource(Long datasourceId, EmbeddingModelWrapper embeddingModel) {
        ConnectionConfig connectionConfig = connectionService.getForConnectionSkipPermissionCheck(datasourceId);
        submitBuildSchemaKBTaskForDatasource(connectionConfig, embeddingModel);
    }

    public void submitBuildSchemaKBTaskForDatasource(ConnectionConfig connection,
            EmbeddingModelWrapper embeddingModel) {
        submitSchemaBuildTaskExecutor.submit(() -> {
            ConnectionSession session = new DefaultConnectSessionFactory(connection).generateSession();
            try {
                List<Database> databases =
                        databaseService.listBasicByDatasourceIdSkipPermissionCheck(connection.getId());
                databases
                        .stream().filter(d -> Objects.nonNull(d.getProject()) && Objects.nonNull(d.getProject().id()))
                        .forEach(database -> doBuildSchemaKBTaskForDatabase(database, session, embeddingModel));
            } finally {
                session.expire();
            }
        });
    }

    public void submitBuildSchemaKBTaskForDatabaseIds(List<Long> dbIds, @Nullable ConnectionSession session,
            @Nullable EmbeddingModelWrapper embeddingModel) {
        submitBuildSchemaKBTaskForDatabases(
                databaseService.listBasicSkipPermissionCheckByIds(dbIds), session, embeddingModel);
    }

    public void submitBuildSchemaKBTaskForDatabases(List<Database> databases, @Nullable ConnectionSession session,
            @Nullable EmbeddingModelWrapper embeddingModel) {
        databases.forEach(d -> submitBuildSchemaKBTaskForDatabase(d, session, embeddingModel));
    }

    public void submitBuildSchemaKBTaskForDatabase(Database database, @Nullable ConnectionSession session,
            @Nullable EmbeddingModelWrapper embeddingModel) {
        submitSchemaBuildTaskExecutor.submit(() -> doBuildSchemaKBTaskForDatabase(database, session, embeddingModel));
    }

    private void doBuildSchemaKBTaskForDatabase(Database database, @Nullable ConnectionSession session,
            @Nullable EmbeddingModelWrapper embeddingModel) {
        if (embeddingModel == null) {
            AIConfig aiConfig = aiConfigService.getAIConfigSkipPermissionCheck(
                    database.getOrganizationId());
            if (!Objects.equals(Boolean.TRUE, aiConfig.getChatEnabled())
                    && !Objects.equals(Boolean.TRUE, aiConfig.getCopilotEnabled())) {
                return;
            }
            embeddingModel = getEmbeddingModel(aiConfig);
            if (embeddingModel == null) {
                log.warn("Embedding model is null, database={}", database);
                return;
            }
        }
        Lock lock = vectordbJdbcLockRepository.obtain(LOCK_KEY_PREFIX + database.getId());
        if (!lock.tryLock()) {
            log.info("Build schema index lock is not available, database={}", database);
            return;
        }
        boolean destroySession = false;
        try {
            if (session == null) {
                ConnectionConfig connectionConfig = connectionService.getForConnectionSkipPermissionCheck(
                        database.getDataSource().getId());
                session = new DefaultConnectSessionFactory(connectionConfig).generateSession();
                destroySession = true;
            }
            DBSchemaAccessor schemaAccessor = DBSchemaAccessors.create(session);
            DBTableEditor tableEditor = DBTableEditors.create(session);

            List<DBObjectIdentity> tables = schemaAccessor.listTables(database.getName(), null);
            Function<List<DBObjectIdentity>, List<SchemaIndex>> listTableIndicesFunction =
                    ts -> CopilotSchemaAccessor.listTablesSchemaIndex(ts, schemaAccessor, tableEditor,
                            database);
            if (CollectionUtils.isNotEmpty(tables)) {
                submitEachDBIdentity(database,
                        tables,
                        listTableIndicesFunction,
                        embeddingModel);
            }
        } catch (Exception e) {
            log.warn("Failed to build schema index for database {}", database, e);
        } finally {
            lock.unlock();
            if (destroySession) {
                session.expire();
            }
        }
    }

    private <T> void submitEachDBIdentity(Database database, List<DBObjectIdentity> identities,
            Function<List<DBObjectIdentity>, List<SchemaIndex>> listSchemaIndicesFunction,
            EmbeddingModelWrapper embeddingModel) {
        if (CollectionUtils.isEmpty(identities)) {
            return;
        }
        List<List<DBObjectIdentity>> distributedIdentities = distribute(identities,
                schemaKBProperties.getBuildKBConcurrent(), schemaKBProperties.getEmbeddingBatchSize()).stream()
                        .filter(CollectionUtils::isNotEmpty).toList();
        log.info("Distribute db identity succeed, identitySizes={}", distributedIdentities
                .stream().map(i -> i.size() + "").collect(Collectors.joining(", ")));
        List<Future<Object>> futures = distributedIdentities.stream().map(i -> schemaKBBuildExecutor
                .submit(
                        () -> {
                            List<SchemaIndex> list = listSchemaIndicesFunction.apply(i);
                            if (CollectionUtils.isNotEmpty(list)) {
                                schemaIndexService.doBuildIndexes(database, list, embeddingModel);
                            }
                            return null;
                        }))
                .toList();
        for (Future<Object> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                log.warn("Failed to build schema index", e);
            }
        }
    }

    private List<List<DBObjectIdentity>> distribute(List<DBObjectIdentity> originalList, int n, int minSize) {
        if (originalList == null || originalList.isEmpty() || n <= 0 || minSize <= 0) {
            return List.of();
        }
        int total = originalList.size();
        int groupCount = Math.min(n, (total + minSize - 1) / minSize);
        if (groupCount == 0) {
            return List.of();
        }
        List<List<DBObjectIdentity>> result = new ArrayList<>(groupCount);
        int baseSize = total / groupCount;
        int extra = total % groupCount;
        int start = 0;
        for (int i = 0; i < groupCount; i++) {
            int currentGroupSize = baseSize + (i < extra ? 1 : 0);
            int end = start + currentGroupSize;
            end = Math.min(end, total);
            result.add(new ArrayList<>(originalList.subList(start, end)));
            start = end;
        }
        return result;
    }

    private EmbeddingModelWrapper getEmbeddingModel(AIConfig aiConfig) {
        String embeddingModel = aiConfig.getDefaultEmbeddingModel();
        if (StringUtils.isBlank(embeddingModel)) {
            return null;
        }
        String[] split = embeddingModel.split("/", 2);
        Verify.equals(2, split.length, "Invalid embedding model format");

        ModelCredential credential = llmService.getModelCredentialSkipPermissionCheck(split[0],
                split[1], aiConfig.getOrganizationId());
        if (credential == null) {
            return null;
        }
        return llmProviderFacades.getProviderFacade(split[0]).generateEmbeddingModel(split[1], credential);
    }


}
