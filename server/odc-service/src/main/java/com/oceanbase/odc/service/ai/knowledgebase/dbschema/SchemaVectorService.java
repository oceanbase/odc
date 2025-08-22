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
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.util.HashUtils;
import com.oceanbase.odc.metadb.ai.QueryKBVectorParams;
import com.oceanbase.odc.metadb.ai.SchemaKBVectorEntity;
import com.oceanbase.odc.metadb.ai.SchemaKBVectorRepository;
import com.oceanbase.odc.service.ai.knowledgebase.model.Document;
import com.oceanbase.odc.service.ai.knowledgebase.model.DocumentWrapper;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.llm.sdk.EmbeddingModelWrapper;
import com.oceanbase.tools.dbbrowser.util.DBSchemaAccessorUtil;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/7/31
 */
@Slf4j
@Component
public class SchemaVectorService {
    private static final long DEFAULT_COLLECT_BATCH_INTERVAL_MILLIS = 1000L;
    private static final int DEFAULT_BATCH_SIZE = 8;
    @Autowired(required = false)
    @Qualifier("documentEmbeddingExecutor")
    public ThreadPoolTaskExecutor documentEmbeddingExecutor;
    @Autowired
    private SchemaKBProperties schemaKBProperties;
    @Autowired
    private SchemaKBVectorRepository vectorRepository;

    /**
     * 执行文档向量化，并将向量化结果存储到知识库中，所有 Document 必须使用同一个 EmbeddingModel
     */
    public void embeddingDocuments(List<DocumentWrapper> documents) {
        List<List<DocumentWrapper>> partitions = ListUtils.partition(documents,
                schemaKBProperties.getEmbeddingBatchSize());
        for (List<DocumentWrapper> partition : partitions) {
            EmbeddingModel model = partition.get(0).model().model();
            List<TextSegment> segments = partition.stream().map(d -> {
                Document document = d.document();
                return new TextSegment(document.getPageContent(), new Metadata());
            }).toList();

            try {
                // 防止触发模型限流
                Thread.sleep(1000);
                long start = System.currentTimeMillis();
                Response<List<Embedding>> resp = model.embedAll(segments);
                if (log.isDebugEnabled()) {
                    log.debug("Embedding document resp: {}", resp);
                }
                List<Embedding> embeddings = resp.content();
                int rows = insertVectorEntities(partition, embeddings);
                log.info("Embedding document success, rows: {}, cost: {}ms",
                        rows, System.currentTimeMillis() - start);
            } catch (Exception e) {
                log.warn("Embedding document failed", e);
            }
        }
    }

    public List<Document> retrieveDocument(String query, Long databaseId, String tag, int topK,
            EmbeddingModelWrapper embeddingModel) {
        EmbeddingModel model = embeddingModel.model();
        List<Float> embedding = model.embed(query).content().vectorAsList();
        QueryKBVectorParams params = new QueryKBVectorParams();
        params.setEmbedding(embedding);
        params.setDatabaseId(databaseId);
        params.setTag(tag);
        params.setTopK(1000);
        List<SchemaKBVectorEntity> entities = vectorRepository.annSearchTopKByVectorAndDatabaseIdAndTag(params);
        return entities.stream().limit(topK).map(this::convertVectorEntity2Document).collect(Collectors.toList());
    }

    private int insertVectorEntities(List<DocumentWrapper> documents, List<Embedding> embeddings) {
        List<SchemaKBVectorEntity> vectorEntities = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            DocumentWrapper item = documents.get(i);
            SchemaKBVectorEntity vectorEntity = buildVectorEntity(item.document(), item.database());
            vectorEntity.setEmbedding(embeddings.get(i).vectorAsList());
            vectorEntities.add(vectorEntity);
        }
        List<Integer> affectRows = DBSchemaAccessorUtil.partitionFind(vectorEntities, 500, e -> {
            int affectRow;
            try {
                affectRow = vectorRepository.saveAll(e);
            } catch (Exception ignore) {
                affectRow = 0;
            }
            return Collections.singletonList(affectRow);
        });
        return affectRows.stream().mapToInt(Integer::intValue).sum();
    }

    private SchemaKBVectorEntity buildVectorEntity(Document document, Database database) {
        SchemaKBVectorEntity vectorEntity = new SchemaKBVectorEntity();
        vectorEntity.setDatabaseId(database.getId());
        vectorEntity.setTag(document.getOriginType());
        vectorEntity.setContent(document.getPageContent());
        vectorEntity.setContentSha1(HashUtils.sha1(document.getPageContent()));
        vectorEntity.setTableName(document.getTableName());
        vectorEntity.setOrganizationId(database.getOrganizationId());
        return vectorEntity;
    }

    private Document convertVectorEntity2Document(SchemaKBVectorEntity entity) {
        Document document = new Document();
        document.setPageContent(entity.getContent());
        document.setOriginType(entity.getTag());
        document.setTableName(entity.getTableName());
        if (entity.getDistance() < 0) {
            throw new IllegalArgumentException("The distance between vectors should not be less than zero.");
        }
        document.setScore(1 / (1 + (entity.getDistance() / 100)));
        return document;
    }

}
