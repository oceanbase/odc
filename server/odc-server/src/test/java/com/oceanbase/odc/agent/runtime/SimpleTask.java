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
package com.oceanbase.odc.agent.runtime;

import com.oceanbase.odc.service.task.base.TaskBase;
import com.oceanbase.odc.service.task.caller.JobContext;

/**
 * @author longpeng.zlp
 * @date 2024/11/12 15:07
 */
public final class SimpleTask extends TaskBase<String> {
    private final boolean shouldThrowException;

    public SimpleTask() {
        this.shouldThrowException = false;
    }

    public SimpleTask(boolean shouldThrowException) {
        this.shouldThrowException = shouldThrowException;
    }

    @Override
    protected void doInit(JobContext context) throws Exception {}

    @Override
    public boolean start() throws Exception {
        if (shouldThrowException) {
            throw new IllegalStateException("exception should be thrown");
        }
        return true;
    }

    @Override
    public void stop() throws Exception {}

    @Override
    public void close() throws Exception {}

    @Override
    public double getProgress() {
        return 100;
    }

    @Override
    public String getTaskResult() {
        return "res";
    }
}
