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

package com.oceanbase.odc.service.task.executor;

import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.constants.JobEnvConstants;
import com.oceanbase.odc.service.task.enums.TaskRunModeEnum;
import com.oceanbase.odc.service.task.executor.context.JobContextProvider;
import com.oceanbase.odc.service.task.executor.context.JobContextProviderFactory;
import com.oceanbase.odc.service.task.executor.executor.TaskExecutor;
import com.oceanbase.odc.service.task.executor.executor.ThreadPoolTaskExecutor;
import com.oceanbase.odc.service.task.executor.logger.LogUtils;
import com.oceanbase.odc.service.task.executor.task.Task;
import com.oceanbase.odc.service.task.executor.task.TaskFactory;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/11/22 15:33
 */
@Slf4j
public class TaskApplication {

    private TaskExecutor taskExecutor;

    private JobContextProvider jobContextProvider;

    public void run(String[] args) {

        init(args);
        EmbedServer server = new EmbedServer();
        server.start(JobUtils.getPort());
        log.info("Starting embed server.");
        try {
            JobContext context = jobContextProvider.provide();
            Task task = TaskFactory.create(context.getJobClass());
            log.info("Task created, context: {}", context);
            taskExecutor.execute(task, context);
            ExitHelper.await();
        } catch (Exception e) {
            log.warn("Execute task error:", e);
        } finally {
            try {
                server.stop();
            } catch (Exception e) {
                log.warn("Stop embed server occur exception:", e);
            }
        }

    }

    private void init(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Task executor exits, systemInfo={}", SystemUtils.getSystemMemoryInfo());
        }));
        System.setProperty(JobEnvConstants.LOG_DIRECTORY, LogUtils.getBaseLogPath());
        log.info("Log directory is {}.", LogUtils.getBaseLogPath());

        String runMode = SystemUtils.getEnvOrProperty(JobEnvConstants.TASK_RUN_MODE);
        Verify.notBlank(runMode, JobEnvConstants.TASK_RUN_MODE);

        jobContextProvider = JobContextProviderFactory.create(TaskRunModeEnum.valueOf(runMode));
        log.info("JobContextProvider init success: {}", jobContextProvider.getClass().getSimpleName());
        taskExecutor = new ThreadPoolTaskExecutor(1);
        log.info("Task executor init success: {}", taskExecutor.getClass().getSimpleName());
        log.info("Task application ip is {}.", SystemUtils.getLocalIpAddress());
        log.info("Task application port is {}.", JobUtils.getPort());
    }

}
