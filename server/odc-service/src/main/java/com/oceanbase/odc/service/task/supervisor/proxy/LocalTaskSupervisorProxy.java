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
package com.oceanbase.odc.service.task.supervisor.proxy;

import java.io.IOException;

import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.ProcessConfig;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.supervisor.TaskSupervisor;
import com.oceanbase.odc.service.task.supervisor.endpoint.ExecutorEndpoint;
import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;
import com.oceanbase.odc.service.task.supervisor.protocol.TaskNetClient;
import com.oceanbase.odc.service.task.supervisor.runtime.EndpointInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * command executor to route task command local impl
 * 
 * @author longpeng.zlp
 * @date 2024/10/29 11:43
 */
@Slf4j
public class LocalTaskSupervisorProxy implements TaskSupervisorProxy {
    private final TaskSupervisor taskSupervisor;
    private final RemoteTaskSupervisorProxy remoteTaskSupervisorProxy;
    private final SupervisorEndpoint localEndPoint;

    public LocalTaskSupervisorProxy(SupervisorEndpoint supervisorEndpoint,
            String mainClassName) {
        this.localEndPoint = supervisorEndpoint;
        log.info("LocalTaskSupervisorProxy start with endpoint={}", supervisorEndpoint);
        remoteTaskSupervisorProxy = new RemoteTaskSupervisorProxy(new TaskNetClient());
        taskSupervisor = new TaskSupervisor(supervisorEndpoint, mainClassName);
    }

    @Override
    public ExecutorEndpoint startTask(SupervisorEndpoint supervisorEndpoint, JobContext jobContext,
            ProcessConfig processConfig) throws JobException, IOException {
        if (isLocalCommandCall(supervisorEndpoint)) {
            log.info("local call start task, supervisorEndpoint={}, jobContext={}, processConfig={}",
                    supervisorEndpoint, jobContext, processConfig);
            return taskSupervisor.startTask(jobContext, processConfig);
        } else {
            log.info("remote call start task, supervisorEndpoint={}, jobContext={}, processConfig={}",
                    supervisorEndpoint, jobContext, processConfig);
            return remoteTaskSupervisorProxy.startTask(supervisorEndpoint, jobContext, processConfig);
        }
    }

    @Override
    public boolean destroyTask(SupervisorEndpoint supervisorEndpoint, ExecutorEndpoint executorEndpoint,
            JobContext jobContext) throws JobException, IOException {
        if (null == executorEndpoint) {
            throw new JobException("empty executor endpoint to stop");
        }
        if (isLocalCommandCall(supervisorEndpoint)) {
            log.info("local call stop task, supervisorEndpoint={}, executorEndpoint={}, jobContext={}",
                    supervisorEndpoint, executorEndpoint, jobContext);
            return taskSupervisor.destroyTask(executorEndpoint, jobContext);
        } else {
            log.info("remote call stop task, supervisorEndpoint={}, executorEndpoint={}, jobContext={}",
                    supervisorEndpoint, executorEndpoint, jobContext);
            return remoteTaskSupervisorProxy.destroyTask(supervisorEndpoint, executorEndpoint, jobContext);
        }
    }


    @Override
    public boolean isTaskAlive(SupervisorEndpoint supervisorEndpoint, ExecutorEndpoint executorEndpoint,
            JobContext jobContext) throws JobException, IOException {
        if (isLocalCommandCall(supervisorEndpoint)) {
            log.info("local call canBeFinished task, supervisorEndpoint={}, executorEndpoint={}, jobContext={}",
                    supervisorEndpoint, executorEndpoint, jobContext);
            return taskSupervisor.isTaskAlive(TaskSupervisor.getExecutorIdentifier(executorEndpoint));
        } else {
            log.info("remote call canBeFinished task, supervisorEndpoint={}, executorEndpoint={}, jobContext={}",
                    supervisorEndpoint, executorEndpoint, jobContext);
            return remoteTaskSupervisorProxy.isTaskAlive(supervisorEndpoint, executorEndpoint, jobContext);
        }
    }

    @Override
    public boolean isSupervisorAlive(SupervisorEndpoint supervisorEndpoint) {
        if (isLocalCommandCall(supervisorEndpoint)) {
            log.info("local call isSupervisorAlive task, supervisorEndpoint={}", supervisorEndpoint);
            return true;
        } else {
            log.info("remote call isSupervisorAlive task, supervisorEndpoint={}", supervisorEndpoint);
            return remoteTaskSupervisorProxy.isSupervisorAlive(supervisorEndpoint);
        }
    }

    @Override
    public EndpointInfo supervisorResources(SupervisorEndpoint supervisorEndpoint) {
        return EndpointInfo.getEndpointInfo();
    }

    protected boolean isLocalCommandCall(SupervisorEndpoint supervisorEndpoint) {
        if (null == supervisorEndpoint) {
            throw new IllegalStateException("end point must be given for task supervisor proxy");
        }
        return supervisorEndpoint.equals(SupervisorEndpoint.SELF_ENDPOINT) || supervisorEndpoint.equals(localEndPoint);
    }
}
