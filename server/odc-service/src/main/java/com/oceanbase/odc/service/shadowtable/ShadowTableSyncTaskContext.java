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
package com.oceanbase.odc.service.shadowtable;

import java.util.concurrent.Future;

import com.oceanbase.odc.service.flow.task.model.ShadowTableSyncTaskResult;
import com.oceanbase.odc.service.flow.task.model.ShadowTableSyncTaskResult.TableSyncExecuting;
import com.oceanbase.odc.service.shadowtable.model.TableSyncExecuteStatus;

import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2022/9/23 下午2:17
 * @Description: []
 */
public class ShadowTableSyncTaskContext {
    private volatile ShadowTableSyncTask task;
    private Future<ShadowTableSyncTaskResult> handler;

    ShadowTableSyncTaskContext(@NonNull ShadowTableSyncTask task, @NonNull Future<ShadowTableSyncTaskResult> handler) {
        this.task = task;
        this.handler = handler;
    }

    public double getProgress() {
        return task.getProgress();
    }

    public boolean isSuccessful() {
        ShadowTableSyncTaskResult result = getResult();
        for (TableSyncExecuting executing : result.getTables()) {
            if (executing.getStatus() != TableSyncExecuteStatus.SUCCESS) {
                return false;
            }
        }
        return true;
    }

    public boolean isFailed() {
        ShadowTableSyncTaskResult result = getResult();
        for (TableSyncExecuting executing : result.getTables()) {
            if (executing.getStatus() == TableSyncExecuteStatus.FAILED) {
                return true;
            }
        }
        return false;
    }

    public ShadowTableSyncTaskResult getResult() {
        return task.getResult();
    }

    public boolean isDone() {
        return handler.isDone();
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return handler.cancel(mayInterruptIfRunning);
    }

    public boolean isCancelled() {
        return handler.isCancelled();
    }



}
