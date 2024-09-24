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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.common.concurrent.ExecutorUtils;
import com.oceanbase.odc.core.shared.PreConditions;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/8/26 15:14
 * @Description: []
 */
@Slf4j
public final class GroupExecutionEngine<Input, Result> implements AutoCloseable {
    private final ExecutorServiceManager groupExecutorServiceManager;
    private ExecutorService daemonExecutorService;
    private ExecutionGroupContext<Input, Result> executionContext;

    public GroupExecutionEngine(int executorSize) {
        this.groupExecutorServiceManager = new ExecutorServiceManager(executorSize, "logical-database-change-executor");
        daemonExecutorService = Executors.newSingleThreadExecutor();
    }

    public ExecutionGroupContext<Input, Result> execute(List<ExecutionGroup<Input, Result>> groups) {
        PreConditions.notEmpty(groups, "groups");
        this.executionContext =
                new ExecutionGroupContext<>(groups, this.groupExecutorServiceManager.getExecutorService());
        daemonExecutorService.submit(() -> {
            try {
                Collection<ExecutionGroup<Input, Result>> executionGroups = executionContext.getExecutionGroups();
                if (CollectionUtils.isEmpty(executionGroups)) {
                    return;
                }
                executionGroups.forEach(
                        group -> group.getExecutionUnits().forEach(unit -> unit.beforeExecute(executionContext)));
                for (ExecutionGroup<Input, Result> group : executionGroups) {
                    if (Thread.currentThread().isInterrupted()) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedException();
                    }
                    try {
                        group.execute(this.groupExecutorServiceManager.getExecutorService(), executionContext);
                    } catch (InterruptedException ex) {
                        log.warn("ExecutionGroup is interrupted, ex=", ex);
                        Thread.currentThread().interrupt();
                        throw ex;
                    }
                    executionContext.increaseCompletedGroupCount();
                }
            } catch (InterruptedException e) {
                executionContext.addThrowable(e);
                Thread.currentThread().interrupt();
            }
        });
        return executionContext;
    }

    public void terminate(String executionUnitId) {
        try {
            log.info("Terminate executionUnit, executionId={}", executionUnitId);
            ExecutionSubGroupUnit<Input, Result> executionUnit = executionContext.getExecutionUnit(executionUnitId);
            executionUnit.terminate(executionContext);
        } catch (Exception ex) {
            log.warn("ExecutionUnit terminate failed, executionId={}", executionUnitId, ex);
        }
    }

    public void terminateAll() {
        for (ExecutionSubGroupUnit<Input, Result> unit : executionContext.listAllExecutionUnits()) {
            try {
                unit.terminate(executionContext);
            } catch (Exception ex) {
                log.warn("ExecutionUnit terminate failed, executionId={}", unit.getId(), ex);
            }
        }
    }

    public void skip(String executionUnitId) {
        ExecutionSubGroupUnit<Input, Result> executionUnit = executionContext.getExecutionUnit(executionUnitId);
        executionUnit.skip(executionContext);
    }

    @Override
    public void close() throws Exception {
        this.groupExecutorServiceManager.close();
        if (this.daemonExecutorService != null) {
            ExecutorUtils.gracefulShutdown(this.daemonExecutorService, "group-executor-daemon", 5);
        }
    }
}
