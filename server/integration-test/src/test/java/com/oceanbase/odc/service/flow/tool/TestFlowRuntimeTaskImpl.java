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
package com.oceanbase.odc.service.flow.tool;

import java.util.UUID;
import java.util.concurrent.Callable;

import org.flowable.engine.delegate.DelegateExecution;

import com.oceanbase.odc.service.flow.task.BaseRuntimeFlowableDelegate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestFlowRuntimeTaskImpl extends BaseRuntimeFlowableDelegate<String> {

    private boolean isInterrupted = false;

    @Override
    protected Callable<String> initCallable(DelegateExecution execution) {
        return () -> {
            log.info("Task starts, activityId={}", execution.getCurrentActivityId());
            long start = System.currentTimeMillis();
            while (!isInterrupted && System.currentTimeMillis() - start < 500);
            String returnVal = UUID.randomUUID().toString();
            log.info("Task ends, activityId={}, returnVal={}", execution.getCurrentActivityId(), returnVal);
            return returnVal;
        };
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        isInterrupted = true;
        return true;
    }

    @Override
    public boolean isCancelled() {
        return isInterrupted;
    }

}
