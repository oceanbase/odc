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

import com.oceanbase.odc.service.task.constants.JobEnvConstants;
import com.oceanbase.odc.service.task.enums.DeployModelEnum;
import com.oceanbase.odc.service.task.executor.util.SystemEnvUtil;

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
        Task task = TaskFactory.create(jobContextProvider.provide());
        log.info("Task created, taskDetails: {}", task);
        taskExecutor.execute(task);
        while (!task.isStopped() && !task.isFinished()) {
            try {
                Thread.sleep(1000);
                log.info("Waiting for task finished ...");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void init(String[] args) {
        DeployModelEnum mode = DeployModelEnum.valueOf(SystemEnvUtil.nullSafeGet(JobEnvConstants.DEPLOY_MODE));
        jobContextProvider = JobContextProviderFactory.create(mode);
        log.info("JobContextProvider init success: {}", jobContextProvider.getClass().getName());
        taskExecutor = new ThreadPoolTaskExecutor(1);
        log.info("Task executor init success: {}", taskExecutor.getClass().getName());
    }

}
