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

package com.oceanbase.odc.service.datatransfer.task.common;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.datatransfer.model.DataTransferScope;
import com.oceanbase.odc.service.datatransfer.task.TransferTask;
import com.oceanbase.odc.service.flow.task.model.DataTransferTaskResult;

public class DataTransferTracer {

    private final DataTransferTaskRunner job;
    private final Future<Void> future;
    private volatile TaskStatus status = TaskStatus.RUNNING;

    public DataTransferTracer(DataTransferTaskRunner job, Future<Void> future) {
        this.job = job;
        this.future = future;
    }

    public DataTransferTaskResult get() {
        DataTransferTaskResult result = new DataTransferTaskResult();

        job.getTaskQueue().forEach(task -> {
            if (task.scope() == DataTransferScope.DATA) {
                result.getDataObjectsInfo().addAll(task.status());
            } else {
                result.getSchemaObjectsInfo().addAll(task.status());
            }
        });
        return result;
    }

    public TaskStatus status() {
        if (status.isTerminated()) {
            return status;
        }
        try {
            future.get(1, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ignore) {
            // eat exception
        } catch (Exception e) {
            status = TaskStatus.FAILED;
        }
        DataTransferTaskResult result = get();
        status = result.isAllTasksSucceeded() ? TaskStatus.DONE : TaskStatus.FAILED;
        return status;
    }

    public void cancel() {
        this.status = TaskStatus.CANCELED;
        future.cancel(true);
    }

    public double progress() {
        double progress = 0.0;

        TransferTask current = job.getCurrent();
        if (current == null) {
            return progress;
        }
        List<TransferTask> total = job.getTaskQueue();
        int stage = total.indexOf(current);
        progress = stage * 100D / total.size();
        return progress + current.progress();
    }
}
