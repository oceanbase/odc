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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.metadb.ai.QueryKBKeyValueParams;
import com.oceanbase.odc.metadb.ai.SchemaKBKeyValueEntity;
import com.oceanbase.odc.metadb.ai.SchemaKBKeyValueRepository;
import com.oceanbase.odc.service.ai.chat.model.ChatProperties;
import com.oceanbase.odc.service.ai.knowledgebase.model.Document;
import com.oceanbase.odc.service.ai.knowledgebase.model.RetrieveOriginType;
import com.oceanbase.odc.service.llm.sdk.EmbeddingModelWrapper;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/8/1
 */
@Slf4j
@Component
public class SchemaKBRetrieveService {

    @Autowired
    private SchemaVectorService schemaVectorService;
    @Autowired
    private SchemaKBKeyValueRepository keyValueRepository;
    @Autowired
    private ChatProperties chatProperties;

    public List<Document> retrieveDocumentByVectorIndex(String query, List<String> tableNames, List<String> columnNames,
            Long databaseId, EmbeddingModelWrapper embeddingModel) {
        List<Document> docs = new ArrayList<>();
        try {
            // 表召回
            // 根据原始内容召回
            docs.addAll(schemaVectorService.retrieveDocument(query, databaseId, RetrieveOriginType.TABLE.name(),
                    chatProperties.getVectorIndexTableRetrieveNum(), embeddingModel));
            // 根据抽取表名召回
            if (tableNames != null) {
                for (String tableName : tableNames) {
                    docs.addAll(schemaVectorService.retrieveDocument(tableName, databaseId,
                            RetrieveOriginType.TABLE.name(), 1, embeddingModel));
                }
            }
            // 字段召回
            if (columnNames != null && !columnNames.isEmpty()) {
                String columnStr = StringUtils.join(columnNames, ' ');
                docs.addAll(
                        schemaVectorService.retrieveDocument(columnStr, databaseId,
                                RetrieveOriginType.FIELD.name(),
                                chatProperties.getVectorIndexFieldRetrieveNum(), embeddingModel));
            }
        } catch (Exception e) {
            // 存在召回时索引尚未构建的情况，此时会因为没有表而报错，吞掉异常，返回空列表
            log.warn("retrieve vector index failed", e);
        }
        if (log.isDebugEnabled()) {
            log.debug("retrieve vector index result: {}", JsonUtils.toJson(docs));
        }
        // 过滤低分
        return docs.stream().filter(doc -> doc.getScore() > chatProperties.getVectorIndexRetrieveMinScore())
                .collect(Collectors.toList());
    }

    public List<Document> retrieveDocumentByKeyValueIndex(List<String> tableNames, List<String> columnNames,
            Long databaseId) {
        List<Document> docs = new ArrayList<>();
        // 表召回
        if (tableNames != null) {
            for (String tableName : tableNames) {
                QueryKBKeyValueParams params = new QueryKBKeyValueParams();
                params.setKeys(Collections.singletonList(tableName));
                params.setDatabaseId(databaseId);
                params.setTag(RetrieveOriginType.TABLE.name());
                List<SchemaKBKeyValueEntity> keyValueEntities =
                        keyValueRepository.findByKeyContentAndDatabaseIdAndTag(params);
                for (SchemaKBKeyValueEntity keyValueEntity : keyValueEntities) {
                    Document document = convertKeyValueEntity2Document(keyValueEntity);
                    document.setScore(chatProperties.getKvIndexTableRetrieveScore());
                    docs.add(document);
                }
            }
        }
        // 字段召回
        if (columnNames != null) {
            Map<String, Integer> tableMapCount = new HashMap<>();
            for (String column : columnNames) {
                QueryKBKeyValueParams params = new QueryKBKeyValueParams();
                params.setKeys(Collections.singletonList(column));
                params.setDatabaseId(databaseId);
                params.setTag(RetrieveOriginType.FIELD.name());
                List<SchemaKBKeyValueEntity> keyValueEntities =
                        keyValueRepository.findByKeyContentAndDatabaseIdAndTag(params);
                for (SchemaKBKeyValueEntity entity : keyValueEntities) {
                    tableMapCount.put(entity.getValueContent(),
                            tableMapCount.getOrDefault(entity.getValueContent(), 1));
                }
            }
            List<String> sortedTables = tableMapCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .map(Map.Entry::getKey)
                    .toList();
            for (int i = 0; i < Math.min(chatProperties.getKvIndexFieldRetrieveNum(), sortedTables.size()); i++) {
                Document document = new Document();
                document.setTableName(sortedTables.get(i));
                document.setOriginType(RetrieveOriginType.FIELD.name());
                document.setScore(1.0 - i * chatProperties.getKvIndexFieldRetrieveScoreDecrease());
                docs.add(document);
            }
        }
        return docs;
    }

    private Document convertKeyValueEntity2Document(SchemaKBKeyValueEntity entity) {
        Document document = new Document();
        document.setPageContent(entity.getKeyContent());
        document.setTableName(entity.getValueContent());
        document.setOriginType(entity.getTag());
        return document;
    }

}
