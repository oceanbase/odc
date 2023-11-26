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
package com.oceanbase.odc.service.resultset;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.oceanbase.odc.service.flow.task.model.ResultSetExportResult;

import lombok.Getter;

@Getter
public class ResultSetExportTaskContext {

    private final Future<ResultSetExportResult> future;
    private final ResultSetExportTask task;

    public ResultSetExportTaskContext(Future<ResultSetExportResult> future, ResultSetExportTask task) {
        this.future = future;
        this.task = task;
    }

    public double progress() {
        return task.getJob() == null ? 0 : task.getJob().getProgress();
    }

    public boolean isDone() {
        return future.isDone();
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean canceled = true;
        if (task.getJob() != null) {
            canceled = task.getJob().cancel(mayInterruptIfRunning);
        }
        return canceled && future.cancel(mayInterruptIfRunning);
    }

    public boolean isCanceled() {
        boolean isCanceled = true;
        if (task.getJob() != null) {
            isCanceled = task.getJob().isCanceled();
        }
        return isCanceled && future.isCancelled();
    }

    public ResultSetExportResult get() throws ExecutionException, InterruptedException, TimeoutException {
        return future.get(1, TimeUnit.MILLISECONDS);
    }

}
