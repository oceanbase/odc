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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/8/28 17:46
 * @Description: []
 */
@Slf4j
@Getter
public class ExecutionSubGroupUnit<Input, Result> {
    private final String id;
    private final Long order;
    private final ExecutionHandler<Input, Result> handler;
    private final Input input;

    public ExecutionSubGroupUnit(String id, Long order, ExecutionHandler<Input, Result> executionCallback,
            Input input) {
        this.id = id;
        this.order = order;
        this.handler = executionCallback;
        this.input = input;
    }

    public void beforeExecute(ExecutionGroupContext<Input, Result> context) {
        try {
            context.setExecutionResult(id, (k, v) -> handler.beforeExecute(context));
        } catch (Exception e) {
            log.warn("ExecutionUnit execute failed, executionId={}, ex=", id, e);
        }
    }

    public void execute(ExecutionGroupContext<Input, Result> context) {
        context.setExecutionResult(id, (k, v) -> {
            if (v.getStatus() != ExecutionStatus.PENDING) {
                throw new IllegalStateException(
                        "Cannot execute because ExecutionUnit is not in PENDING status, executionId=" + id + ", status="
                                + v.getStatus());
            }
            log.info("ExecutionUnit starts to execute, executionId={}", id);
            v.setStatus(ExecutionStatus.RUNNING);
            return v;
        });
        if (context.getExecutionResult(id).getStatus() == ExecutionStatus.RUNNING) {
            try {
                ExecutionResult<Result> result = handler.execute(context);
                log.info("ExecutionUnit execute done, executionId={}", id);
                context.setExecutionResult(id, (k, v) -> {
                    if (v.getStatus() == ExecutionStatus.RUNNING) {
                        return result;
                    }
                    result.setStatus(v.getStatus());
                    return result;
                });
            } catch (Exception e) {
                log.warn("ExecutionUnit execute failed, executionId={}, ex=", id, e);
                context.setExecutionResult(id, (k, v) -> {
                    if (v.getStatus() == ExecutionStatus.RUNNING) {
                        v.setStatus(ExecutionStatus.FAILED);
                    }
                    return v;
                });
            }
        }
    }

    public void terminate(ExecutionGroupContext<Input, Result> context) {
        context.setExecutionResult(id, (k, v) -> {
            if (v.getStatus() != ExecutionStatus.RUNNING) {
                throw new IllegalStateException(
                        "Cannot terminate because ExecutionUnit is not in RUNNING status, executionId=" + id
                                + ", status=" + v.getStatus());
            }
            log.info("ExecutionUnit starts to terminate, executionId={}", id);
            v.setStatus(ExecutionStatus.TERMINATING);
            return v;
        });
        if (context.getExecutionResult(id).getStatus() == ExecutionStatus.TERMINATING) {
            try {
                handler.terminate(context);
                context.setExecutionResult(id, (k, v) -> {
                    if (v.getStatus() == ExecutionStatus.TERMINATING) {
                        log.info("ExecutionUnit terminate done, executionId={}", id);
                        v.setStatus(ExecutionStatus.TERMINATED);
                    }
                    return v;
                });
            } catch (Exception ex) {
                log.warn("ExecutionUnit terminate failed, executionId={}, ex=", id, ex);
                context.setExecutionResult(id, (k, v) -> {
                    if (v.getStatus() == ExecutionStatus.TERMINATING) {
                        v.setStatus(ExecutionStatus.TERMINATE_FAILED);
                    }
                    return v;
                });
            }
        }
    }

    public void skip(ExecutionGroupContext<Input, Result> context) {
        context.setExecutionResult(id, (k, v) -> {
            if (v.getStatus() == ExecutionStatus.FAILED || v.getStatus() == ExecutionStatus.TERMINATED
                    || v.getStatus() == ExecutionStatus.TERMINATE_FAILED) {
                log.info("ExecutionUnit starts to skip, executionId={}", id);
                v.setStatus(ExecutionStatus.SKIPPING);
                log.info("ExecutionUnit skip done, executionId={}", id);
                v.setStatus(ExecutionStatus.SKIPPED);
            }
            return v;
        });
    }
}
