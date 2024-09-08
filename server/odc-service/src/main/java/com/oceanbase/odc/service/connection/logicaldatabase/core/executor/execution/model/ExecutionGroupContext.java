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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/8/28 17:29
 * @Description: []
 */
@Slf4j
public final class ExecutionGroupContext<T, R> {

    private final Collection<ExecutionGroup<T, R>> executionGroups;

    private final Map<String, ExecutionUnit<T, R>> id2ExecutionUnit;

    private final ExecutorService executorService;

    private volatile Map<String, ExecutionResult<R>> executionId2Result;

    @Getter
    private int completedGroupCount = 0;

    public ExecutionGroupContext(Collection<ExecutionGroup<T, R>> executionGroups, ExecutorService executorService) {
        this.executionGroups = executionGroups;
        this.executorService = executorService;
        this.executionId2Result = new ConcurrentHashMap<>();
        executionGroups.stream().flatMap(group -> group.getExecutionUnits().stream())
                .forEach(unit -> executionId2Result.put(unit.getId(), new ExecutionResult<>(ExecutionStatus.PENDING,
                        unit.getOrder())));
        this.id2ExecutionUnit = executionGroups.stream().flatMap(group -> group.getExecutionUnits().stream())
                .collect(Collectors.toMap(ExecutionUnit::getId, unit -> unit));
    }

    public void execute() throws InterruptedException {
        for (ExecutionGroup<T, R> group : executionGroups) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                throw new InterruptedException();
            }
            group.execute(this.executorService, this);
            completedGroupCount++;
        }
    }

    public ExecutionResult<R> getExecutionResult(String executionId) {
        return executionId2Result.get(executionId);
    }

    public void setExecutionResult(String executionId, ExecutionResult<R> result) {
        synchronized (result) {
            executionId2Result.put(executionId, result);
        }
    }

    public boolean isCompleted() {
        log.info("completedGroupCount={},executionId2Result={}", completedGroupCount, executionId2Result);
        return executionId2Result.values().stream().allMatch(ExecutionResult::isCompleted);
    }

    public Map<String, ExecutionResult<R>> getResults() {
        log.info("executionId2Result={}", executionId2Result);
        return executionId2Result;
    }

    public void terminate(String executionUnitId) {
        try {
            ExecutionUnit<T, R> executionUnit = id2ExecutionUnit.get(executionUnitId);
            executionUnit.terminate(this);
        } catch (Exception ex) {
            log.warn("ExecutionUnit terminate failed, id={}", executionUnitId, ex);
        }

    }

    public void terminateAll() {
        for (ExecutionUnit<T, R> unit : id2ExecutionUnit.values()) {
            try {
                unit.terminate(this);
            } catch (Exception ex) {
                log.warn("ExecutionUnit terminate failed, id={}", unit.getId(), ex);
            }
        }
    }

    public void skip(String executionUnitId) {
        ExecutionUnit<T, R> executionUnit = id2ExecutionUnit.get(executionUnitId);
        executionUnit.skip(this);
    }
}

