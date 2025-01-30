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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.common.response.OdcResult;
import com.oceanbase.odc.service.resource.ResourceID;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.supervisor.TaskSupervisor;
import com.oceanbase.odc.service.task.supervisor.endpoint.ExecutorEndpoint;
import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;
import com.oceanbase.odc.service.task.util.HttpClientUtils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-11-22
 * @since 4.2.4
 */
@Slf4j
public class ProcessJobCaller extends BaseJobCaller {
    @Getter
    private final ProcessConfig processConfig;

    private final TaskSupervisor taskSupervisor;

    public ProcessJobCaller(ProcessConfig processConfig, String mainClassName) {
        this.processConfig = processConfig;
        // only serve local task supervisor
        this.taskSupervisor = new TaskSupervisor(new SupervisorEndpoint(SystemUtils.getLocalIpAddress(),
                DefaultExecutorIdentifier.DEFAULT_PORT), mainClassName);
    }

    @Override
    public ExecutorIdentifier doStart(JobContext context) throws JobException {
        ExecutorEndpoint executorEndpoint = taskSupervisor.startTask(context, copyProcessConfig(processConfig));
        return ExecutorIdentifierParser.parser(executorEndpoint.getIdentifier());
    }

    @Override
    protected void doFinish(JobIdentity ji, ExecutorIdentifier ei, ResourceID resourceID)
            throws JobException {
        ExecutorEndpoint executorEndpoint = buildExecutorEndpoint(ei);
        JobContext jobContext = createJobContext(ji);
        if (isSameTaskSupervisor(executorEndpoint, taskSupervisor.getSupervisorEndpoint())) {
            taskSupervisor.destroyTask(executorEndpoint, jobContext);
            updateExecutorDestroyed(ji);
            return;
        }

        if (!isRemoteTaskSupervisorAlive(executorEndpoint)) {
            JobConfiguration configuration = JobConfigurationHolder.getJobConfiguration();
            JobEntity jobEntity = configuration.getTaskFrameworkService().find(ji.getId());
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

    /**
     * copy process config and remove listen port if it's in pull mode
     * 
     * @param origin
     * @return
     */
    protected ProcessConfig copyProcessConfig(ProcessConfig origin) {
        ProcessConfig ret = new ProcessConfig();
        ret.setJvmXmxMB(origin.getJvmXmxMB());
        ret.setJvmXmsMB(origin.getJvmXmsMB());
        Map<String, String> evn = new HashMap<>();
        if (null != origin.getEnvironments()) {
            evn.putAll(origin.getEnvironments());
        }
        if (StringUtils.equalsIgnoreCase(evn.get(JobEnvKeyConstants.REPORT_ENABLED), "false")) {
            evn.remove(JobEnvKeyConstants.ODC_EXECUTOR_PORT);
        }
        ret.setEnvironments(evn);
        return ret;
    }

    public boolean canBeFinish(JobIdentity ji, ExecutorIdentifier ei, ResourceID resourceID) {
        ExecutorEndpoint executorEndpoint = buildExecutorEndpoint(ei);
        // same machine can operate the task
        if (isSameTaskSupervisor(executorEndpoint, taskSupervisor.getSupervisorEndpoint())) {
            return true;
        }
        // remote is down
        if (!isRemoteTaskSupervisorAlive(executorEndpoint)) {
            log.info("Cannot connect to target odc server, executor can be destroyed,jobId={}, identifier={}",
                    ji.getId(), ei);
            return true;
        }
        return false;
    }


    @Override
    protected boolean isExecutorExist(ExecutorIdentifier identifier, ResourceID resourceID) {
        return taskSupervisor.isTaskAlive(identifier);
    }

    protected JobContext createJobContext(JobIdentity jobIdentity) {
        return new JobContext() {
            @Override
            public JobIdentity getJobIdentity() {
                return jobIdentity;
            }

            @Override
            public String getJobClass() {
                throw new IllegalStateException("not impl");
            }

            @Override
            public Map<String, String> getJobProperties() {
                throw new IllegalStateException("not impl");
            }

            @Override
            public Map<String, String> getJobParameters() {
                throw new IllegalStateException("not impl");
            }
        };
    }

    protected ExecutorEndpoint buildExecutorEndpoint(ExecutorIdentifier executorIdentifier) {
        return new ExecutorEndpoint(
                TaskSupervisor.COMMAND_PROTOCOL_NAME,
                executorIdentifier.getHost(),
                DefaultExecutorIdentifier.DEFAULT_PORT,
                executorIdentifier.getPort(),
                executorIdentifier.toString());
    }

    /**
     * this method will be moved out
     * 
     * @param executorEndpoint
     * @return
     */
    public boolean isRemoteTaskSupervisorAlive(ExecutorEndpoint executorEndpoint) {
        String url = String.format("http://%s:%s/api/v1/heartbeat/isHealthy", executorEndpoint.getHost(),
                executorEndpoint.getSupervisorPort());
        try {
            OdcResult<Boolean> result = HttpClientUtils.request("GET", url, new TypeReference<OdcResult<Boolean>>() {});
            return result.getData();
        } catch (IOException e) {
            log.warn("Check odc server health failed, url={}", url);
            return false;
        }
    }

    /**
     * if this is on same machine
     *
     * @param supervisorEndpoint
     * @return
     */
    protected boolean isSameTaskSupervisor(ExecutorEndpoint executorEndpoint, SupervisorEndpoint supervisorEndpoint) {
        return StringUtils.equalsIgnoreCase(executorEndpoint.getHost(), supervisorEndpoint.getHost())
                && Integer.compare(executorEndpoint.getSupervisorPort(), supervisorEndpoint.getPort()) == 0;
    }

}
