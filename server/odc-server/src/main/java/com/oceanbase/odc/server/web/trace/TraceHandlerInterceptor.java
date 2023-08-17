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
package com.oceanbase.odc.server.web.trace;

import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.common.util.SystemUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * for trace info initialize and destroy for each request
 */
@Slf4j
@Component
public class TraceHandlerInterceptor implements HandlerInterceptor {

    private static final String SERVER = SystemUtils.getHostName();


    @Autowired
    private WebTraceUtils webTraceUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        TraceContextHolder.trace();
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception e) {
        try {
            String userId = webTraceUtils.isAuthRequired() ? webTraceUtils.currentUserId() : "Not-Required";
            String clientAddress = webTraceUtils.clientAddress();
            String userAgent = webTraceUtils.userAgent();
            String fullURL = webTraceUtils.fullURL();
            String methodName = getReturnType(handler);
            long rt = TraceContextHolder.getDuration();
            String httpMethod = request.getMethod();
            int httpStatus = response.getStatus();
            log.info(
                    "userId={}, serverName={}, httpMethod={}, httpStatus={}, odcCode={}, fullUrl={}, methodName={}, rt={}, userAgent={}, clientAddress={}",
                    userId, SERVER, httpMethod,
                    httpStatus, TraceContextHolder.getOdcCode(), fullURL,
                    methodName, rt, userAgent, clientAddress);
        } finally {
            TraceContextHolder.clear();
        }
    }

    private String getReturnType(Object handler) {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            return method.getName();
        }
        return "";
    }
}
