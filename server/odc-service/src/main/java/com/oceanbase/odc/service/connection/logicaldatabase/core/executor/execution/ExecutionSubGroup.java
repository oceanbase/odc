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

import java.util.List;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * @Author: Lebie
 * @Date: 2024/9/2 18:32
 * @Description: []
 */

@Data
@RequiredArgsConstructor
public class ExecutionSubGroup<Input, Result> {
    private final List<ExecutionSubGroupUnit<Input, Result>> executionUnits;

    public void execute(ExecutionGroupContext<Input, Result> context) {
        for (ExecutionSubGroupUnit<Input, Result> executionUnit : executionUnits) {
            executionUnit.execute(context);
        }
    }
}
