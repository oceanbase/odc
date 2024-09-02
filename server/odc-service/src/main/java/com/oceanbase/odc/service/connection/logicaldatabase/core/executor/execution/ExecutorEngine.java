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

package com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution;

import java.util.List;
import java.util.concurrent.Executors;

import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionCallback;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionGroup;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionGroupContext;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionResult;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionUnit;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.thread.ExecutorServiceManager;

/**
 * @Author: Lebie
 * @Date: 2024/8/26 15:14
 * @Description: []
 */
public final class ExecutorEngine<T, R> implements AutoCloseable {
    private final ExecutorServiceManager groupExecutorServiceManager;

    public ExecutorEngine(int executorSize) {
        this.groupExecutorServiceManager = new ExecutorServiceManager(executorSize);
    }

    public ExecutionGroupContext<T, R> execute(List<ExecutionGroup<T, R>> groups) {
        PreConditions.notEmpty(groups, "groups");
        ExecutionGroupContext<T, R> executionContext =
                new ExecutionGroupContext<>(groups, this.groupExecutorServiceManager.getExecutorService());
        Executors.newSingleThreadExecutor().submit(() -> executionContext.init());
        return executionContext;
    }

    @Override
    public void close() throws Exception {
        this.groupExecutorServiceManager.close();
    }

}
