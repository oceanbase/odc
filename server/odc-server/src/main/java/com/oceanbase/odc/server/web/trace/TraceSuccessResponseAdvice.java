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

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.server.web.controller.RestExceptionHandler;
import com.oceanbase.odc.service.common.response.SuccessResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * A response advice, do below: <br>
 * 1. Attach trace info for success response; <br>
 * 2. Log request trace info. <br>
 * for error response, refer {@link RestExceptionHandler} for details
 * 
 * @author yizhou.xw
 * @version : TraceResponseAdvice.java, v 0.1 2021-02-22 17:37
 */
@Slf4j
@ControllerAdvice(basePackages = {"com.oceanbase.odc.server.web.controller.v2"})
public class TraceSuccessResponseAdvice implements ResponseBodyAdvice<SuccessResponse> {
    private static final String SERVER = SystemUtils.getHostName();

    @Autowired
    private WebTraceUtils webTraceUtils;

    @Override
    public boolean supports(MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> methodReturnType = returnType.getMethod().getReturnType();
        return SuccessResponse.class.isAssignableFrom(methodReturnType);
    }

    @Override
    public SuccessResponse beforeBodyWrite(SuccessResponse body, MethodParameter returnType,
            MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request, ServerHttpResponse response) {
        if (Objects.nonNull(body)) {
            body.setServer(SERVER);
            body.setTraceId(TraceContextHolder.getTraceId());
            if (StringUtils.isNotBlank(TraceContextHolder.getRequestId())) {
                body.setRequestId(TraceContextHolder.getRequestId());
            }

            body.setDurationMillis(TraceContextHolder.getDuration());
            body.setTimestamp(OffsetDateTime.ofInstant(
                    Instant.ofEpochMilli(TraceContextHolder.getStartEpochMilli()), ZoneId.systemDefault()));
        }
        return body;
    }
}
