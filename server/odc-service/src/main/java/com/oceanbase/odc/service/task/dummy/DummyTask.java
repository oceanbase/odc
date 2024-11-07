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
package com.oceanbase.odc.service.task.dummy;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.oceanbase.odc.service.task.TaskContext;
import com.oceanbase.odc.service.task.base.BaseTask;
import com.oceanbase.odc.service.task.caller.JobContext;

import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2024/8/28 10:51
 */
@Slf4j
public class DummyTask extends BaseTask<String> {

    private AtomicBoolean stopped = new AtomicBoolean(false);
    private AtomicLong loopCount = new AtomicLong(0);
    private final long maxLoopCount = 1000000;

    @Override
    public double getProgress() {
        return (loopCount.get() / maxLoopCount) * 100;
    }

    @Override
    public String getTaskResult() {
        return "has loop count" + loopCount.get();
    }

    @Override
    protected void doInit(JobContext context) throws Exception {}

    @Override
    protected boolean doStart(JobContext context, TaskContext taskContext) throws Exception {
        while (!stopped.get() && loopCount.get() < maxLoopCount) {
            Thread.sleep(1000);
            log.info("dummy task loop for to {}", loopCount.get());
        }
        return !stopped.get();
    }

    @Override
    protected void doStop() throws Exception {
        stopped.set(true);
    }

    @Override
    protected void doClose() throws Exception {
        stopped.set(true);
    }
}
