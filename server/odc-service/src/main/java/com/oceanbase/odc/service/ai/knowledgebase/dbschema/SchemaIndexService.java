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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.oceanbase.odc.common.util.HashUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.metadb.ai.QueryKBKeyValueParams;
import com.oceanbase.odc.metadb.ai.QueryKBVectorParams;
import com.oceanbase.odc.metadb.ai.SchemaKBKeyValueEntity;
import com.oceanbase.odc.metadb.ai.SchemaKBKeyValueRepository;
import com.oceanbase.odc.metadb.ai.SchemaKBVectorRepository;
import com.oceanbase.odc.service.ai.chat.model.RetrieveResult;
import com.oceanbase.odc.service.ai.knowledgebase.model.Document;
import com.oceanbase.odc.service.ai.knowledgebase.model.DocumentWrapper;
import com.oceanbase.odc.service.ai.knowledgebase.model.RetrieveOriginType;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.llm.sdk.EmbeddingModelWrapper;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/7/24
 */
@Component
@Slf4j
public class SchemaIndexService {

    @Autowired
    private SchemaKBKeyValueRepository keyValueRepository;
    @Autowired
    private SchemaKBVectorRepository vectorRepository;
    @Autowired(required = false)
    @Qualifier("vectordbDataSource")
    private DataSource vectordbDataSource;
    @Autowired
    private SchemaKBProperties schemaKBProperties;
    @Autowired
    private SchemaVectorService schemaVectorService;

    public void doBuildIndexes(Database database, List<SchemaIndex> schemaIndexes, EmbeddingModelWrapper model) {
        QueryKBKeyValueParams params = new QueryKBKeyValueParams();
        params.setKeys(schemaIndexes.stream().map(SchemaIndex::getName).collect(Collectors.toList()));
        params.setDatabaseId(database.getId());
        params.setTag(RetrieveOriginType.HASH.name());
        List<SchemaKBKeyValueEntity> keyValueEntities = keyValueRepository.findByKeyContentAndDatabaseIdAndTag(params);
        Verify.notNull(database.getId(), "database.id");
        this.vectorRepository.createTableIfNotExists(database.getId());

        Map<String, String> oldIndexName2HashValue = keyValueEntities.stream()
                .collect(Collectors.toMap(SchemaKBKeyValueEntity::getKeyContent,
                        SchemaKBKeyValueEntity::getValueContent,
                        (existing, replacement) -> existing));
        Map<String, String> newIndexName2HashValue = schemaIndexes.stream()
                .collect(Collectors.toMap(SchemaIndex::getName, idx -> HashUtils.md5(idx.getDefinition())));

        List<SchemaIndex> toBeInserted = schemaIndexes.stream()
                .filter(index -> !oldIndexName2HashValue.containsKey(index.getName()))
                .collect(Collectors.toList());
        List<SchemaIndex> toBeUpdated = schemaIndexes.stream().filter(index -> {
            if (!oldIndexName2HashValue.containsKey(index.getName())) {
                return false;
            }
            String name = index.getName();
            return !Objects.equals(oldIndexName2HashValue.get(name), newIndexName2HashValue.get(name));
        }).toList();
        insertVectorIndexes(toBeInserted, database, model);
        updateVectorIndexes(toBeUpdated, database, model);
        insertKeyValueIndexes(toBeInserted, database, newIndexName2HashValue);
        updateKeyValueIndexes(toBeUpdated, database, newIndexName2HashValue, oldIndexName2HashValue);
    }

    public List<String> sortTopKTable(List<RetrieveResult> docs, int topK) {
        Map<String, Double> docScoreMap = new HashMap<>();
        for (RetrieveResult retrieveResult : docs) {
            for (Document document : retrieveResult.getDocuments()) {
                String key = document.getTableName();
                docScoreMap.put(key,
                        docScoreMap.getOrDefault(key, 0.0) + document.getScore() * retrieveResult.getWeight());
            }
        }
        return docScoreMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(topK)
                .map(Map.Entry::getKey)
                .toList();
    }

    public String retrieveDDLByTableName(String tableName, Long databaseId) {
        QueryKBKeyValueParams params = new QueryKBKeyValueParams();
        params.setDatabaseId(databaseId);
        params.setKeys(Collections.singletonList(tableName));
        params.setTag(RetrieveOriginType.DDL.name());
        List<SchemaKBKeyValueEntity> keyValueEntities = keyValueRepository.findByKeyContentAndDatabaseIdAndTag(params);
        if (keyValueEntities.isEmpty()) {
            return "";
        }
        return keyValueEntities.get(0).getValueContent();
    }

