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

package com.oceanbase.odc.service.task.caller;

import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-11-22
 * @since 4.2.4
 */
@Slf4j
public class ProcessJobCaller extends BaseJobCaller {

    private final ProcessConfig processConfig;

    public ProcessJobCaller(ProcessConfig processConfig) {
        this.processConfig = processConfig;
    }

    @Override
    protected ExecutorIdentifier doStart(JobContext context) throws JobException {

        String executorName = JobUtils.generateExecutorName(context.getJobIdentity());
        ProcessBuilder pb = new ExecutorProcessBuilderFactory().getProcessBuilder(
                processConfig.getEnvironments(), JobUtils.generateExecutorSelectorOnProcess(executorName));
        Process process;
        try {
            process = pb.start();
        } catch (Exception ex) {
            throw new JobException("Start process failed.", ex);
        }

        long pid = SystemUtils.getProcessPid(process);
        if (pid == -1) {
            process.destroyForcibly();
            throw new JobException("Get pid failed, job id={0} ", context.getJobIdentity().getId());
        }

        boolean isProcessRunning =
                SystemUtils.isProcessRunning(pid, JobUtils.generateExecutorSelectorOnProcess(executorName));

        if (!isProcessRunning) {
            process.destroyForcibly();
            throw new JobException("Start process failed, not process found, pid={0},executorName={1}.",
                    pid, executorName);
        }

        ProcessExecutorIdentifier pei = new ProcessExecutorIdentifier();
        pei.setIpAddress(SystemUtils.getLocalIpAddress());
        pei.setPid(pid);
        pei.setExecutorName(executorName);

        return pei;
    }

    @Override
    protected void doStop(JobIdentity ji) throws JobException {}

    @Override
    protected void doDestroy(ExecutorIdentifier identifier) throws JobException {
        ProcessExecutorIdentifier pei = (ProcessExecutorIdentifier) identifier;
        long pid = pei.getPid();
        if (SystemUtils.isProcessRunning(pid,
                JobUtils.generateExecutorSelectorOnProcess(identifier.getExecutorName()))) {
            log.info("Found process, try kill it, pid={}.", pid);
            boolean result = SystemUtils.killProcessByPid(pid);
            if (result) {
                log.info("Destroy succeed by kill process, executorName={}, pid={}", pei.getExecutorName(), pid);
            } else {
                throw new JobException("Destroy executor failed by kill process, executorName={0}, pid={1}",
                        pei.getExecutorName(), pid);
            }
        }
    }
}
