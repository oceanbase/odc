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

import java.sql.SQLException;

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

    public void beforeExecute(ExecutionGroupContext<T, R> context) {
        try {
            context.setExecutionResult(id, (k, v) -> callback.beforeExecute(context));
        } catch (Exception e) {
            log.warn("ExecutionUnit execute failed, executionId={}, ex=", id, e);
        }
    }

    public void execute(ExecutionGroupContext<T, R> context) {
        context.setExecutionResult(id, (k, v) -> {
            if (v.getStatus() != ExecutionStatus.PENDING) {
                throw new IllegalStateException(
                        "ExecutionUnit is not in PENDING status, executionId=" + id + ", status=" + v.getStatus());
            }
            log.info("ExecutionUnit starts to execute, executionId={}", id);
            v.setStatus(ExecutionStatus.RUNNING);
            return v;
        });
        context.setExecutionResult(id, (k, v) -> {
            try {
                if (v.getStatus() == ExecutionStatus.RUNNING) {
                    ExecutionResult<R> result = callback.execute(context);
                    log.info("ExecutionUnit execute success, executionId={}", id);
                    return result;
                }
                log.warn("Abort to execute, as the ExecutionUnit({}) is not in RUNNING status, executionId={}",
                        v.getStatus(), id);
                return v;
            } catch (SQLException e) {
                log.warn("ExecutionUnit execute failed, executionId={}, ex=", id, e);
                v.setStatus(ExecutionStatus.FAILED);
                return v;
            }
        });
        log.info("ExecutionUnit execute success, executionId={}", id);
    }

    public void terminate(ExecutionGroupContext<T, R> context) {
        context.setExecutionResult(id, (k, v) -> {
            if (v.getStatus() == ExecutionStatus.RUNNING) {
                try {
                    callback.terminate(context);
                    v.setStatus(ExecutionStatus.TERMINATED);
                } catch (Exception e) {
                    log.warn("ExecutionUnit terminate failed, executionId={}", id, e);
                }
            }
            return v;
        });
    }

    public void skip(ExecutionGroupContext<T, R> context) {
        context.setExecutionResult(id, (k, v) -> {
            if (v.getStatus() == ExecutionStatus.FAILED || v.getStatus() == ExecutionStatus.TERMINATED) {
                v.setStatus(ExecutionStatus.SKIPPED);
            }
            return v;
        });
    }

}
