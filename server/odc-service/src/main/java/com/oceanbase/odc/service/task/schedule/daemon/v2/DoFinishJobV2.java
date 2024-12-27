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
import java.util.Map;
import java.util.Optional;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.data.domain.Page;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.alarm.AlarmEventNames;
import com.oceanbase.odc.core.alarm.AlarmUtils;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.metadb.task.ResourceAllocateInfoEntity;
import com.oceanbase.odc.service.resource.ResourceID;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifier;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifierParser;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.ResourceIDUtil;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.enums.TaskRunMode;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.exception.TaskRuntimeException;
import com.oceanbase.odc.service.task.resource.ResourceAllocateState;
import com.oceanbase.odc.service.task.schedule.DefaultJobContextBuilder;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.service.task.supervisor.TaskCallerResult;
import com.oceanbase.odc.service.task.supervisor.TaskSupervisor;
import com.oceanbase.odc.service.task.supervisor.endpoint.ExecutorEndpoint;
import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * try finish job and notify resource module that task has no reference to task Notice: to
 * compatible old ODC version, finish action may ignore some job cause they are created in old ODC
 * version and it will be handled by proper node as previous ODC logic done.
 * 
 * @author longpeng.zlp
 * @date 2024/12/17 08:44
 */
@Slf4j
@DisallowConcurrentExecution
public class DoFinishJobV2 implements Job {

