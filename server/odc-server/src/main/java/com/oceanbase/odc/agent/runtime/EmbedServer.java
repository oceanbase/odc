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
package com.oceanbase.odc.agent.runtime;

import java.util.function.Consumer;

import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.task.executor.TraceDecoratorUtils;
import com.oceanbase.odc.service.task.net.HttpServerContainer;
import com.oceanbase.odc.service.task.net.RequestHandler;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-12-13
 * @since 4.2.4
 */
@Slf4j
class EmbedServer extends HttpServerContainer<SuccessResponse<Object>> {
    @Override
    protected int getPort() {
        int port;
        if (JobUtils.getExecutorPort().isPresent()) {
            // start with assigned port
            port = JobUtils.getExecutorPort().get();
        } else {
            port = 0;
        }
        return port;
    }

    @Override
    protected RequestHandler<SuccessResponse<Object>> getRequestHandler() {
        return new ExecutorRequestHandler();
    }

    @Override
    protected String getModuleName() {
        return "odc-job";
    }

    @Override
    protected Thread createThread(Runnable r) {
        return new Thread(TraceDecoratorUtils.decorate(r));
    }

    @Override
    protected Consumer<Integer> portConsumer() {
        return JobUtils::setExecutorPort;
    }
}
