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

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.task.util.HttpClientUtils;

import io.netty.handler.codec.http.HttpMethod;

/**
 * @author longpeng.zlp
 * @date 2024/11/25 10:55
 */
public class HttpServerTest {
    private HttpServerContext<String> httpServerContext;
    private HttpServer<String> httpServer;
    private Thread serverThread;
    private AtomicInteger startedPort = new AtomicInteger(0);
    private SimpleRequestHandler simpleRequestHandler;
    private CountDownLatch countDownLatch = new CountDownLatch(1);

    @Before
    public void setUp() throws InterruptedException {
        simpleRequestHandler = new SimpleRequestHandler();
        httpServerContext = new HttpServerContext<String>() {
            @Override
            public int listenPort() {
                return 0;
            }

            @Override
            public String moduleName() {
                return "testModule";
            }

            @Override
            public RequestHandler<String> requestHandler() {
                return simpleRequestHandler;
            }

            @Override
            public Consumer<Integer> portListener() {
                return (port) -> {
                    startedPort.set(port);
                    countDownLatch.countDown();
                };
            }
        };
        httpServer = new HttpServer<>(httpServerContext);
        httpServer.start();
        serverThread = new Thread(httpServer::waitStop);
        serverThread.start();
        countDownLatch.await();
    }

    @After
    public void shutdown() throws InterruptedException {
        httpServer.stop();
        serverThread.join();
    }

    @Test
    public void testHttpServerProcessRequest() throws IOException {
        Assert.assertEquals(httpServer.getRealListenPort(), startedPort.get());
        // send command
        int port = httpServer.getRealListenPort();
        Assert.assertEquals("getResult", HttpClientUtils.request("GET", "http://127.0.0.1:" + port + "/api/get",
                new TypeReference<String>() {}));
        Assert.assertEquals("postResult", HttpClientUtils.request("POST", "http://127.0.0.1:" + port + "/api/post",
                "{}", new TypeReference<String>() {}));
    }

    @Test
    public void testHttpServerProcessRequestThrowsException() throws IOException {
        simpleRequestHandler.setShouldThrowsException(true);
        // send command
        int port = httpServer.getRealListenPort();
        Assert.assertEquals("exception throws", HttpClientUtils.request("GET", "http://127.0.0.1:" + port + "/api/get",
                new TypeReference<String>() {}));
    }

    private static final class SimpleRequestHandler implements RequestHandler<String> {
        private boolean shouldThrowsException = false;

        public void setShouldThrowsException(boolean shouldThrowsException) {
            this.shouldThrowsException = shouldThrowsException;
        }

        @Override
        public String process(HttpMethod httpMethod, String uri, String requestData) {
            if (shouldThrowsException) {
                throw new RuntimeException("exception throws");
            }
            if (StringUtils.contains(uri, "get")) {
                return "getResult";
            } else {
                return "postResult";
            }
        }

        @Override
        public String processException(Throwable e) {
            return e.getMessage();
        }
    }
}
