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

import java.io.IOException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;
import com.oceanbase.odc.service.task.util.HttpClientUtils;

/**
 * @author longpeng.zlp
 * @date 2024/10/29 15:01
 */
public class TaskNetClient {
    /**
     * send command to supervisor end point and return reponse
     * 
     * @param supervisorEndpoint
     * @param taskCommand
     * @return
     */
    public String sendCommand(SupervisorEndpoint supervisorEndpoint, TaskCommand taskCommand) throws IOException {
        String url = buildUrl(supervisorEndpoint, taskCommand);
        String requestBody = taskCommand.serialize();
        return HttpClientUtils.request("POST", url, requestBody, new TypeReference<String>() {});
    }

    /**
     * send heartbeat command
     */
    public String heartbeat(SupervisorEndpoint supervisorEndpoint) throws IOException {
        StringBuilder sb = new StringBuilder(64);
        appendHttpURlBase(supervisorEndpoint, sb);
        sb.append("/heartbeat");
        return HttpClientUtils.request("GET", sb.toString(), new TypeReference<String>() {});
    }

    public String memInfo(SupervisorEndpoint supervisorEndpoint) throws IOException {
        StringBuilder sb = new StringBuilder(64);
        appendHttpURlBase(supervisorEndpoint, sb);
        sb.append("/memInfo");
        return HttpClientUtils.request("GET", sb.toString(), new TypeReference<String>() {});
    }

    protected String buildUrl(SupervisorEndpoint supervisorEndpoint, TaskCommand taskCommand) {
        StringBuilder sb = new StringBuilder(64);
        appendHttpURlBase(supervisorEndpoint, sb);
        // create command url
        sb.append("/task/command/").append(taskCommand.commandType().name().toLowerCase());
        return sb.toString();
    }

    protected void appendHttpURlBase(SupervisorEndpoint supervisorEndpoint, StringBuilder sb) {
        // create base
        sb.append("http://")
                .append(supervisorEndpoint.getHost()).append(":")
                .append(supervisorEndpoint.getPort());
    }
}
