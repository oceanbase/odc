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

import java.io.Serializable;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @Author: Lebie
 * @Date: 2024/8/28 17:49
 * @Description: []
 */
@Data
public class ExecutionResult<R> implements Serializable {
    private static final long serialVersionUID = 1L;
    private R result;
    private ExecutionStatus status;

    public ExecutionResult(ExecutionStatus status) {
        this.status = status;
    }

    public ExecutionResult(R result, ExecutionStatus status) {
        this.result = result;
        this.status = status;
    }

    public boolean isCompleted() {
        return status == ExecutionStatus.SUCCESS || status == ExecutionStatus.SKIPPED;
    }
}
