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
package com.oceanbase.odc.service.structurecompare;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.validation.constraints.NotNull;

import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonTaskResult;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2024/1/18
 * @since ODC_release_4.2.4
 */
public class StructureComparisonContext implements Future<DBStructureComparisonTaskResult> {
    private StructureComparisonTask task;
    private Future<DBStructureComparisonTaskResult> controlFuture;

    public StructureComparisonContext(@NonNull StructureComparisonTask task,
            @NonNull Future<DBStructureComparisonTaskResult> controlFuture) {
        this.task = task;
        this.controlFuture = controlFuture;
    }

    public double getProgress() {
        return task.getProgress();
    }

    public DBStructureComparisonTaskResult getStatus() {
        return task.getTaskResult();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return controlFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return controlFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return controlFuture.isDone();
    }

    @Override
    public DBStructureComparisonTaskResult get() throws InterruptedException, ExecutionException {
        return controlFuture.get();
    }

    @Override
    public DBStructureComparisonTaskResult get(long timeout, @NotNull TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return controlFuture.get(timeout, unit);
    }

}
