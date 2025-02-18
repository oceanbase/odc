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
package com.oceanbase.odc.service.task.supervisor.protocol;

import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.supervisor.endpoint.ExecutorEndpoint;

import lombok.Getter;

/**
 * @author longpeng.zlp
 * @date 2024/10/29 15:47
 */
public class GeneralTaskCommand extends TaskCommand {
    protected static final String EXECUTOR_ENC_POINT_STR = "executorEndpoint";
    @Getter
    protected ExecutorEndpoint executorEndpoint;
    protected CommandType commandType;


    public void append(ObjectNode objectNode) {
        objectNode.put(EXECUTOR_ENC_POINT_STR, JsonUtils.toJson(executorEndpoint));
    }

    public CommandType commandType() {
        return commandType;
    }

    public static <T extends GeneralTaskCommand> T fromJsonNode(Supplier<T> commandSupplier, JsonNode jsonNode,
            CommandType commandType) {
        T command = commandSupplier.get();
        JsonNode executorEndpointNode = jsonNode.get(EXECUTOR_ENC_POINT_STR);
        ExecutorEndpoint endpoint = JsonUtils
                .fromJson(null == executorEndpointNode ? null : executorEndpointNode.asText(), ExecutorEndpoint.class);
        command.fillCommonFields(jsonNode);
        command.executorEndpoint = endpoint;
        command.commandType = commandType;
        return command;
    }

    public static GeneralTaskCommand create(JobContext jobContext, ExecutorEndpoint endpoint, CommandType commandType) {
        GeneralTaskCommand ret = new GeneralTaskCommand();
        ret.commandType = commandType;
        ret.version = COMMAND_VERSION;
        ret.jobContext = jobContext;
        ret.executorEndpoint = endpoint;
        return ret;
    }

}