    private JobConfiguration configuration;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        configuration = JobConfigurationHolder.getJobConfiguration();
        // scan terminate job
        TaskFrameworkService taskFrameworkService = configuration.getTaskFrameworkService();
        TaskFrameworkProperties taskFrameworkProperties = configuration.getTaskFrameworkProperties();
        Page<JobEntity> jobs = taskFrameworkService.findTerminalJob(0,
                taskFrameworkProperties.getSingleFetchDestroyExecutorJobRows());
        jobs.forEach(a -> {
            try {
                configuration.getTransactionManager()
                        .doInTransactionWithoutResult(() -> destroyExecutor(taskFrameworkService, a));
            } catch (Throwable e) {
                log.warn("Try to destroy failed, jobId={}.", a.getId(), e);
            }
        });
    }

    private void destroyExecutor(TaskFrameworkService taskFrameworkService, JobEntity jobEntity) {
        if (!jobEntity.getStatus().isTerminated()) {
            log.warn("job status expected terminated, current job id = {}, status = {}", jobEntity.getId(),
                    jobEntity.getStatus());
            return;
        }
        try {
            if (tryDestroyExecutor(jobEntity)) {
                // update destroyed status
                taskFrameworkService.updateExecutorToDestroyed(jobEntity.getId());
            }
        } catch (Throwable e) {
            log.warn("Destroy executor occur error, jobId={}: ", jobEntity.getId(), e);
            if (e.getMessage() != null &&
                    !e.getMessage().startsWith(JobConstants.ODC_EXECUTOR_CANNOT_BE_DESTROYED)) {
                Map<String, String> eventMessage = AlarmUtils.createAlarmMapBuilder()
                        .item(AlarmUtils.ORGANIZATION_NAME,
                                Optional.ofNullable(jobEntity.getOrganizationId()).map(
                                        Object::toString).orElse(StrUtil.EMPTY))
                        .item(AlarmUtils.TASK_JOB_ID_NAME, String.valueOf(jobEntity.getId()))
                        .item(AlarmUtils.MESSAGE_NAME,
                                MessageFormat.format("Job executor destroy failed, jobId={0}, message={1}",
                                        jobEntity.getId(), e.getMessage()))
                        .build();
                AlarmUtils.alarm(AlarmEventNames.TASK_EXECUTOR_DESTROY_FAILED, eventMessage);
            }
            throw new TaskRuntimeException(e);
        }
    }

    private boolean tryDestroyExecutor(JobEntity jobEntity)
            throws JobException {
        Optional<ResourceAllocateInfoEntity> resourceAllocateInfoEntity = configuration.getSupervisorAgentAllocator()
                .queryResourceAllocateIntoEntity(jobEntity.getId());
        // allocate by latest version, resource allocate info table exists record
        if (resourceAllocateInfoEntity.isPresent()) {
            destroyTaskByAgent(resourceAllocateInfoEntity.get(), jobEntity);
            log.info("deallocate agent resource for job id = {}", jobEntity.getId());
            configuration.getSupervisorAgentAllocator().deallocateSupervisorEndpoint(jobEntity.getId());
            return true;
        }
        // allocate by old odc version
        if (configuration.getTaskFrameworkProperties().getRunMode() == TaskRunMode.K8S) {
            // running in old k8s mode
            releaseK8sResource(jobEntity);
            return true;
        } else {
            // running in old process mode
            return tryDestroyProcess(jobEntity);
        }
    }

    private void destroyTaskByAgent(ResourceAllocateInfoEntity entity, JobEntity jobEntity) throws JobException {
        SupervisorEndpoint supervisorEndpoint = JsonUtils.fromJson(entity.getEndpoint(), SupervisorEndpoint.class);
        if (null == supervisorEndpoint) {
            if (ResourceAllocateState.FAILED.equal(entity.getResourceAllocateState())) {
                log.info("failed supervisor state for job id = {}, endpoint str = {}", jobEntity.getId(),
                        entity.getEndpoint());
            } else {
                log.info("invalid supervisor state for job id = {}, endpoint str = {}", jobEntity.getId(),
                        entity.getEndpoint());
            }
            return;
        }
        String executorIdentifierStr = jobEntity.getExecutorIdentifier();
        if (StringUtils.isBlank(executorIdentifierStr)) {
            // task not started
            configuration.getSupervisorAgentAllocator().deallocateSupervisorEndpoint(jobEntity.getId());
            log.info("executor is empty, task not started, deallocate usage resource, job id = {}", jobEntity.getId());
            return;
        }
        ExecutorIdentifier identifier = ExecutorIdentifierParser.parser(executorIdentifierStr);
        ExecutorEndpoint executorEndpoint =
                new ExecutorEndpoint("command", supervisorEndpoint.getHost(), supervisorEndpoint.getPort(),
                        identifier.getPort(), executorIdentifierStr);
        JobContext jobContext = new DefaultJobContextBuilder().build(jobEntity);
        TaskCallerResult taskCallerResult = configuration.getTaskSupervisorJobCaller().destroyTask(supervisorEndpoint,
                executorEndpoint, jobContext);
        if (null != taskCallerResult.getE()) {
            throw new JobException("destroy task failed for job id = " + jobEntity.getId(), taskCallerResult.getE());
        }
    }

    // release old version odc process, only release task on local machine
    private boolean tryDestroyProcess(JobEntity jobEntity) throws JobException {
        // running in process mode
        String executorIdentifier = jobEntity.getExecutorIdentifier();
        if (StringUtils.isEmpty(executorIdentifier)) {
            log.info("executor is empty， ignore destroy process");
            return true;
        }
        ExecutorIdentifier identifier = ExecutorIdentifierParser.parser(executorIdentifier);
        if (StringUtils.equals(StringUtils.trim(identifier.getHost()),
                StringUtils.trim(SystemUtils.getLocalIpAddress()))) {
            // in same machine, kill it
            TaskSupervisor taskSupervisor = new TaskSupervisor(null, null);
            taskSupervisor.destroyTask(identifier);
            return true;
        } else {
            log.info("{} not on {}, ignore it", executorIdentifier, SystemUtils.getLocalIpAddress());
            // not on this machine, ignore it
            return false;
        }
    }

    // release old version odc k8s resource, direct mark resource released
    private void releaseK8sResource(JobEntity jobEntity) {
        String executorIdentifier = jobEntity.getExecutorIdentifier();
        if (StringUtils.isEmpty(executorIdentifier)) {
            log.info("executor is empty， ignore release resource");
            return;
        }
        ExecutorIdentifier identifier = ExecutorIdentifierParser.parser(executorIdentifier);
        ResourceID resourceID = ResourceIDUtil.getResourceID(identifier, jobEntity);
        configuration.getResourceManager().release(resourceID);
    }
}
