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
package com.oceanbase.odc.service.flow.task;

import org.flowable.engine.delegate.DelegateExecution;

import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonTaskResult;
import com.oceanbase.odc.service.task.TaskService;

/**
 * @author jingtian
 * @date 2024/1/9
 * @since
 */
public class DBStructureComparisonFlowableTask extends BaseODCFlowTaskDelegate<DBStructureComparisonTaskResult> {
    @Override
    protected DBStructureComparisonTaskResult start(Long taskId, TaskService taskService, DelegateExecution execution)
            throws Exception {
        return null;
    }

    @Override
    protected boolean isSuccessful() {
        return false;
    }

    @Override
    protected boolean isFailure() {
        return false;
    }

    @Override
    protected void onTimeout(Long taskId, TaskService taskService) {

    }

    @Override
    protected void onProgressUpdate(Long taskId, TaskService taskService) {

    }

    @Override
    protected boolean cancel(boolean mayInterruptIfRunning, Long taskId, TaskService taskService) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }
}
