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

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

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

        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        ProcessBuilder pb = new ProcessBuilder();
        List<String> commands = new ArrayList<>();
        commands.add("java");
        commands.addAll(runtimeMxBean.getInputArguments().stream()
                .filter(c -> !c.startsWith("-agentlib") && !c.startsWith("-javaagent"))
                .collect(Collectors.toList()));
        commands.add("-classpath");
        commands.add(runtimeMxBean.getClassPath());
        commands.add(JobConstants.ODC_SERVER_CLASS_NAME);
        pb.directory(new File("."));

        pb.command(commands);
        pb.environment().putAll(processConfig.getEnvironments());
        Process process;
        try {
            process = pb.start();
        } catch (Exception ex) {
            throw new JobException("Start process failed.", ex);
        }
        long pid = SystemUtils.getProcessPid(process);
        if (pid == -1) {
            process.destroyForcibly();
            throw new JobException("Get pid failed,  job id " + context.getJobIdentity().getId());
        }

        return DefaultExecutorIdentifier.builder().host(SystemUtils.getLocalIpAddress())
                .executorName(pid + "").build();
    }

    @Override
    protected void doStop(JobIdentity ji) throws JobException {}

    @Override
    protected void doDestroy(ExecutorIdentifier identifier) throws JobException {
        if (SystemUtils.getLocalIpAddress().equals(identifier.getHost())) {
            if (SystemUtils.isProcessRunning(Long.parseLong(identifier.getExecutorName()))) {
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
