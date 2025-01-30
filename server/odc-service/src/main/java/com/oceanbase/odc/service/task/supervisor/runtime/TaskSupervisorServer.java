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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.google.common.annotations.VisibleForTesting;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.task.net.HttpServerContainer;
import com.oceanbase.odc.service.task.net.RequestHandler;
import com.oceanbase.odc.service.task.supervisor.protocol.TaskCommand;

import io.netty.handler.codec.http.HttpMethod;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author longpeng.zlp
 * @date 2024/10/29 18:05
 */
@Slf4j
public class TaskSupervisorServer extends HttpServerContainer<String> {
    // executor to handle command
    @Getter
    private final TaskCommandExecutor taskCommandExecutor;
    // port to listen
    private final int port;
    // deserialize task command
    private TaskCommandDeserializer taskCommandDeserializer = new TaskCommandDeserializer();
    private AtomicInteger serverPort = new AtomicInteger(-1);

    public TaskSupervisorServer(int port, TaskCommandExecutor taskCommandExecutor) {
        this.port = port;
        this.taskCommandExecutor = taskCommandExecutor;
    }

    @Override
    protected int getPort() {
        return port;
    }

    @Override
    protected RequestHandler<String> getRequestHandler() {
        return new RequestHandler<String>() {
            @Override
            public String process(HttpMethod httpMethod, String uri, String requestData) {
                // handle heartbeat request
                if (StringUtils.containsIgnoreCase(uri, "heartbeat")) {
                    return "true";
                } else if (StringUtils.containsIgnoreCase(uri, "memInfo")) {
                    return JsonUtils.toJson(EndpointInfo.getEndpointInfo());
                }
                try {
                    TaskCommand taskCommand = taskCommandDeserializer.deserializeTaskCommand(requestData);
                    return taskCommandExecutor.onCommand(taskCommand);
                } catch (Throwable e) {
                    return processException(e);
                }
            }

            @Override
            public String processException(Throwable e) {
                return e.getMessage();
            }
        };
    }

    @Override
    protected String getModuleName() {
        return "TaskSupervisor";
    }

    @Override
    protected Thread createThread(Runnable r) {
        return new Thread(r);
    }

    @Override
    protected Consumer<Integer> portConsumer() {
        return (p) -> serverPort.set(p);
    }

    @VisibleForTesting
    public int getServerPort() {
        return serverPort.get();
    }
}
