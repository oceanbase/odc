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
public abstract class ExecutionGroup<Input, Result> {
    private final List<ExecutionSubGroupUnit<Input, Result>> executionUnits;
    private final List<ExecutionSubGroup<Input, Result>> subGroups;

    public ExecutionGroup(List<ExecutionSubGroupUnit<Input, Result>> executionUnits) {
        this.executionUnits = executionUnits;
        this.subGroups = listSubGroups(executionUnits);
    }

    protected abstract List<ExecutionSubGroup<Input, Result>> listSubGroups(
            List<ExecutionSubGroupUnit<Input, Result>> executionUnits);

    public void execute(ExecutorService executorService, ExecutionGroupContext<Input, Result> context) {
        for (ExecutionSubGroup<Input, Result> subGroup : subGroups) {
            executorService.submit(() -> subGroup.execute(context));
        }
    }
}
