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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.flowable.engine.impl.bpmn.parser.factory.AbstractBehaviorFactory;
import org.flowable.engine.impl.bpmn.parser.factory.DefaultActivityBehaviorFactory;
import org.flowable.engine.impl.bpmn.parser.factory.DefaultListenerFactory;
import org.flowable.job.service.impl.asyncexecutor.DefaultAsyncJobExecutor;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.service.flow.BeanInjectedClassDelegateFactory;
import com.oceanbase.odc.service.flow.OdcAsyncJobExecutor;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link org.springframework.context.annotation.Configuration} for {@code Flowable}
 *
 * @author yh263208
 * @date 2022-01-11 16:52
 * @since ODC_release_3.3.0
 */
public abstract class BaseFlowableConfiguration {

    protected static final int POOL_SIZE = 50;
    protected static final int MAX_CONCURRENT_SIZE = POOL_SIZE - 10;
    protected static final int MIN_CONCURRENT_SIZE = 16;
    @Autowired
    private FlowInstanceRepository flowInstanceRepository;
    @Autowired
    private ServiceTaskInstanceRepository serviceRepository;

    @Bean
    public SpringProcessEngineConfiguration springProcessEngineConfiguration(
            @Autowired @Qualifier("metadbTransactionManager") PlatformTransactionManager platformTransactionManager,
            DataSource dataSource) {
        SpringProcessEngineConfiguration processEngineCfg =
                new OdcProcessEngineConfiguration(flowInstanceRepository, serviceRepository,
                        MAX_CONCURRENT_SIZE, MIN_CONCURRENT_SIZE);
        processEngineCfg.setAsyncExecutorAsyncJobAcquisitionEnabled(false)
                .setDataSource(dataSource)
                .setCreateDiagramOnDeploy(false)
                .setAsyncExecutorActivate(true);
        processEngineCfg.setAsyncExecutorNumberOfRetries(0);
        processEngineCfg.setTransactionManager(platformTransactionManager);
        return processEngineCfg;
    }

    @Slf4j
    public static class OdcProcessEngineConfiguration extends SpringProcessEngineConfiguration {

        private final static long KEEP_ALIVE_TIME = 5000L;
        private final static String DEFAULT_THREAD_POOL_NAMING_PATTERN =
                "flowable-default-async-job-executor-thread-%d";
        private final static String CUSTOM_THREAD_POOL_NAMING_PATTERN = "flowable-custom-async-job-executor-thread-%d";
        private final int maxConcurrentSize;
        private final int minConcurrentSize;
        private final FlowInstanceRepository flowInstanceRepository;
        private final ServiceTaskInstanceRepository serviceRepository;

        public OdcProcessEngineConfiguration(@NonNull FlowInstanceRepository flowInstanceRepository,
                @NonNull ServiceTaskInstanceRepository serviceTaskRepository, int maxConcurrentSize,
                int minConcurrentSize) {
            this.flowInstanceRepository = flowInstanceRepository;
            this.serviceRepository = serviceTaskRepository;
            if (maxConcurrentSize <= minConcurrentSize) {
                throw new IllegalArgumentException("Illegal concurrent task size");
            }
            this.maxConcurrentSize = maxConcurrentSize;
            this.minConcurrentSize = minConcurrentSize;
        }

        @Override
        public void initListenerFactory() {
            if (listenerFactory == null) {
                DefaultListenerFactory factory = new DefaultListenerFactory(new BeanInjectedClassDelegateFactory());
                factory.setExpressionManager(expressionManager);
                listenerFactory = factory;
            } else if ((listenerFactory instanceof AbstractBehaviorFactory)
                    && ((AbstractBehaviorFactory) listenerFactory).getExpressionManager() == null) {
                ((AbstractBehaviorFactory) listenerFactory).setExpressionManager(expressionManager);
            }
        }

        @Override
        public void initBehaviorFactory() {
            if (activityBehaviorFactory == null) {
                DefaultActivityBehaviorFactory factory =
                        new DefaultActivityBehaviorFactory(new BeanInjectedClassDelegateFactory());
                factory.setExpressionManager(expressionManager);
                activityBehaviorFactory = factory;
            } else if ((activityBehaviorFactory instanceof AbstractBehaviorFactory)
                    && ((AbstractBehaviorFactory) activityBehaviorFactory).getExpressionManager() == null) {
                ((AbstractBehaviorFactory) activityBehaviorFactory).setExpressionManager(expressionManager);
            }
        }

