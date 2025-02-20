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

import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.supervisor.protocol.TaskCommand;

/**
 * task command executor for execute
 * 
 * @author longpeng.zlp
 * @date 2024/11/22 14:50
 */
public interface TaskCommandExecutor {
    /**
     * execute command and write response if needed
     * 
     * @param taskCommand
     * @return result in string
     * @throws JobException
     */
    String onCommand(TaskCommand taskCommand) throws JobException;
}
