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

import java.net.InetSocketAddress;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.oceanbase.odc.service.task.executor.TraceDecoratorThreadFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2024/11/22 15:17
 */
@Slf4j
public class HttpServer<T> {
    // port expect to listen
    private final HttpServerContext<T> serverContext;
    // real port listened
    private int realListenPort;
    // thread pool to do things
    private ThreadPoolExecutor requestExecutor;
    // start flag
    private AtomicBoolean started = new AtomicBoolean(false);
    // stopped flag, set when exception occur or stop method called
    private AtomicBoolean stopped = new AtomicBoolean(false);
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channel;

    public HttpServer(HttpServerContext<T> serverContext) {
        this.serverContext = serverContext;
    }

    // start http server
    public synchronized void start() {
        if (!started.compareAndSet(false, true)) {
            log.info("http server for {} has been started", serverContext.moduleName());
            return;
        }
        // param
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        requestExecutor = createThreadPool();
        try {
            // start server
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline()
                                    .addLast(new IdleStateHandler(0, 0, 30 * 3, TimeUnit.SECONDS)) // beat 3N,
                                    // close if
                                    // idle
                                    .addLast(new HttpServerCodec())
                                    .addLast(new HttpObjectAggregator(5 * 1024 * 1024)) // merge request &
                                    // response to FULL
                                    .addLast(new HttpServerHandler<>(serverContext.requestHandler(), requestExecutor,
                                            serverContext.moduleName()));
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            int expectListenPort = serverContext.listenPort();
            // start with random port
            ChannelFuture future = bootstrap.bind(expectListenPort).sync();
            channel = future.channel();
            InetSocketAddress localAddress = (InetSocketAddress) channel.localAddress();
            // save port to system properties
            realListenPort = localAddress.getPort();
            serverContext.portListener().accept(realListenPort);

            log.info("{} remoting server start success, nettype = {}, port = {}, listenPort = {}",
                    serverContext.moduleName(), serverContext.requestHandler().getClass(), expectListenPort,
                    realListenPort);
        } catch (InterruptedException e) {
            log.info("{} remoting server stop.", serverContext.moduleName());
            stopped.set(true);
        } catch (Exception e) {
            log.error("{} remoting server error.", serverContext.moduleName(), e);
            stopped.set(true);
        }
    }

    public void waitStop() {
        try {
            if (stopped.get()) {
                log.info("stop flag has been set");
                return;
            }
            if (null != channel) {
                log.info("wait for channel future stop");
                channel.closeFuture().sync();
            }
            log.info("channel stopped, quit waitStop");
        } catch (Throwable e) {
            log.error("{} remoting server error.", serverContext.moduleName(), e);
        }
    }

    /**
     * stop http server
     */
    public void stop() {
        stopped.set(true);
        if (null != channel) {
            channel.close();
        }
        synchronized (this) {
            shutDownEventLoop(workerGroup);
            shutDownEventLoop(bossGroup);
        }
        if (null != requestExecutor) {
            requestExecutor.shutdown();
        }
        log.info("HttpServer shutdown invoked");
    }

    /**
     * get real listen port
     * 
     * @return
     */
    public int getRealListenPort() {
        return realListenPort;
    }

    private ThreadPoolExecutor createThreadPool() {
        return new ThreadPoolExecutor(
                0,
                128,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(64),
                new TraceDecoratorThreadFactory(new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r,
                                serverContext.moduleName() + ", EmbedServer bizThreadPool-" + r.hashCode());
                    }
                }),
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        throw new RuntimeException(
                                serverContext.moduleName() + ", EmbedServer bizThreadPool is EXHAUSTED!");
                    }
                });
    }

    private void shutDownEventLoop(EventLoopGroup eventLoopGroup) {
        if (null == eventLoopGroup) {
            return;
        }
        // stop
        try {
            Future<?> shutdownFuture = eventLoopGroup.shutdownGracefully();
            shutdownFuture.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
