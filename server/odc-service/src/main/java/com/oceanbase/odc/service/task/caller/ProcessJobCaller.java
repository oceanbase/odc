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

import java.util.Date;

import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.util.JobDateUtils;

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

        ProcessBuilder pb = new JobProcessBuilder().build(processConfig.getEnvironments());
        Process process;
        try {
            process = pb.start();
        } catch (Exception ex) {
            throw new JobException("Start process failed.", ex);
        }

        Date time = JobDateUtils.getCurrentDate();
        long pid = SystemUtils.getProcessPid(process);
        if (pid == -1) {
            process.destroyForcibly();
            throw new JobException("Get pid failed,  job id " + context.getJobIdentity().getId());
        }

        return DefaultExecutorIdentifier.builder().host(SystemUtils.getLocalIpAddress()).namespace(time.getTime() + "")
                .executorName(pid + "").build();
    }

    @Override
    protected void doStop(JobIdentity ji) throws JobException {}

    @Override
    protected void doDestroy(ExecutorIdentifier identifier) throws JobException {
        if (SystemUtils.getLocalIpAddress().equals(identifier.getHost())) {
            if (SystemUtils.isProcessRunning(Long.parseLong(identifier.getExecutorName()),
                    Long.parseLong(identifier.getNamespace()))) {
                log.info("Found process {}, kill it.", identifier.getExecutorName());
                String executorName = identifier.getExecutorName();
                boolean result = SystemUtils.killProcessByPid(Long.parseLong(executorName));
                if (result) {
                    log.info("Destroy identifier {} by kill pid {} succeed.", identifier,
                            identifier.getExecutorName());
                } else {
                    throw new JobException("Destroy identifier" + identifier + " by kill pid " +
                            identifier.getExecutorName() + "  failed.");
                }
            }
        }
    }

}
