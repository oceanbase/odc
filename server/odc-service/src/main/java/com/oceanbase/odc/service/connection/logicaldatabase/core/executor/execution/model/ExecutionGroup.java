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

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import com.oceanbase.odc.core.sql.execute.model.JdbcGeneralResult;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.ExecutionUtils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @Author: Lebie
 * @Date: 2024/8/28 17:29
 * @Description: []
 */
@Getter
public abstract class ExecutionGroup<T, R> {
    private final List<ExecutionUnit<T, R>> executionUnits;
    private final List<ExecutionSubGroup<T, R>> subGroups;

    public ExecutionGroup(List<ExecutionUnit<T, R>> executionUnits) {
        this.executionUnits = executionUnits;
        this.subGroups = listSubGroups(executionUnits);
    }

    protected abstract List<ExecutionSubGroup<T, R>> listSubGroups(List<ExecutionUnit<T, R>> executionUnits);

    public void execute(ExecutorService executorService, ExecutionGroupContext<T, R> context) {
        for (ExecutionSubGroup<T, R> subGroup : subGroups) {
            executorService.submit(() -> subGroup.execute(context));
        }
        waitForCompletion(context, executionUnits.stream().map(ExecutionUnit::getId).collect(Collectors.toSet()));
    }

    private void waitForCompletion(ExecutionGroupContext<T, R> context, Set<String> executionIds) {
        List<ExecutionResult<R>> results = executionIds.stream()
                .map(context::getExecutionResult).collect(Collectors.toList());
        while (!Thread.currentThread().isInterrupted()) {
            List<R> incompleteTasks = results.stream()
                    .filter(result -> !result.isCompleted())
                    .map(ExecutionResult::getResult)
                    .collect(Collectors.toList());
            if (incompleteTasks.isEmpty()) {
                break;
            }
            synchronized (incompleteTasks) {
                try {
                    incompleteTasks.wait(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