    private void updateVectorIndexes(List<SchemaIndex> indexes, Database database, EmbeddingModelWrapper model) {
        if (CollectionUtils.isEmpty(indexes)) {
            log.info("No vector index to update, dbId={}", database.getId());
            return;
        }
        // 索引变更
        deleteVectorIndexes(indexes, database);
        // 避免大事务，落库操作内部完成
        insertVectorIndexes(indexes, database, model);
    }

    private void insertKeyValueIndexes(List<SchemaIndex> indexes, Database database,
            Map<String, String> idxName2HashValue) {
        for (SchemaIndex index : indexes) {
            new TransactionTemplate(new DataSourceTransactionManager(vectordbDataSource)).execute(status -> {
                try {
                    String idxContentHashValue = idxName2HashValue.get(index.getName());
                    if (StringUtils.isEmpty(idxContentHashValue)) {
                        return null;
                    }
                    int rowAdd = insertKeyValueIndex(index, database);
                    rowAdd += insertKeyValueHashIndex(index, database, idxContentHashValue);
                    log.info("Copilot build k-v index success, dbId={}, name={}, affectRowNum={}",
                            database.getId(), index.getName(), rowAdd);
                } catch (Exception e) {
                    log.warn("Failed to build k-v index, dbId={}, names={}, errMsg={}", database.getId(),
                            indexes.stream().map(SchemaIndex::getName).collect(Collectors.joining(",")),
                            e.getMessage(), e);
                    status.setRollbackOnly();
                }
                return null;
            });
        }
    }

    private void updateKeyValueIndexes(List<SchemaIndex> indexes, Database database,
            Map<String, String> newIdxName2HashValue, Map<String, String> oldIdxName2HashValue) {
        for (SchemaIndex index : indexes) {
            new TransactionTemplate(new DataSourceTransactionManager(vectordbDataSource)).execute(status -> {
                try {
                    String newIdxContentHashValue = newIdxName2HashValue.get(index.getName());
                    String oldIdcContentHashValue = oldIdxName2HashValue.get(index.getName());
                    if (StringUtils.isEmpty(newIdxContentHashValue) || StringUtils.isEmpty(
                            oldIdcContentHashValue)) {
                        return null;
                    }
                    // 索引变更
                    int rowDelete = deleteKeyValueIndex(index, database);
                    int rowAdd = insertKeyValueIndex(index, database);
                    // 哈希变更
                    rowDelete += deleteKeyValueHashIndex(database, oldIdcContentHashValue);
                    rowAdd += insertKeyValueHashIndex(index, database, newIdxContentHashValue);
                    log.info("Copilot change k-v index success, dbId={}, name={}, addAffectRowNum={}, "
                            + "dropAffectRowNum={}", database.getId(), index.getName(), rowAdd, rowDelete);
                } catch (Exception e) {
                    log.warn("Failed to change k-v index, dbId={}, names={}, errMsg={}", database.getId(),
                            indexes.stream().map(SchemaIndex::getName).collect(Collectors.joining(", ")),
                            e.getMessage(), e);
                    status.setRollbackOnly();
                }
                return null;
            });
        }
    }

    private int deleteKeyValueHashIndex(Database database, String hash) {
        QueryKBKeyValueParams queryKeyValueParams = new QueryKBKeyValueParams();
        queryKeyValueParams.setValue(hash);
        queryKeyValueParams.setDatabaseId(database.getId());
        return keyValueRepository.deleteByValueContentAndDatabaseId(queryKeyValueParams);
    }

    private int deleteVectorIndexes(List<SchemaIndex> indexes, Database database) {
        int rowAffected = 0;
        QueryKBVectorParams queryVectorParams = new QueryKBVectorParams();
        queryVectorParams.setTableNames(indexes.stream().map(SchemaIndex::getName).collect(Collectors.toList()));
        queryVectorParams.setDatabaseId(database.getId());
        rowAffected += vectorRepository.deleteByTableNameAndDatabaseId(queryVectorParams);
        log.info("Copilot delete vector index success, dbId={}, names={}, affectRowNum={}", database.getId(),
                indexes.stream().map(SchemaIndex::getName).collect(Collectors.joining(",")), rowAffected);
        return rowAffected;
    }

    private int deleteKeyValueIndex(SchemaIndex index, Database database) {
        int rowAffected = 0;
        QueryKBKeyValueParams queryKeyValueParams = new QueryKBKeyValueParams();
        queryKeyValueParams.setKeys(Collections.singletonList(index.getName()));
        queryKeyValueParams.setDatabaseId(database.getId());
        queryKeyValueParams.setTag(RetrieveOriginType.DDL.name());
        rowAffected += keyValueRepository.deleteByKeyContentAndDatabaseId(queryKeyValueParams);
        queryKeyValueParams = new QueryKBKeyValueParams();
        queryKeyValueParams.setValue(index.getName());
        queryKeyValueParams.setDatabaseId(database.getId());
        rowAffected += keyValueRepository.deleteByValueContentAndDatabaseId(queryKeyValueParams);
        return rowAffected;
    }

