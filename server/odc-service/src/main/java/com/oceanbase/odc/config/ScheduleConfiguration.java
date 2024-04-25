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
package com.oceanbase.odc.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.oceanbase.odc.common.trace.TraceDecorator;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.config.SystemConfigService;
import com.oceanbase.odc.service.datasecurity.SensitiveColumnScanningResultCache;
import com.oceanbase.odc.service.db.schema.syncer.DBSchemaSyncProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yizhou.xw
 * @version : ScheduleConfiguration.java, v 0.1 2021-07-28 15:06
 */
@Slf4j
@Configuration
public class ScheduleConfiguration {

    private final int CORE_NUMBER = SystemUtils.availableProcessors();

    private final long REFRESH_CONFIG_RATE_MILLIS = 3 * 60 * 1000L;

    private static final int SHORT_VALIDATE_INTERVAL_MS = 10 * 1000;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private DBSchemaSyncProperties dbSchemaSyncProperties;

    @Bean(name = "connectionStatusCheckExecutor")
    public ThreadPoolTaskExecutor connectionStatusCheckExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_NUMBER * 2);
        executor.setMaxPoolSize(CORE_NUMBER * 10);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("connection-status-check-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.setTaskDecorator(new TraceDecorator<>());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        log.info("connectionStatusCheckExecutor initialized");
        return executor;
    }

    @Bean(name = "authorizationFacadeExecutor")
    public ThreadPoolTaskExecutor authorizationFacadeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int poolSize = Math.max(SystemUtils.availableProcessors(), 5);
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("authorization-calculator-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.setTaskDecorator(new TraceDecorator<>());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        log.info("authorizationFacadeExecutor initialized");
        return executor;
    }

    @Bean(name = "loaderdumperExecutor")
    public ThreadPoolTaskExecutor loaderdumperExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int poolSize = Math.max(SystemUtils.availableProcessors(), 5);
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("loader-dumper-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.setTaskDecorator(new TraceDecorator<>());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        log.info("loaderdumperExecutor initialized");
        return executor;
    }

    @Bean(name = "autoApprovalExecutor")
    public ThreadPoolTaskExecutor autoApprovalExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int poolSize = Math.max(SystemUtils.availableProcessors(), 5);
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setThreadNamePrefix("auto-approval-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.setTaskDecorator(new TraceDecorator<>());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        log.info("autoApprovalExecutor initialized");
        return executor;
    }

    @Bean(name = "flowTaskExecutor")
    public ThreadPoolTaskExecutor flowTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_NUMBER * 2);
        executor.setMaxPoolSize(CORE_NUMBER * 10);
        executor.setThreadNamePrefix("flow-task-executor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.setTaskDecorator(new TraceDecorator<>());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        log.info("flowTaskExecutor initialized");
        return executor;
    }

    @Bean(name = "shadowTableComparingExecutor")
    public ThreadPoolTaskExecutor shadowTableComparingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int poolSize = Math.max(SystemUtils.availableProcessors(), 5);
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("shadowtable-comparing-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.setTaskDecorator(new TraceDecorator<>());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        log.info("shadowTableComparingExecutor initialized");
        return executor;
    }

    @Bean(name = "shadowTableSyncTaskExecutor")
    public ThreadPoolTaskExecutor shadowTableSyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int poolSize = Math.max(SystemUtils.availableProcessors(), 5);
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("shadowtable-sync-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.setTaskDecorator(new TraceDecorator<>());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        log.info("shadowTableSyncTaskExecutor initialized");
        return executor;
    }

    @Bean(name = "cloudLoadDataTaskExecutor")
    public ThreadPoolTaskExecutor cloudLoadDataTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int poolSize = Math.max(SystemUtils.availableProcessors(), 5);
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("cloud-load-data-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.setTaskDecorator(new TraceDecorator<>());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        log.info("cloudLoadDataTaskExecutor initialized");
        return executor;
    }

    @Bean(name = "syncDatabaseTaskExecutor")
    public ThreadPoolTaskExecutor syncDatabaseTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int poolSize = Math.max(SystemUtils.availableProcessors() * 8, 64);
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("database-sync-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.setTaskDecorator(new TraceDecorator<>());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        log.info("syncDatabaseTaskExecutor initialized");
        return executor;
    }

    @Bean(name = "scanSensitiveColumnExecutor")
    public ThreadPoolTaskExecutor scanSensitiveColumnExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int poolSize = Math.max(SystemUtils.availableProcessors(), 5);
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setThreadNamePrefix("sensitive-column-scan-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.setTaskDecorator(new TraceDecorator<>());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        log.info("scanSensitiveColumnExecutor initialized");
        return executor;
    }

    @Bean(name = "syncDBSchemaTaskExecutor")
    public ThreadPoolTaskExecutor syncDBSchemaTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(dbSchemaSyncProperties.getExecutorThreadCount());
        executor.setMaxPoolSize(dbSchemaSyncProperties.getExecutorThreadCount());
        executor.setThreadNamePrefix("database-schema-sync-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.setTaskDecorator(new TraceDecorator<>());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        log.info("syncDBSchemaTaskExecutor initialized");
        return executor;
    }

    @Lazy
    @Bean(name = "taskResultPublisherExecutor")
    public ThreadPoolTaskExecutor taskResultPublisherExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int corePoolSize = Math.max(SystemUtils.availableProcessors() * 2, 8);
        int MaxPoolSize = Math.max(SystemUtils.availableProcessors() * 8, 64);
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(MaxPoolSize);
        executor.setThreadNamePrefix("task-result-publish-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.setTaskDecorator(new TraceDecorator<>());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        log.info("taskResultPublisherExecutor initialized");
        return executor;
    }

    @Lazy
    @Bean(name = "taskFrameworkMonitorExecutor")
    public ThreadPoolTaskExecutor taskFrameworkMonitorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setThreadNamePrefix("task-framework-monitoring-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.setTaskDecorator(new TraceDecorator<>());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        log.info("taskFrameworkMonitorExecutor initialized");
        return executor;
    }

    @Scheduled(fixedDelay = REFRESH_CONFIG_RATE_MILLIS)
    public void refreshSysConfig() {
        systemConfigService.refresh();
    }

    @Scheduled(fixedRate = SHORT_VALIDATE_INTERVAL_MS)
    public void clearExpiredTask() {
        SensitiveColumnScanningResultCache.getInstance().clearExpiredTaskInfo();
    }

}
