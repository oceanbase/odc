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
package com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

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

    private static final ExecutorService SHUTDOWN_EXECUTOR = Executors.newSingleThreadExecutor(build("closer"));

    private final ExecutorService executorService;


    public ExecutorServiceManager(int executorSize, String nameFormat) {
        executorService = getExecutorService(executorSize, nameFormat);
    }

    public ExecutorServiceManager(int executorSize) {
        this(executorSize, DEFAULT_NAME_FORMAT);
    }

    private ExecutorService getExecutorService(int executorSize, String nameFormat) {
        return 0 == executorSize ? Executors.newCachedThreadPool(build(nameFormat))
                : Executors.newFixedThreadPool(executorSize, build(nameFormat));
    }

    public void close() {
        SHUTDOWN_EXECUTOR.execute(() -> {
            try {
                executorService.shutdown();
                while (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private static ThreadFactory build(String nameFormat) {
        return new ThreadFactoryBuilder().setDaemon(true).setNameFormat(NAME_FORMAT_PREFIX + nameFormat).build();
    }
}