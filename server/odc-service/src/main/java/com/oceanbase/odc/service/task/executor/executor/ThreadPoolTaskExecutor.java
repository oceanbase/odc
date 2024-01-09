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

package com.oceanbase.odc.service.task.executor.executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.executor.task.Task;

/**
 * A thread pool task executor.
 * 
 * @author gaoda.xy
 * @date 2023/11/24 11:22
 */
public class ThreadPoolTaskExecutor implements TaskExecutor {

    private final ExecutorService executor;

    public ThreadPoolTaskExecutor(int nThreads) {
        this.executor = Executors.newFixedThreadPool(nThreads);
    }

    @Override
    public void execute(Task task, JobContext jc) {
        executor.submit(() -> task.start(jc));
    }

}
