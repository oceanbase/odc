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
package com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.oceanbase.odc.common.concurrent.ExecutorUtils;

import lombok.Getter;

/**
 * @Author: Lebie
 * @Date: 2024/8/26 21:58
 * @Description: []
 */
@Getter
public final class ExecutorServiceManager {

    private static final String DEFAULT_NAME_FORMAT = "%d";
    private static final String NAME_FORMAT_PREFIX = "default-executor-";

    private static final ExecutorService SHUTDOWN_EXECUTOR = Executors.newSingleThreadExecutor(build("closer-"));

    private final ExecutorService executorService;


    public ExecutorServiceManager(int executorSize, String nameFormatPrefix) {
        executorService = getExecutorService(executorSize, nameFormatPrefix);
    }

    private ExecutorService getExecutorService(int executorSize, String nameFormatPrefix) {
        return 0 == executorSize ? Executors.newCachedThreadPool(build(nameFormatPrefix))
                : Executors.newFixedThreadPool(executorSize, build(nameFormatPrefix));
    }

    public void close() {
        SHUTDOWN_EXECUTOR.execute(() -> {
            ExecutorUtils.gracefulShutdown(executorService, NAME_FORMAT_PREFIX, 5);
        });
    }

    private static ThreadFactory build(String nameFormatPrefix) {
        return new ThreadFactoryBuilder().setDaemon(true).setNameFormat(nameFormatPrefix + "%d").build();
    }
}
