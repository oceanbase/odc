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
package com.oceanbase.odc.service.task.supervisor;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.ProcessConfig;
import com.oceanbase.odc.service.task.enums.JobCallerAction;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.listener.JobCallerEvent;
import com.oceanbase.odc.service.task.supervisor.endpoint.ExecutorEndpoint;
import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;
import com.oceanbase.odc.service.task.supervisor.proxy.LocalTaskSupervisorProxy;
import com.oceanbase.odc.service.task.supervisor.proxy.TaskSupervisorProxy;
import com.oceanbase.odc.service.task.util.TaskExecutorClient;

import lombok.extern.slf4j.Slf4j;

/**
 * job caller for task operation
 * 
 * @author longpeng.zlp
 * @date 2024/11/28 14:49
 */
@Slf4j
public class TaskSupervisorJobCaller {
    // event listener
    private final JobEventHandler jobEventHandler;
    // supervisor command proxy to send command to supervisor endpoint
    private final LocalTaskSupervisorProxy taskSupervisorProxy;
    // task executor client to send command to task directly
    private final TaskExecutorClient taskExecutorClient;

    public TaskSupervisorJobCaller(JobEventHandler jobEventHandler,
            LocalTaskSupervisorProxy taskSupervisorProxy, TaskExecutorClient taskExecutorClient) {
        this.jobEventHandler = jobEventHandler;
        this.taskSupervisorProxy = taskSupervisorProxy;
        this.taskExecutorClient = taskExecutorClient;
    }

    // only pull mode
    public ExecutorEndpoint startTask(SupervisorEndpoint supervisorEndpoint, JobContext jobContext,
            ProcessConfig processConfig) throws JobException {
        ExecutorEndpoint executorEndpoint = null;
        try {
            // do start process
            jobEventHandler.beforeStartJob(jobContext);
            executorEndpoint = taskSupervisorProxy.startTask(supervisorEndpoint, jobContext, processConfig);
            jobEventHandler.afterStartJob(executorEndpoint, jobContext);
            // send success event
            log.info("Start job succeed, jobId={}.", jobContext.getJobIdentity().getId());
            jobEventHandler.onNewEvent(new JobCallerEvent(jobContext.getJobIdentity(), JobCallerAction.START, true,
                    TaskSupervisor.getExecutorIdentifier(executorEndpoint), null));
            return executorEndpoint;
        } catch (Exception e) {
            // try roll back
            destroyTask(supervisorEndpoint, executorEndpoint, jobContext);
            // send failed event
            jobEventHandler
                    .onNewEvent(new JobCallerEvent(jobContext.getJobIdentity(), JobCallerAction.START, false, e));
            throw new JobException("Start job failed", e);
        }
    }

    /**
     * stop task through supervisor agent, this will force stop task
     */
    public TaskCallerResult destroyTask(SupervisorEndpoint supervisorEndpoint, ExecutorEndpoint executorEndpoint,
            JobContext jobContext) throws JobException {
        try {
            TaskCallerResult stopResult = null;
            if (taskSupervisorProxy.isSupervisorAlive(supervisorEndpoint)) {
                // supervisor is alive and stopped
                boolean stopFlag = taskSupervisorProxy.destroyTask(supervisorEndpoint, executorEndpoint, jobContext);
                stopResult = stopFlag ? TaskCallerResult.SUCCESS_RESULT
                        : TaskCallerResult
                                .failed(new JobException("destroy task failed for endpoint=" + executorEndpoint));
                log.info("destroy through agent with ret = {}, endpoint = {}", stopFlag, supervisorEndpoint);
            } else {
                // supervisor not alive can't determinate stop result
                log.info("supervisor not alive, endpoint = {}", supervisorEndpoint);
                stopResult = TaskCallerResult.failed(new JobException(
                        "supervisor agent not alive, can't determinate endpoint = " + executorEndpoint + " status"));
            }
            log.info("Stop job {}, jobId={}.", stopResult.getSucceed() ? "successfully" : "failed",
                    jobContext.getJobIdentity().getId());
            jobEventHandler
                    .onNewEvent(new JobCallerEvent(jobContext.getJobIdentity(), JobCallerAction.DESTROY,
                            stopResult.getSucceed(), stopResult.getE()));
            return stopResult;
        } catch (Exception e) {
            // handle stop exception
            jobEventHandler
                    .onNewEvent(new JobCallerEvent(jobContext.getJobIdentity(), JobCallerAction.DESTROY, false, e));
            return TaskCallerResult
                    .failed(new JobException("job be stop failed, jobId={0}.", e, jobContext.getJobIdentity().getId()));
        }
    }

