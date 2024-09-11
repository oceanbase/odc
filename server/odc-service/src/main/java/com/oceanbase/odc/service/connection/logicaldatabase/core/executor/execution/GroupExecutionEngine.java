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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.oceanbase.odc.common.concurrent.ExecutorUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.thread.ExecutorServiceManager;

/**
 * @Author: Lebie
 * @Date: 2024/8/26 15:14
 * @Description: []
 */
public final class GroupExecutionEngine<Input, Result> implements AutoCloseable {
    private final ExecutorServiceManager groupExecutorServiceManager;
    private ExecutorService daemonExecutorService;

    public GroupExecutionEngine(int executorSize) {
        this.groupExecutorServiceManager = new ExecutorServiceManager(executorSize);
    }

    public ExecutionGroupContext<Input, Result> execute(List<ExecutionGroup<Input, Result>> groups) {
        PreConditions.notEmpty(groups, "groups");
        ExecutionGroupContext<Input, Result> executionContext =
                new ExecutionGroupContext<>(groups, this.groupExecutorServiceManager.getExecutorService());
        daemonExecutorService = Executors.newSingleThreadExecutor();
        daemonExecutorService.submit(() -> {
            try {
                executionContext.execute();
            } catch (InterruptedException e) {
                executionContext.addThrowable(e);
                Thread.currentThread().interrupt();
            }
        });
        return executionContext;
    }

    @Override
    public void close() throws Exception {
        this.groupExecutorServiceManager.close();
        if (this.daemonExecutorService != null) {
            ExecutorUtils.gracefulShutdown(this.daemonExecutorService, "group-executor-daemon", 5);
        }
    }

}
