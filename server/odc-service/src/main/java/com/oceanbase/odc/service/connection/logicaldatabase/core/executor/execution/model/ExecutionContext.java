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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.thread.ExecutorServiceManager;

/**
 * @Author: Lebie
 * @Date: 2024/8/23 17:57
 * @Description: []
 */
public final class ExecutionContext<T extends ExecutionUnit, R extends ExecutionResult> {
    private final List<ExecutionGroup<T>> groups;
    private final Map<String, ExecutionGroup<T>> id2Group;
    private final ExecutorServiceManager groupExecutorServiceManager;
    private final ExecutorServiceManager subGroupExecutorServiceManager;
    private final ExecutionCallback<T, R> callback;
    private final Map<String, T> id2ExecutionUnit;
    private volatile Map<String, R> executionUnitId2Result;
    private int currentlyExecutingGroupIndex;

    public ExecutionContext(@NotEmpty List<ExecutionGroup<T>> groups, @NotNull ExecutionCallback<T, R> callback,
            @NotNull ExecutorServiceManager groupExecutorServiceManager,
            @NotNull ExecutorServiceManager subGroupExecutorServiceManager) {
        this.groups = groups;
        this.callback = callback;
        this.id2Group = groups.stream().collect(Collectors.toMap(ExecutionGroup::getId, group -> group));
        this.groupExecutorServiceManager = groupExecutorServiceManager;
        this.subGroupExecutorServiceManager = subGroupExecutorServiceManager;
        this.executionUnitId2Result = new ConcurrentHashMap<>();
        this.id2ExecutionUnit = new HashMap<>();
        groups.stream().forEach(group -> group.listSubGroups().stream().forEach(subGroup -> subGroup.stream()
                .forEach(executionUnit -> id2ExecutionUnit.put(executionUnit.getId(), executionUnit))));
        currentlyExecutingGroupIndex = 0;
        groupExecutorServiceManager.getExecutorService().submit(() -> start(callback));
    }

    public List<ExecutionGroup<T>> listGroups() {
        return groups;
    }

    public ExecutionGroup<T> getGroup(@NotNull String id) {
        if (!id2Group.containsKey(id)) {
            throw new UnexpectedException("Execution group not found, id=" + id);
        }
        return id2Group.get(id);
    }

    public ExecutionGroup<T> getCurrentlyExecutingGroup() {
        return groups.get(currentlyExecutingGroupIndex);
    }

    public R getExecutionUnitResult(@NotNull String id) {
        return executionUnitId2Result.get(id);
    }

    public void skipExecutionUnit(@NotNull String id) {
        R result = executionUnitId2Result.get(id);
        synchronized (result) {
            if (result == null) {
                throw new UnexpectedException("Execution unit not found, id=" + id);
            }
            if (result.getStatus() == ExecutionStatus.FAILURE || result.getStatus() == ExecutionStatus.TERMINATED) {
                result.setStatus(ExecutionStatus.SKIPPED);
            }
        }
    }

    public void terminateExecutionUnit(@NotNull String id) {
        R result = executionUnitId2Result.get(id);
        synchronized (result) {
            if (result == null) {
                throw new UnexpectedException("Execution unit not found, id=" + id);
            }
            if (result.getStatus() == ExecutionStatus.FAILURE || result.getStatus() == ExecutionStatus.SKIPPED) {
                this.callback.terminate(id2ExecutionUnit.get(id));
                result.setStatus(ExecutionStatus.TERMINATED);
            }
        }
    }

    private void start(ExecutionCallback<T, R> callback) {
        if (currentlyExecutingGroupIndex != 0) {
            throw new UnexpectedException("Execution context was already started");
        }
        List<ExecutionGroup<T>> groups = listGroups();
        for (ExecutionGroup<T> group : groups) {
            List<List<T>> subGroups = group.listSubGroups();
            for (List<T> subGroup : subGroups) {

                groupExecutorServiceManager.getExecutorService().submit(() -> {
                    List<List<T>> batches = createBatches(subGroup, group.getSubGroupConcurrency());
                    for (List<T> batch : batches) {
                        List<Future<R>> batchResult = new ArrayList<>();
                        batch.stream().forEach(
                                executionUnit -> batchResult.add(subGroupExecutorServiceManager.getExecutorService()
                                        .submit(() -> callback.execute(executionUnit))));
                        List<R> results = getGroupResults(batchResult);
                        results.stream().forEach(result -> executionUnitId2Result.put(result.getId(), result));
                    }
                });
            }
            waitForGroupCompletion(
                    group.listExecutionUnits().stream().map(ExecutionUnit::getId).collect(Collectors.toSet()));
            currentlyExecutingGroupIndex++;
        }
    }

    private void waitForGroupCompletion(Set<String> executionIds) {
        List<R> results = executionIds.stream().map(this::getExecutionUnitResult).collect(Collectors.toList());
        while (true) {
            List<R> incompleteTasks = results.stream()
                    .filter(result -> !result.isCompleted())
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

    private List<R> getGroupResults(List<Future<R>> resultFutures) {
        List<R> result = new ArrayList<>();
        for (Future<R> each : resultFutures) {
            try {
                result.add(each.get());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    private List<List<T>> createBatches(List<T> tasks, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i += batchSize) {
            batches.add(tasks.subList(i, Math.min(i + batchSize, tasks.size())));
        }
        return batches;
    }
}
