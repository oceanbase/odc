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

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.oceanbase.odc.common.trace.TraceDecorator;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.common.util.ConditionalOnProperty;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/7/21
 */
@Slf4j
@Configuration
@ConditionalOnProperty(value = "odc.ai.kb.enable-build-kb", havingValues = "true")
public class SchemaKBConfiguration {
    private final int CORE_NUMBER = SystemUtils.availableProcessors();
    private final int KEEP_ALIVE_SECONDS = 60;
    @Autowired
    private SchemaKBProperties schemaKBProperties;

    @Bean(name = "submitSchemaBuildTaskExecutor")
    public ThreadPoolTaskExecutor submitSchemaBuildTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_NUMBER);
        executor.setMaxPoolSize(CORE_NUMBER);
        executor.setQueueCapacity(Integer.MAX_VALUE);
        executor.setThreadNamePrefix("schema-task-submit-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.setTaskDecorator(new TraceDecorator<>());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        executor.setAllowCoreThreadTimeOut(true);
        executor.initialize();
        log.info("submitSchemaBuildTaskExecutor initialized");
        return executor;
    }

    @Bean(name = "schemaKBBuildExecutor")
    public ThreadPoolTaskExecutor schemaKBBuildExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int corePoolSize = Math.max(CORE_NUMBER * 2, this.schemaKBProperties.getBuildKBConcurrent());
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(corePoolSize * 5);
        executor.setQueueCapacity(Integer.MAX_VALUE);
        executor.setThreadNamePrefix("schema-KB-build-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.setTaskDecorator(new TraceDecorator<>());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        executor.setAllowCoreThreadTimeOut(true);
        executor.initialize();
        log.info("schemaKBBuildExecutor initialized");
        return executor;
    }

    @Bean(name = "documentEmbeddingExecutor")
    public ThreadPoolTaskExecutor documentEmbeddingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int corePoolSize = Math.max(CORE_NUMBER * 2, this.schemaKBProperties.getEmbeddingDocumentConcurrent());
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(corePoolSize * 5);
        executor.setQueueCapacity(Integer.MAX_VALUE);
        executor.setThreadNamePrefix("document-embedding-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.setTaskDecorator(new TraceDecorator<>());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        executor.setAllowCoreThreadTimeOut(true);
        executor.initialize();
        log.info("documentEmbeddingExecutor initialized");
        return executor;
    }

}