    private int insertKeyValueHashIndex(SchemaIndex index, Database database, String hash) {
        SchemaKBKeyValueEntity keyValueEntity = new SchemaKBKeyValueEntity();
        keyValueEntity.setKeyContent(index.getName());
        keyValueEntity.setValueContent(hash);
        keyValueEntity.setTag(RetrieveOriginType.HASH.name());
        keyValueEntity.setDatabaseId(database.getId());
        return keyValueRepository.saveAll(Collections.singletonList(keyValueEntity));
    }

    private void insertVectorIndexes(List<SchemaIndex> indices, Database database, EmbeddingModelWrapper model) {
        List<Document> documents = new ArrayList<>();
        Document name2table = new Document();
        for (SchemaIndex index : indices) {
            name2table.setTableName(index.getName());
            name2table.setPageContent(index.getVectorIndexContent());
            name2table.setOriginType(RetrieveOriginType.TABLE.name());
            documents.add(name2table);

            StringBuilder stringBuilder = new StringBuilder();
            List<String> fields = new ArrayList<>();
            for (SchemaIndex indexItem : index.getFields()) {
                String line = indexItem.getVectorIndexContent();
                if (stringBuilder.length() + line.length() > model.credential().getMaxToken()
                        && !stringBuilder.isEmpty()) {
                    fields.add(stringBuilder.toString());
                    stringBuilder.setLength(0);
                }
                stringBuilder.append(line).append('_');
            }
            if (!stringBuilder.isEmpty()) {
                fields.add(stringBuilder.toString());
            }
            fields.forEach(fieldConcatStr -> {
                Document field2table = new Document();
                field2table.setTableName(index.getName());
                field2table.setPageContent(fieldConcatStr);
                field2table.setOriginType(RetrieveOriginType.FIELD.name());
                documents.add(field2table);
            });
        }
        if (documents.isEmpty()) {
            log.info("No vector index to insert, dbId={}", database.getId());
            return;
        }
        schemaVectorService
                .embeddingDocuments(
                        documents.stream().map(d -> new DocumentWrapper(d, database, model)).toList());
    }

    private int insertKeyValueIndex(SchemaIndex index, Database database) {
        List<SchemaKBKeyValueEntity> keyValueEntities = new ArrayList<>();
        keyValueEntities.add(convertToSchemaKBKeyValueEntity(
                index.getName(), RetrieveOriginType.TABLE, index, database));
        String comment = index.getComment();
        if (StringUtils.isNotEmpty(comment)) {
            keyValueEntities.add(convertToSchemaKBKeyValueEntity(comment, RetrieveOriginType.TABLE, index, database));
        }
        for (SchemaIndex indexItem : index.getFields()) {
            keyValueEntities.add(convertToSchemaKBKeyValueEntity(
                    indexItem.getName(), RetrieveOriginType.FIELD, index, database));
            String idxComment = indexItem.getComment();
            if (StringUtils.isNotEmpty(idxComment)) {
                keyValueEntities
                        .add(convertToSchemaKBKeyValueEntity(idxComment, RetrieveOriginType.FIELD, index, database));
            }
        }
        // 把DDL也存入
        SchemaKBKeyValueEntity keyValueEntity = new SchemaKBKeyValueEntity();
        keyValueEntity.setKeyContent(index.getName());
        keyValueEntity.setValueContent(index.getDefinition());
        keyValueEntity.setDatabaseId(database.getId());
        keyValueEntity.setTag(RetrieveOriginType.DDL.name());
        keyValueEntity.setOrganizationId(database.getOrganizationId());
        keyValueEntities.add(keyValueEntity);
        // 过滤过长的内容
        List<SchemaKBKeyValueEntity> filterKeyValueEntities = keyValueEntities.stream()
                .filter(item -> item.getKeyContent().length() < schemaKBProperties.getKvIndexMaxKeyLength()
                        && item.getValueContent().length() < schemaKBProperties.getKvIndexMaxValueLength())
                .collect(Collectors.toList());
        return keyValueRepository.saveAll(filterKeyValueEntities);
    }

    private SchemaKBKeyValueEntity convertToSchemaKBKeyValueEntity(String key, RetrieveOriginType tag,
            SchemaIndex index, Database database) {
        SchemaKBKeyValueEntity keyValueEntity = new SchemaKBKeyValueEntity();
        keyValueEntity.setKeyContent(key);
        keyValueEntity.setValueContent(index.getName());
        keyValueEntity.setDatabaseId(database.getId());
        keyValueEntity.setTag(tag.name());
        keyValueEntity.setOrganizationId(database.getOrganizationId());
        return keyValueEntity;
    }

}
