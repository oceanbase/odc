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
package com.oceanbase.odc.service.task.schedule.daemon.v2;

import java.text.MessageFormat;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.data.domain.Page;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.alarm.AlarmEventNames;
import com.oceanbase.odc.core.alarm.AlarmUtils;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.exception.TaskRuntimeException;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.service.task.supervisor.endpoint.ExecutorEndpoint;
import com.oceanbase.odc.service.task.util.JobUtils;
import com.oceanbase.odc.service.task.util.TaskSupervisorUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * send stop command to task for Canceling and Time out job
 * 
 * @author longpeng.zlp
 * @date 2024-12-13
 * @since 4.3.3
 */
@Slf4j
@DisallowConcurrentExecution
public class DoStopJobV2 implements Job {

    private JobConfiguration configuration;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        configuration = JobConfigurationHolder.getJobConfiguration();
        // scan preparing job
        TaskFrameworkService taskFrameworkService = configuration.getTaskFrameworkService();
        TaskFrameworkProperties taskFrameworkProperties = configuration.getTaskFrameworkProperties();
        Page<JobEntity> jobs = taskFrameworkService.findNeedStoppedJobs(0,
                taskFrameworkProperties.getSingleFetchCancelingJobRows());
        jobs.forEach(a -> {
            try {
                sendStopToTask(taskFrameworkService, a);
            } catch (Throwable e) {
                log.warn("Try to start job {} failed: ", a.getId(), e);
            }
        });
    }

    private void sendStopToTask(TaskFrameworkService taskFrameworkService, JobEntity jobEntity) {
        // task has started, but receive cancel command or task run timeout
        // we send command to it
        if (!StringUtils.isBlank(jobEntity.getExecutorEndpoint())) {
            log.info("Prepare send stop to task, jobId={}. current task status = {}", jobEntity.getId(),
                    jobEntity.getStatus());
            try {
                getConfiguration().getTaskSupervisorJobCaller().stopTaskDirectly(
                        buildExecutorEndpointFromIdentifier(jobEntity.getExecutorEndpoint()),
                        TaskSupervisorUtil.buildJobContextFromJobEntity(jobEntity));
            } catch (JobException e) {
                log.warn("Stop job occur error: ", e);
                AlarmUtils.alarm(AlarmEventNames.TASK_CANCELED_FAILED,
                        MessageFormat.format("Cancel job failed, jobId={0}", jobEntity.getId()));
                throw new TaskRuntimeException(e);
            }
        } else {
            log.info("Task with no executor endpoint, jobId={}. current task status = {}", jobEntity.getId(),
                    jobEntity.getStatus());
        }
        switch (jobEntity.getStatus()) {
            case CANCELING:
                // receive cancel command, transfer to DO_CANCELING
                JobUtils.updateStatusAndCheck(jobEntity.getId(), JobStatus.CANCELING, JobStatus.DO_CANCELING,
                        taskFrameworkService);
                break;
            case TIMEOUT:
                // prepare timeout or heartbeat expired
                String message = StringUtils.isBlank(jobEntity.getExecutorEndpoint()) ? "job prepare timeout"
                        : "job heartbeat timeout";
                taskFrameworkService.updateStatusDescriptionByIdOldStatus(jobEntity.getId(), JobStatus.TIMEOUT,
                        JobStatus.FAILED, message);
                break;
            default:
                throw new TaskRuntimeException("do stop job can't process status for " + jobEntity.getId()
                        + " with status " + jobEntity.getStatus());
        }
    }

    private JobConfiguration getConfiguration() {
        return configuration;
    }

    public static ExecutorEndpoint buildExecutorEndpointFromIdentifier(String executorEndpoint) {
        if (StringUtils.isBlank(executorEndpoint)) {
            return null;
        }
        if (executorEndpoint.startsWith("http://")) {
            executorEndpoint = executorEndpoint.substring(7);
        }
        String[] hostAndPort = executorEndpoint.split(":");
        ExecutorEndpoint endpoint = new ExecutorEndpoint();
        endpoint.setHost(hostAndPort[0]);
        endpoint.setExecutorPort(Integer.valueOf(hostAndPort[1]));
        endpoint.setSupervisorPort(9989);
        return endpoint;
    }
}
