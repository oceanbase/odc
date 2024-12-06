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

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.ProcessConfig;
import com.oceanbase.odc.service.task.supervisor.TaskSupervisorProxy;
import com.oceanbase.odc.service.task.supervisor.endpoint.ExecutorEndpoint;
import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;
import com.oceanbase.odc.service.task.supervisor.protocol.CommandType;
import com.oceanbase.odc.service.task.supervisor.protocol.GeneralTaskCommand;
import com.oceanbase.odc.service.task.supervisor.protocol.StartTaskCommand;
import com.oceanbase.odc.service.task.supervisor.protocol.TaskCommandSender;

/**
 * @author longpeng.zlp
 * @date 2024/10/29 14:42
 */
public class RemoteTaskSupervisorProxy implements TaskSupervisorProxy {
    private TaskCommandSender taskCommandSender;

    @Override
    public ExecutorEndpoint startTask(SupervisorEndpoint supervisorEndpoint, JobContext jobContext,
            ProcessConfig processConfig) throws IOException {
        return JsonUtils.fromJson(
                taskCommandSender.sendCommand(supervisorEndpoint, StartTaskCommand.create(jobContext, processConfig)),
                ExecutorEndpoint.class);
    }

    @Override
    public boolean stopTask(SupervisorEndpoint supervisorEndpoint, ExecutorEndpoint executorEndpoint,
            JobContext jobContext) throws IOException {
        return Boolean.valueOf(taskCommandSender.sendCommand(supervisorEndpoint,
                GeneralTaskCommand.create(jobContext, executorEndpoint, CommandType.STOP)));
    }

    @Override
    public boolean modifyTask(SupervisorEndpoint supervisorEndpoint, ExecutorEndpoint executorEndpoint,
            JobContext jobContext) throws IOException {
        return Boolean.valueOf(taskCommandSender.sendCommand(supervisorEndpoint,
                GeneralTaskCommand.create(jobContext, executorEndpoint, CommandType.MODIFY)));
    }

    @Override
    public boolean finishTask(SupervisorEndpoint supervisorEndpoint, ExecutorEndpoint executorEndpoint,
            JobContext jobContext) throws IOException {
        return Boolean.valueOf(taskCommandSender.sendCommand(supervisorEndpoint,
                GeneralTaskCommand.create(jobContext, executorEndpoint, CommandType.FINISH)));
    }

    @Override
    public boolean canBeFinished(SupervisorEndpoint supervisorEndpoint, ExecutorEndpoint executorEndpoint,
            JobContext jobContext) throws IOException {
        return Boolean.valueOf(taskCommandSender.sendCommand(supervisorEndpoint,
                GeneralTaskCommand.create(jobContext, executorEndpoint, CommandType.IS_ALIVE)));
    }


}
