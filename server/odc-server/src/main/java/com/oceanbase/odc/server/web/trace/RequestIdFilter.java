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

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.oceanbase.odc.common.trace.TraceContextHolder;

import lombok.extern.slf4j.Slf4j;

/**
 * RequestId filter
 *
 * @author yizhou.xw
 * @version : RequestIdFilter.java, v 0.1 2021-03-23 19:46
 */
@Slf4j
public class RequestIdFilter extends OncePerRequestFilter {
    private static final int MAX_REQUEST_ID_LENGTH = 36;

    public RequestIdFilter() {
        log.info("RequestIdFilter initialized.");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        String requestId = getRequestId(request);
        if (StringUtils.isNotBlank(requestId)) {
            if (requestId.length() > MAX_REQUEST_ID_LENGTH) {
                requestId = requestId.substring(requestId.length() - MAX_REQUEST_ID_LENGTH);
            }
            TraceContextHolder.setRequestId(requestId);
        }
        filterChain.doFilter(request, response);
    }

    private String getRequestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        if (StringUtils.isNotBlank(requestId)) {
            return requestId;
        }
        requestId = request.getParameter("requestId");
        if (StringUtils.isNotBlank(requestId)) {
            return requestId;
        }
        return request.getParameter("RequestId");
    }
}
