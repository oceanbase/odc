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
package com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/8/28 17:46
 * @Description: []
 */
@Slf4j
@Getter
public class ExecutionUnit<T, R> {
    private final String id;
    private final Long order;
    private final ExecutionCallback<T, R> callback;
    private final T input;

    public ExecutionUnit(String id, Long order, ExecutionCallback<T, R> executionCallback, T input) {
        this.id = id;
        this.order = order;
        this.callback = executionCallback;
        this.input = input;
    }

    public void execute(ExecutionGroupContext<T, R> context) {
        try {
            ExecutionResult<R> result = callback.execute(context);
            log.info("ExecutionUnit execute success, id: {}, result: {}", id, result);
            context.setExecutionResult(id, result);
        } catch (Exception e) {
            log.warn("ExecutionUnit execute failed, id: {}", id, e);
        }
    }

    public void terminate(ExecutionGroupContext<T, R> context) {
        try {
            ExecutionResult<R> result = context.getExecutionResult(id);
            synchronized (result) {
                if (result.getStatus() == ExecutionStatus.RUNNING) {
                    callback.terminate(context);
                    context.getExecutionResult(id).setStatus(ExecutionStatus.TERMINATED);
                }
            }
        } catch (Exception e) {
            log.warn("ExecutionUnit terminate failed, id: {}", id, e);
            context.getExecutionResult(id).setStatus(ExecutionStatus.FAILED);
        }
    }

    public void skip(ExecutionGroupContext<T, R> context) {
        ExecutionResult<R> result = context.getExecutionResult(id);
        synchronized (result) {
            if (result.getStatus() == ExecutionStatus.FAILED || result.getStatus() == ExecutionStatus.TERMINATED) {
                result.setStatus(ExecutionStatus.SKIPPED);
            }
        }
    }

}
