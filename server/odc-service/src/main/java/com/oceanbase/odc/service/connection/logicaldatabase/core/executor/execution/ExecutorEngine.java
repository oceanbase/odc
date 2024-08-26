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

import com.oceanbase.odc.core.shared.exception.NotImplementedException;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionCallback;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionContext;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionGroup;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionUnit;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.thread.ExecutorServiceManager;

/**
 * @Author: Lebie
 * @Date: 2024/8/26 15:14
 * @Description: []
 */
public final class ExecutorEngine implements AutoCloseable{
    private final ExecutorServiceManager executorServiceManager;

    private ExecutorEngine(final int executorSize) {
        executorServiceManager = new ExecutorServiceManager(executorSize);
    }

    public <T extends ExecutionUnit, R> R execute(ExecutionContext<T> context, ExecutionCallback<T, R> callback) {
        throw new NotImplementedException();
    }

    @Override
    public void close() throws Exception {

    }
}
