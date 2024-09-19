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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/8/28 17:29
 * @Description: []
 */
@Slf4j
public final class ExecutionGroupContext<Input, Result> {
    @Getter
    private final Collection<ExecutionGroup<Input, Result>> executionGroups;

    private final ConcurrentMap<String, ExecutionSubGroupUnit<Input, Result>> id2ExecutionUnit;

    private final ExecutorService executorService;

    private ConcurrentMap<String, ExecutionResult<Result>> executionId2Result;

    @Getter
    private final List<Throwable> throwables;

    @Getter
    private int completedGroupCount = 0;

    public ExecutionGroupContext(Collection<ExecutionGroup<Input, Result>> executionGroups,
            ExecutorService executorService) {
        this.executionGroups = executionGroups;
        this.executorService = executorService;
        this.executionId2Result = new ConcurrentHashMap<>();
        executionGroups.stream().flatMap(group -> group.getExecutionUnits().stream())
                .forEach(unit -> executionId2Result.put(unit.getId(), new ExecutionResult<>(ExecutionStatus.PENDING,
                        unit.getOrder())));
        this.id2ExecutionUnit = executionGroups.stream().flatMap(group -> group.getExecutionUnits().stream())
                .collect(Collectors.toConcurrentMap(unit -> unit.getId(), unit -> unit));
        this.throwables = new ArrayList<>();
    }

    public synchronized void increaseCompletedGroupCount() {
        completedGroupCount++;
    }

    public ExecutionResult<Result> getExecutionResult(String executionId) {
        return executionId2Result.get(executionId);
    }

    public void setExecutionResult(String executionId,
            BiFunction<String, ExecutionResult<Result>, ExecutionResult<Result>> result) {
        executionId2Result.compute(executionId, result);
    }

    public boolean isCompleted() {
        return executionId2Result.values().stream().allMatch(ExecutionResult::isCompleted);
    }

    public Map<String, ExecutionResult<Result>> getResults() {
        return executionId2Result;
    }

    public ExecutionSubGroupUnit<Input, Result> getExecutionUnit(String executionId) {
        return id2ExecutionUnit.get(executionId);
    }

    public List<ExecutionSubGroupUnit<Input, Result>> listAllExecutionUnits() {
        return new ArrayList<>(id2ExecutionUnit.values());
    }

    public void addThrowable(Throwable throwable) {
        this.throwables.add(throwable);
    }
}

