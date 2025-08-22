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

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.oceanbase.odc.common.trace.TraceDecorator;
import com.oceanbase.odc.common.util.SystemUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/8/4
 */
@Configuration
@Slf4j
public class ChatConfiguration {
    private final int CORE_NUMBER = SystemUtils.availableProcessors();

    @Bean(name = "copilotOperationExecutor")
    public ThreadPoolTaskExecutor copilotOperationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_NUMBER * 2);
        executor.setMaxPoolSize(CORE_NUMBER * 10);
        executor.setQueueCapacity(Integer.MAX_VALUE);
        executor.setThreadNamePrefix("copilot-operation-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.setTaskDecorator(new TraceDecorator<>());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        log.info("CopilotOperationExecutor initialized");
        return executor;
    }

    @Bean(name = "nl2sqlGenerateExecutor")
    public ThreadPoolTaskExecutor nl2sqlGenerateExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_NUMBER * 2);
        executor.setMaxPoolSize(CORE_NUMBER * 10);
        executor.setQueueCapacity(Integer.MAX_VALUE);
        executor.setThreadNamePrefix("nl2Sql-generate-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.setTaskDecorator(new TraceDecorator<>());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        log.info("nl2SqlOperationExecutor initialized");
        return executor;
    }

}
