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

package com.oceanbase.odc.plugin.task.mysql.datatransfer.job;

import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;
import com.oceanbase.tools.loaddump.common.model.ObjectStatus.Status;

import lombok.Getter;

public abstract class AbstractJob {

    @Getter
    protected final ObjectResult object;

    @Getter
    protected long bytes = 0;

    @Getter
    protected long records = 0;

    private volatile boolean canceled = false;

    public AbstractJob(ObjectResult object) {
        this.object = object;
    }

    public abstract void run() throws Exception;

    public void cancel() {
        this.canceled = true;
    }

    public boolean isCanceled() {
        return canceled || Thread.currentThread().isInterrupted();
    }

    public boolean isDone() {
        return object.getStatus() == Status.FAILURE || object.getStatus() == Status.SUCCESS;
    }

    @Override
    public String toString() {
        return object.getSummary();
    }

    public void setStatus(Status status) {
        object.setStatus(status);
    }

    protected void increaseCount(long delta) {
        object.getCount().getAndAdd(delta);
    }

    protected void increaseTotal(long delta) {
        object.getTotal().getAndAdd(delta);
    }

}
