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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.odc.service.task.supervisor.protocol.CommandType;
import com.oceanbase.odc.service.task.supervisor.protocol.GeneralTaskCommand;
import com.oceanbase.odc.service.task.supervisor.protocol.StartTaskCommand;
import com.oceanbase.odc.service.task.supervisor.protocol.TaskCommand;

/**
 * @author longpeng.zlp
 * @date 2024/10/29 15:59
 */
public class TaskCommandDeserializer {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public TaskCommand deserializeTaskCommand(String commandStr) throws JsonProcessingException {
        JsonNode objectNode = OBJECT_MAPPER.readTree(commandStr);
        JsonNode commandTypeNode = objectNode.get(TaskCommand.COMMAND_TYPE_NAME);
        if (null == commandTypeNode) {
            throw new IllegalStateException("invalid command, str=" + commandStr);
        }
        CommandType commandType = CommandType.valueOf(commandTypeNode.asText().toUpperCase());
        switch (commandType) {
            case START:
                return StartTaskCommand.fromJsonNode(objectNode);
            case DESTROY:
            case IS_TASK_ALIVE:
                return GeneralTaskCommand.fromJsonNode(GeneralTaskCommand::new, objectNode, commandType);
            default:
                throw new IllegalStateException("not support command type, str=" + commandType);
        }
    }
}
