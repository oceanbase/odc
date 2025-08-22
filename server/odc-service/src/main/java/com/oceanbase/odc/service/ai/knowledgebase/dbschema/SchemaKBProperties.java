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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/7/21
 */
@Configuration
@Data
@RefreshScope
public class SchemaKBProperties {

    @Value("${odc.ai.kb.enable-build-kb:true}")
    private boolean enableBuildKB;

    @Value("${odc.ai.kb.build-kb-fixed-delay-seconds:3600}")
    private int buildKBFixedDelaySeconds;

    @Value("${odc.ai.kb.build-kb-concurrent:10}")
    private int buildKBConcurrent;

    @Value("${odc.ai.kb.embedding-document-concurrent:10}")
    private int embeddingDocumentConcurrent;

    @Value("${odc.ai.kb.embedding-batch-size:8}")
    private int embeddingBatchSize;

    @Value("${odc.ai.kb.index-expire-interval-hours:24}")
    private int indexExpireIntervalHours;

    @Value("${odc.copilot.retrieve.kv.max-key-length:128}")
    private Integer kvIndexMaxKeyLength;

    @Value("${odc.copilot.retrieve.kv.max-value-length:4096}")
    private Integer kvIndexMaxValueLength;

}
