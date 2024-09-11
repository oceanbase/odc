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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/8/28 17:29
 * @Description: []
 */
@Getter
@Slf4j
public abstract class ExecutionGroup<Input, Result> {
    private final List<ExecutionSubGroupUnit<Input, Result>> executionUnits;
    private final List<ExecutionSubGroup<Input, Result>> subGroups;

    public ExecutionGroup(List<ExecutionSubGroupUnit<Input, Result>> executionUnits) {
        this.executionUnits = Collections.unmodifiableList(new ArrayList<>(executionUnits));
        this.subGroups = listSubGroups(executionUnits);
    }

    protected abstract List<ExecutionSubGroup<Input, Result>> listSubGroups(
            List<ExecutionSubGroupUnit<Input, Result>> executionUnits);

    public void execute(ExecutorService executorService, ExecutionGroupContext<Input, Result> context)
            throws InterruptedException {
        for (ExecutionSubGroup<Input, Result> subGroup : subGroups) {
            executorService.submit(() -> subGroup.execute(context));
        }
        waitForCompletion(context,
                this.getExecutionUnits().stream().map(ExecutionSubGroupUnit::getId).collect(Collectors.toSet()));
    }

    private void waitForCompletion(@NonNull ExecutionGroupContext<Input, Result> context,
            @NonNull Set<String> executionIds) throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            List<ExecutionResult<Result>> incompleteResults = executionIds.stream()
                    .map(context::getExecutionResult).filter(result -> !result.isCompleted())
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(incompleteResults)) {
                break;
            }
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
        }
        throw new InterruptedException();
    }
}