        @Override
        public void initAsyncExecutor() {
            if (asyncExecutor == null) {
                OdcAsyncJobExecutor asyncExecutor = new OdcAsyncJobExecutor(flowInstanceRepository, serviceRepository);
                if (asyncExecutorExecuteAsyncRunnableFactory != null) {
                    asyncExecutor.setExecuteAsyncRunnableFactory(asyncExecutorExecuteAsyncRunnableFactory);
                }
                // Message queue mode
                asyncExecutor.setMessageQueueMode(asyncExecutorMessageQueueMode);
                // Thread pool config
                asyncExecutor.setCorePoolSize(asyncExecutorCorePoolSize);
                asyncExecutor.setMaxPoolSize(asyncExecutorMaxPoolSize);
                asyncExecutor.setKeepAliveTime(asyncExecutorThreadKeepAliveTime);
                // Threadpool queue
                if (asyncExecutorThreadPoolQueue != null) {
                    asyncExecutor.setThreadPoolQueue(asyncExecutorThreadPoolQueue);
                }
                asyncExecutor.setQueueSize(asyncExecutorThreadPoolQueueSize);
                // Thread flags
                asyncExecutor.setAsyncJobAcquisitionEnabled(isAsyncExecutorAsyncJobAcquisitionEnabled);
                asyncExecutor.setTimerJobAcquisitionEnabled(isAsyncExecutorTimerJobAcquisitionEnabled);
                asyncExecutor.setResetExpiredJobEnabled(isAsyncExecutorResetExpiredJobsEnabled);
                // Acquisition wait time
                asyncExecutor.setDefaultTimerJobAcquireWaitTimeInMillis(asyncExecutorDefaultTimerJobAcquireWaitTime);
                asyncExecutor.setDefaultAsyncJobAcquireWaitTimeInMillis(asyncExecutorDefaultAsyncJobAcquireWaitTime);
                // Queue full wait time
                asyncExecutor.setDefaultQueueSizeFullWaitTimeInMillis(asyncExecutorDefaultQueueSizeFullWaitTime);
                // Job locking
                asyncExecutor.setTimerLockTimeInMillis(asyncExecutorTimerLockTimeInMillis);
                asyncExecutor.setAsyncJobLockTimeInMillis(asyncExecutorAsyncJobLockTimeInMillis);
                if (asyncExecutorLockOwner != null) {
                    asyncExecutor.setLockOwner(asyncExecutorLockOwner);
                }
                // Reset expired
                asyncExecutor.setResetExpiredJobsInterval(asyncExecutorResetExpiredJobsInterval);
                asyncExecutor.setResetExpiredJobsPageSize(asyncExecutorResetExpiredJobsPageSize);
                // Shutdown
                asyncExecutor.setSecondsToWaitOnShutdown(asyncExecutorSecondsToWaitOnShutdown);
                ThreadPoolExecutor customExecutor = customExecutorService();
                asyncExecutor.setMockDataExecutorService(customExecutor);
                asyncExecutor.setLoaderDumperExecutorService(customExecutor);
                this.asyncExecutor = asyncExecutor;
            }
            asyncExecutor.setJobServiceConfiguration(jobServiceConfiguration);
            asyncExecutor.setAutoActivate(asyncExecutorActivate);
            jobServiceConfiguration.setAsyncExecutor(asyncExecutor);
            DefaultAsyncJobExecutor jobExecutor = (DefaultAsyncJobExecutor) asyncExecutor;
            jobExecutor.setExecutorService(defaultExecutorService());
            // 由于任务执行器的该参数位 int 类型，有最大值限制，因此只能设置任务超期时间最大为 24 天
            jobExecutor.setAsyncJobLockTimeInMillis(Integer.MAX_VALUE);
        }

        private ThreadPoolExecutor defaultExecutorService() {
            int corePoolSize = getCorePoolSize(Runtime.getRuntime().availableProcessors() * 5, 3f);
            log.info("Init default executor core pool size, corePoolSize={}", corePoolSize);
            return generateThreadPoolExecutor(DEFAULT_THREAD_POOL_NAMING_PATTERN, corePoolSize);
        }

        private ThreadPoolExecutor customExecutorService() {
            int corePoolSize = getCorePoolSize(Runtime.getRuntime().availableProcessors(), 2f);
            log.info("Init custom executor core pool size, corePoolSize={}", corePoolSize);
            return generateThreadPoolExecutor(CUSTOM_THREAD_POOL_NAMING_PATTERN, corePoolSize);
        }

        private ThreadPoolExecutor generateThreadPoolExecutor(String namePattern, int corePoolSize) {
            BasicThreadFactory threadFactory = new BasicThreadFactory.Builder().namingPattern(namePattern).build();
            BlockingQueue<Runnable> threadPoolQueue = new LinkedBlockingDeque<>(corePoolSize);
            return new ThreadPoolExecutor(corePoolSize, corePoolSize + 8, KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS,
                    threadPoolQueue, threadFactory);
        }

        private int getCorePoolSize(int size, float proportion) {
            int min = Math.round(minConcurrentSize * proportion);
            if (min <= 1) {
                min = 2;
            }
            int max = Math.round(maxConcurrentSize * proportion);
            if (max <= min) {
                max = min + 1;
            }
            return Math.min(Math.max(size, min), max);
        }
    }

}
