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
import com.oceanbase.odc.service.task.supervisor.endpoint.ExecutorEndpoint;
import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;

/**
 * execute remote/local call for given supervisor endpoint
 * 
 * @author longpeng.zlp
 * @date 2024/10/29 11:48
 */
public interface TaskSupervisorProxy {
    /**
     * execute start task command to supervisorEndpoint
     * 
     * @param supervisorEndpoint
     * @param jobContext
     * @param processConfig
     * @return
     */
    ExecutorEndpoint startTask(SupervisorEndpoint supervisorEndpoint, JobContext jobContext,
            ProcessConfig processConfig)
            throws JobException, IOException;

    /**
     * execute destroy task command to supervisorEndpoint. this will take most effort to make sure task
     * process quit
     * 
     * @param supervisorEndpoint
     * @param jobContext
     * @return
     */
    boolean destroyTask(SupervisorEndpoint supervisorEndpoint, ExecutorEndpoint executorEndpoint, JobContext jobContext)
            throws IOException, JobException;

    /**
     * detect can be finish command to supervisorEndpoint
     * 
     * @param supervisorEndpoint
     * @param jobContext
     * @return
     */
    boolean isTaskAlive(SupervisorEndpoint supervisorEndpoint, ExecutorEndpoint executorEndpoint,
            JobContext jobContext)
            throws JobException, IOException;

    /**
     * if supervisor is alvie
     */
    boolean isSupervisorAlive(SupervisorEndpoint supervisorEndpoint);

    static String getExecutorIdentifierByExecutorEndpoint(ExecutorEndpoint executorEndpoint) {
        return executorEndpoint.getIdentifier();
    }

    static String getExecutorEndpoint(ExecutorEndpoint executorEndpoint) {
        return "http://" + executorEndpoint.getHost() + ":" + executorEndpoint.getExecutorPort();
    }
}
