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

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @Author: Lebie
 * @Date: 2024/8/28 17:29
 * @Description: []
 */
@RequiredArgsConstructor
@Getter
public class ExecutionGroup<R> {
    private final List<ExecutionUnit<R>> executionUnits;
    private final List<ExecutionSubGroup<R>> subGroups;

    public void execute(ExecutorService executorService, ExecutionGroupContext<R> context) {
        for (ExecutionSubGroup< R> subGroup : subGroups) {
            executorService.submit(() -> subGroup.execute(executorService, context));
        }
    }

    @Data
    @RequiredArgsConstructor
    class ExecutionSubGroup<R> {
        private final List<ExecutionUnit<R>> executionUnits;
        private final int concurrency;

        public void execute(ExecutorService executorService, ExecutionGroupContext<R> context) {
            List<List<ExecutionUnit<R>>> batches =
                    ExecutionUtils.createBatches(this.executionUnits, this.concurrency);
            for (List<ExecutionUnit<R>> batch : batches) {
                for (ExecutionUnit<R> executionUnit : batch) {
                    executorService.submit(() -> executionUnit.execute(context));
                }
                waitForCompletion(context, batch.stream().map(ExecutionUnit::getId).collect(Collectors.toSet()));
            }
        }

        private void waitForCompletion(ExecutionGroupContext<R> context, Set<String> executionIds) {
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

}
