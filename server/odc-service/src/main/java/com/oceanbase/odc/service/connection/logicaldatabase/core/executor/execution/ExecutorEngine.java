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

import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.core.shared.exception.NotImplementedException;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionCallback;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionContext;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionGroup;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionResult;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionUnit;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.thread.ExecutorServiceManager;

/**
 * @Author: Lebie
 * @Date: 2024/8/26 15:14
 * @Description: []
 */
public final class ExecutorEngine<T extends ExecutionUnit, R extends ExecutionResult> implements AutoCloseable {
    private final ExecutorServiceManager groupExecutorServiceManager;
    private final ExecutorServiceManager subGroupExecutorServiceManager;
    private final List<ExecutionGroup<T>> groups;
    private final ExecutionCallback<T, R> callback;

    private ExecutorEngine(List<ExecutionGroup<T>> groups, ExecutionCallback<T, R> callback) {
        this.groups = groups;
        this.callback = callback;
        this.groupExecutorServiceManager =
                new ExecutorServiceManager(groups.stream().mapToInt(ExecutionGroup::getConcurrency).max().orElse(0));
        this.subGroupExecutorServiceManager = new ExecutorServiceManager(
                groups.stream().mapToInt(ExecutionGroup::getSubGroupConcurrency).max().orElse(0));
    }

    public ExecutionContext<T, R> execute() {
        return new ExecutionContext<>(groups, callback, groupExecutorServiceManager, subGroupExecutorServiceManager);
    }

    @Override
    public void close() throws Exception {
        this.groupExecutorServiceManager.close();
        this.subGroupExecutorServiceManager.close();
    }

}
