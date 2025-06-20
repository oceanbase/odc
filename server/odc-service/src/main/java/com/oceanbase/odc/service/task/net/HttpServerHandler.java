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

import java.util.concurrent.ThreadPoolExecutor;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.common.util.UrlUtils;
import com.oceanbase.odc.service.task.executor.TraceDecoratorUtils;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2024/11/22 15:37
 */
@Slf4j
public class HttpServerHandler<T> extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final ThreadPoolExecutor bizThreadPool;
    private final RequestHandler<T> requestHandler;
    private final String moduleName;

    public HttpServerHandler(RequestHandler<T> executorRequestHandler, ThreadPoolExecutor bizThreadPool,
            String moduleName) {
        this.requestHandler = executorRequestHandler;
        this.bizThreadPool = bizThreadPool;
        this.moduleName = moduleName;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        // request parse
        // final byte[] requestBytes = ByteBufUtil.getBytes(msg.content()); //
        // byteBuf.toString(io.netty.util.CharsetUtil.UTF_8);
        String requestData = msg.content().toString(CharsetUtil.UTF_8);
        String uri = UrlUtils.decode(msg.uri());
        HttpMethod httpMethod = msg.method();
        boolean keepAlive = HttpUtil.isKeepAlive(msg);
        if (StringUtils.isNotBlank(uri)) {
            log.info("{} get uri {}", moduleName, uri);
        }
        if (StringUtils.isNotBlank(requestData)) {
            log.info("{} get requestData {}", moduleName, requestData);
        }

        // invoke
        bizThreadPool.execute(TraceDecoratorUtils.decorate(new Runnable() {
            @Override
            public void run() {
                T responseObj = null;
                try {
                    // do invoke
                    responseObj = requestHandler.process(httpMethod, uri, requestData);
                } catch (Throwable e) {
                    log.info("request handler failed", e);
                    responseObj = requestHandler.processException(e);
                }

                // to json
                String responseJson = JsonUtils.toJson(responseObj);

                // write response
                writeResponse(ctx, keepAlive, responseJson);
            }
        }));
    }

    /**
     * write response
     */
    private void writeResponse(ChannelHandlerContext ctx, boolean keepAlive, String responseJson) {
        // write response
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.copiedBuffer(responseJson, CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        if (keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        ctx.writeAndFlush(response);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("{} provider netty_http server caught exception", moduleName, cause);
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            ctx.channel().close(); // beat 3N, close if idle
            log.debug("{} provider netty_http server close an idle channel.", moduleName);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
