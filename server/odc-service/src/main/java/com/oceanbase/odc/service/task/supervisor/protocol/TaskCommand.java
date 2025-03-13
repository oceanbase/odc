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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.task.caller.DefaultJobContext;
import com.oceanbase.odc.service.task.caller.JobContext;

import lombok.Getter;

/**
 * @author longpeng.zlp
 * @date 2024/10/29 15:13
 */
public abstract class TaskCommand {
    public static final ObjectMapper objectMapper = new ObjectMapper();
    public static final int COMMAND_VERSION = 0;
    public static final String VERSION_NAME = "version";
    public static final String JOB_CONTEXT_NAME = "jobContext";
    public static final String COMMAND_TYPE_NAME = "command";
    @Getter
    protected JobContext jobContext;
    @Getter
    protected int version;

    public abstract CommandType commandType();

    public String serialize() {
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put(VERSION_NAME, version);
        jsonNode.put(JOB_CONTEXT_NAME, JsonUtils.toJson(jobContext));
        jsonNode.put(COMMAND_TYPE_NAME, commandType().toString().toLowerCase());
        append(jsonNode);
        return jsonNode.toPrettyString();
    }

    protected void fillCommonFields(JsonNode jsonNode) {
        JsonNode versionNode = jsonNode.get(VERSION_NAME);
        version = null == versionNode ? 0 : versionNode.asInt();
        JsonNode jobContextNode = jsonNode.get(JOB_CONTEXT_NAME);
        jobContext =
                jobContextNode == null ? null : JsonUtils.fromJson(jobContextNode.asText(), DefaultJobContext.class);
    }

    public abstract void append(ObjectNode objectNode);
}
