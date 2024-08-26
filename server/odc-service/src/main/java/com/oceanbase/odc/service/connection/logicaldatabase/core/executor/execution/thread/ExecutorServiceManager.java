/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.alibaba.ttl.threadpool.TtlExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;


import lombok.Getter;

/**
 * @Author: Lebie
 * @Date: 2024/8/26 21:58
 * @Description: []
 */
@Getter
public final class ExecutorServiceManager {

    private static final String DEFAULT_NAME_FORMAT = "%d";
    private static final String NAME_FORMAT_PREFIX = "logical-database-change-";


    private static final ThreadFactoryBuilder threadFactory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat(NAME_FORMAT_PREFIX);

    private static final ExecutorService SHUTDOWN_EXECUTOR = Executors.newSingleThreadExecutor(threadFactory.build());

    private final ExecutorService executorService;


    public ExecutorServiceManager(final int executorSize) {
        executorService = TtlExecutors.getTtlExecutorService(getExecutorService(executorSize));
    }

    private ExecutorService getExecutorService(final int executorSize) {
        return 0 == executorSize ? Executors.newCachedThreadPool(threadFactory.build()) : Executors.newFixedThreadPool(executorSize, threadFactory.build());
    }

    /**
     * Close executor service.
     */
    public void close() {
        SHUTDOWN_EXECUTOR.execute(() -> {
            try {
                executorService.shutdown();
                while (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