    /**
     * stop task with http endpoint, this will not guarantee task has stopped
     */
    public TaskCallerResult stopTaskDirectly(ExecutorEndpoint executorEndpoint,
            JobContext jobContext) throws JobException {
        try {
            taskExecutorClient.stop(TaskSupervisorProxy.getExecutorEndpoint(executorEndpoint),
                    jobContext.getJobIdentity());
            jobEventHandler
                    .onNewEvent(new JobCallerEvent(jobContext.getJobIdentity(), JobCallerAction.STOP,
                            true, null));
            return TaskCallerResult.SUCCESS_RESULT;
        } catch (Exception e) {
            log.info("stop task failed cause {}", e.getMessage());
            jobEventHandler
                    .onNewEvent(new JobCallerEvent(jobContext.getJobIdentity(), JobCallerAction.STOP,
                            false, e));
            return TaskCallerResult.failed(e);
        }
    }

    /**
     * modify task use task executor client
     *
     * @param supervisorEndpoint
     * @param executorEndpoint
     * @param jobContext
     * @return
     * @throws JobException
     */
    public TaskCallerResult modifyTask(SupervisorEndpoint supervisorEndpoint, ExecutorEndpoint executorEndpoint,
            JobContext jobContext) {
        try {
            taskExecutorClient.modifyJobParameters(
                    TaskSupervisorProxy.getExecutorIdentifierByExecutorEndpoint(executorEndpoint),
                    jobContext.getJobIdentity(),
                    JsonUtils.toJson(jobContext.getJobParameters()));

            jobEventHandler
                    .onNewEvent(new JobCallerEvent(jobContext.getJobIdentity(), JobCallerAction.MODIFY,
                            true, null));
            return TaskCallerResult.SUCCESS_RESULT;
        } catch (Exception e) {
            jobEventHandler
                    .onNewEvent(new JobCallerEvent(jobContext.getJobIdentity(), JobCallerAction.MODIFY,
                            false, null));
            return TaskCallerResult.failed(e);
        }
    }

    /**
     * it's only db related operation TODO(lx):it will be removed out of this class
     * 
     * @param jobContext
     * @return
     */
    public TaskCallerResult finish(SupervisorEndpoint supervisorEndpoint, ExecutorEndpoint executorEndpoint,
            JobContext jobContext)
            throws JobException {
        TaskCallerResult taskCallerResult = TaskCallerResult.SUCCESS_RESULT;
        if (null == executorEndpoint) {
            log.info("job finished success, it's not created yet");
        } else {
            log.info("try finished job, executorEndpoint={}", executorEndpoint);
            taskCallerResult = destroyTask(supervisorEndpoint, executorEndpoint, jobContext);
            if (!taskCallerResult.getSucceed()) {
                jobEventHandler.finishFailed(executorEndpoint, jobContext);
            }
        }
        jobEventHandler.afterFinished(null, jobContext);
        return taskCallerResult;
    }

    /**
     * for supervisor agent, it will always be true, cause command will be routed to right agent
     * 
     * @param jobContext
     * @return
     */
    public TaskCallerResult canBeFinish(SupervisorEndpoint supervisorEndpoint, ExecutorEndpoint executorEndpoint,
            JobContext jobContext)
            throws JobException {
        return TaskCallerResult.SUCCESS_RESULT;
    }
}
