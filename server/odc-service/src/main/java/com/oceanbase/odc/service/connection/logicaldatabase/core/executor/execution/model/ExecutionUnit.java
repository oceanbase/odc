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

package com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/8/28 17:46
 * @Description: []
 */
@Slf4j
public class ExecutionUnit<R> {
    private final String id;
    private final ExecutionCallback<R>  callback;

    public ExecutionUnit(String id, ExecutionCallback<R> executionCallback) {
        this.id = id;
        this.callback = executionCallback;
    }

    public String getId() {
        return id;
    }

    public void execute(ExecutionGroupContext<R> context) {
        try {
            ExecutionResult<R> result = callback.execute(context);
            context.setExecutionResult(id, result);
            if (result.getStatus() == ExecutionStatus.SUCCESS) {
                callback.onSuccess(this, context);
            } else {
                callback.onFailed(this, context);
            }
        } catch (Exception e) {
            log.warn("ExecutionUnit execute failed, id: {}", id, e);
            callback.onFailed(this, context);
        }
    }

    public void terminate(ExecutionGroupContext<R> context) {
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

    public void skip(ExecutionGroupContext<R> context) {
        ExecutionResult<R> result = context.getExecutionResult(id);
        synchronized (result) {
            if (result.getStatus() == ExecutionStatus.FAILED) {
                result.setStatus(ExecutionStatus.SKIPPED);
            }
        }
    }

}
