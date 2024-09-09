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

import java.util.List;
import java.util.concurrent.ExecutorService;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/8/28 17:29
 * @Description: []
 */
@Getter
@Slf4j
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
    }
}
