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

import com.oceanbase.odc.service.task.base.TaskBase;
import com.oceanbase.odc.service.task.caller.JobContext;

import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2024/8/28 10:51
 */
@Slf4j
public class DummyTask extends TaskBase<String> {

    private AtomicBoolean stopped = new AtomicBoolean(false);
    private AtomicLong loopCount = new AtomicLong(0);
    private final long maxLoopCount = 1000000;

    public DummyTask() {}

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
    public boolean start() throws Exception {
        while (!stopped.get() && loopCount.get() < maxLoopCount) {
            Thread.sleep(1000);
            log.info("dummy task loop for to {}", loopCount.get());
            loopCount.incrementAndGet();
        }
        return true;
    }

    @Override
    public void stop() throws Exception {
        stopped.set(true);
    }

    @Override
    public void close() throws Exception {
        stopped.set(true);
    }
}
