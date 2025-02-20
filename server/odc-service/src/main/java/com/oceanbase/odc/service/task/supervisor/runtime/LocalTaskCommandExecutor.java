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
package com.oceanbase.odc.service.task.supervisor.runtime;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifierParser;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.supervisor.TaskSupervisor;
import com.oceanbase.odc.service.task.supervisor.endpoint.ExecutorEndpoint;
import com.oceanbase.odc.service.task.supervisor.protocol.GeneralTaskCommand;
import com.oceanbase.odc.service.task.supervisor.protocol.StartTaskCommand;
import com.oceanbase.odc.service.task.supervisor.protocol.TaskCommand;

/**
 * @author longpeng.zlp
 * @date 2024/10/29 17:45
 */
public class LocalTaskCommandExecutor implements TaskCommandExecutor {
    private final TaskSupervisor taskSupervisor;

    public LocalTaskCommandExecutor(TaskSupervisor taskSupervisor) {
        this.taskSupervisor = taskSupervisor;
    }

    public String onCommand(TaskCommand taskCommand) throws JobException {
        String ret = null;
        switch (taskCommand.commandType()) {
            case START:
                StartTaskCommand startTaskCommand = (StartTaskCommand) taskCommand;
                ExecutorEndpoint endpoint =
                        taskSupervisor.startTask(startTaskCommand.getJobContext(), startTaskCommand.getProcessConfig());
                ret = JsonUtils.toJson(endpoint);
                break;
            default:
                boolean succeed = callTaskSupervisorFunc((GeneralTaskCommand) taskCommand);
                ret = String.valueOf(succeed);
                break;
        }
        return String.valueOf(ret);
    }

    protected boolean callTaskSupervisorFunc(GeneralTaskCommand generalTaskCommand) throws JobException {
        switch (generalTaskCommand.commandType()) {
            case DESTROY:
                return taskSupervisor.destroyTask(generalTaskCommand.getExecutorEndpoint(),
                        generalTaskCommand.getJobContext());
            case IS_TASK_ALIVE:
                return taskSupervisor.isTaskAlive(
                        ExecutorIdentifierParser.parser(generalTaskCommand.getExecutorEndpoint().getIdentifier()));
            default:
                throw new IllegalStateException("not recognized command " + generalTaskCommand);
        }
    }
}
