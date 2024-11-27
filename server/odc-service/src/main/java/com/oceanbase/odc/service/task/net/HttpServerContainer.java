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
package com.oceanbase.odc.service.task.net;

import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2024/11/25 16:08
 */
@Slf4j
public abstract class HttpServerContainer<T> {
    protected RequestHandler<T> requestHandler;
    protected Thread thread;
    protected HttpServer<T> httpServer;

    public void start() {
        initHttpServer();
        httpServer.start();
        thread = createThread(httpServer::waitStop);
        thread.setDaemon(true); // daemon, service jvm, user thread leave >>> daemon leave >>> jvm leave
        thread.start();
    }

    /**
     * init http server
     */
    private void initHttpServer() {
        requestHandler = getRequestHandler();
        httpServer = new HttpServer<T>(new HttpServerContext<T>() {
            @Override
            public int listenPort() {
                return getPort();
            }

            @Override
            public String moduleName() {
                return getModuleName();
            }

            @Override
            public RequestHandler<T> requestHandler() {
                return requestHandler;
            }

            @Override
            public Consumer<Integer> portListener() {
                return portConsumer();
            }
        });
    }

    public void waitStop() {
        if (null != httpServer) {
            httpServer.waitStop();
        }
    }

    public void stop() throws Exception {
        // destroy server thread
        if (null != httpServer) {
            httpServer.stop();
        }
        if (null != thread) {
            // max wait 5 seconds
            thread.join(5000);
        }
        log.info("{} remoting server destroy success.", getModuleName());
    }

    /**
     * provide port for http server
     * 
     * @return
     */
    protected abstract int getPort();

    /**
     * provide request handler
     * 
     * @return
     */
    protected abstract RequestHandler<T> getRequestHandler();

    /**
     * provide module name
     */
    protected abstract String getModuleName();

    /**
     * provide thread create factory
     * 
     * @param r
     * @return
     */
    protected abstract Thread createThread(Runnable r);

    /**
     * provide port listener
     * 
     * @return
     */
    protected abstract Consumer<Integer> portConsumer();

}
