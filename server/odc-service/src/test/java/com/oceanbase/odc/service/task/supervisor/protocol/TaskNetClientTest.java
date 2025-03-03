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

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;

/**
 * @author longpeng.zlp
 * @date 2024/11/25 16:44
 */
public class TaskNetClientTest {
    @Test
    public void testCommandSenderBuildStartUrl() {
        TaskNetClient taskNetClient = new TaskNetClient();
        SupervisorEndpoint supervisorEndpoint = new SupervisorEndpoint("127.0.0.1", 9999);
        TaskCommand taskCommand = StartTaskCommand.create(null, null);
        Assert.assertEquals(taskNetClient.buildUrl(supervisorEndpoint, taskCommand),
                "http://127.0.0.1:9999/task/command/start");
    }

    @Test
    public void testCommandSenderBuildNoneStartUrl() {
        TaskNetClient taskNetClient = new TaskNetClient();
        SupervisorEndpoint supervisorEndpoint = new SupervisorEndpoint("127.0.0.1", 9999);
        for (CommandType commandType : CommandType.values()) {
            if (commandType == CommandType.START) {
                continue;
            }
            TaskCommand taskCommand = GeneralTaskCommand.create(null, null, commandType);
            Assert.assertEquals(taskNetClient.buildUrl(supervisorEndpoint, taskCommand),
                    "http://127.0.0.1:9999/task/command/" + commandType.name().toLowerCase());
        }
    }
}
