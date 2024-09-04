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
import java.util.List;

/**
 * @Author: Lebie
 * @Date: 2024/8/28 18:31
 * @Description: []
 */
public class ExecutionUtils {

    public static <T> List<List<T>> createBatches(List<T> tasks, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i += batchSize) {
            batches.add(tasks.subList(i, Math.min(i + batchSize, tasks.size())));
        }
        return batches;
    }
}
