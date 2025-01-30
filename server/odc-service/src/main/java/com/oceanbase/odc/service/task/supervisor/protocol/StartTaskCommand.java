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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.ProcessConfig;

import lombok.Getter;

/**
 * @author longpeng.zlp
 * @date 2024/10/29 15:25
 */
public class StartTaskCommand extends TaskCommand {
    protected static final String PROCESS_CONFIG_STR = "processConfig";
    @Getter
    private ProcessConfig processConfig;

    @Override
    public CommandType commandType() {
        return CommandType.START;
    }

    @Override
    public void append(ObjectNode objectNode) {
        objectNode.put(PROCESS_CONFIG_STR, JsonUtils.toJson(processConfig));
    }

    public static StartTaskCommand fromJsonNode(JsonNode jsonNode) {
        StartTaskCommand startTaskCommand = new StartTaskCommand();
        JsonNode processConfigNode = jsonNode.get(PROCESS_CONFIG_STR);
        startTaskCommand.processConfig =
                JsonUtils.fromJson(null == processConfigNode ? null : processConfigNode.asText(), ProcessConfig.class);
        startTaskCommand.fillCommonFields(jsonNode);
        return startTaskCommand;
    }

    public static StartTaskCommand create(JobContext jobContext, ProcessConfig processConfig) {
        StartTaskCommand ret = new StartTaskCommand();
        ret.processConfig = processConfig;
        ret.jobContext = jobContext;
        ret.version = COMMAND_VERSION;
        return ret;
    }
}
