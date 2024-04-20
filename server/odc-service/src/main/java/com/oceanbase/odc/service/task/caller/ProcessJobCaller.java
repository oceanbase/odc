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

import static com.oceanbase.odc.service.task.constants.JobConstants.ODC_EXECUTOR_CANNOT_BE_DESTROYED;

import java.text.MessageFormat;
import java.util.Optional;

import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.util.HttpUtil;
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
                processConfig, context.getJobIdentity().getId(), executorName);
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

        String portString = Optional.ofNullable(SystemUtils.getEnvOrProperty("server.port"))
                .orElse(DefaultExecutorIdentifier.DEFAULT_PORT + "");
        // set process id as namespace
        return DefaultExecutorIdentifier.builder().host(SystemUtils.getLocalIpAddress())
                .port(Integer.parseInt(portString))
                .namespace(pid + "")
                .executorName(executorName).build();
    }

    @Override
    protected void doStop(JobIdentity ji) throws JobException {}

    @Override
    protected void doDestroy(JobIdentity ji, ExecutorIdentifier ei) throws JobException {
        if (isExecutorExist(ei)) {
            long pid = Long.parseLong(ei.getNamespace());
            log.info("Found process, try kill it, pid={}.", pid);
            // first update destroy time, second destroy executor.
            // if executor failed update will be rollback, ensure distributed transaction atomicity.
            updateExecutorDestroyed(ji);
            destroyInternal(ei);
            return;
        }

        if (SystemUtils.getLocalIpAddress().equals(ei.getHost())) {
            updateExecutorDestroyed(ji);
            return;
        }
        JobConfiguration configuration = JobConfigurationHolder.getJobConfiguration();
        JobEntity jobEntity = configuration.getTaskFrameworkService().find(ji.getId());
        if (!HttpUtil.isOdcHealthy(ei.getHost(), ei.getPort())) {
            if (jobEntity.getStatus() == JobStatus.RUNNING) {
                // Cannot connect to target identifier,we cannot kill the process,
                // so we set job to FAILED and avoid two process running
                configuration.getTaskFrameworkService().updateStatusDescriptionByIdOldStatus(
                        ji.getId(), JobStatus.RUNNING, JobStatus.FAILED,
                        MessageFormat.format("Cannot connect to target odc server, jodId={0}, identifier={1}",
                                ji.getId(), ei));
            }
            updateExecutorDestroyed(ji);
            log.warn("Cannot connect to target odc server, set job to failed, jodId={}, identifier={}",
                    ji.getId(), ei);
            return;
        }
        throw new JobException(ODC_EXECUTOR_CANNOT_BE_DESTROYED +
                "Connect to target odc server succeed, but cannot destroy process,"
                + " may not on this machine, jodId={0}, identifier={1}", ji.getId(), ei);
    }

    @Override
    protected void doDestroyInternal(ExecutorIdentifier identifier) throws JobException {
        long pid = Long.parseLong(identifier.getNamespace());
        boolean result = SystemUtils.killProcessByPid(pid);
        if (result) {
            log.info("Destroy succeed by kill process, executorIdentifier={},  pid={}", identifier, pid);
        } else {
            throw new JobException(
                    "Destroy executor failed by kill process, identifier={0}, pid{1}=", identifier, pid);
        }
    }

    @Override
    protected boolean isExecutorExist(ExecutorIdentifier identifier) {
        long pid = Long.parseLong(identifier.getNamespace());
        boolean result = SystemUtils.isProcessRunning(pid,
                JobUtils.generateExecutorSelectorOnProcess(identifier.getExecutorName()));
        if (result) {
            log.info("Found executor by identifier, identifier={}", identifier);
        } else {
            log.warn("Not found executor by identifier, identifier={}", identifier);
        }
        return result;
    }
}
