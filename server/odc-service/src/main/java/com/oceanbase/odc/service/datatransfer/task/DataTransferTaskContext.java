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

package com.oceanbase.odc.service.datatransfer.task;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferTaskResult;

public class DataTransferTaskContext implements Future<DataTransferTaskResult> {
    private final Future<DataTransferTaskResult> controlFuture;
    private final DataTransferTask task;

    public DataTransferTaskContext(Future<DataTransferTaskResult> controlFuture, DataTransferTask task) {
        this.controlFuture = controlFuture;
        this.task = task;
    }

    public DataTransferTaskResult getStatus() {
        if (task.getJob() == null) {
            return new DataTransferTaskResult();
        }
        return new DataTransferTaskResult(task.getJob().getDataObjectsStatus(),
                task.getJob().getSchemaObjectsStatus());
    }

    public double getProgress() {
        if (task.getJob() == null) {
            return 0.0;
        }
        return task.getJob().getProgress();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean canceled = true;
        if (task.getJob() != null) {
            canceled = task.getJob().cancel(mayInterruptIfRunning);
        }
        return canceled && controlFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        boolean isCanceled = true;
        if (task.getJob() != null) {
            isCanceled = task.getJob().isCanceled();
        }
        return isCanceled && controlFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return controlFuture.isDone();
    }

    @Override
    public DataTransferTaskResult get() throws ExecutionException, InterruptedException {
        return controlFuture.get();
    }

    @Override
    public DataTransferTaskResult get(long timeout, TimeUnit unit)
            throws ExecutionException, InterruptedException, TimeoutException {
        return controlFuture.get(timeout, unit);
    }

}
