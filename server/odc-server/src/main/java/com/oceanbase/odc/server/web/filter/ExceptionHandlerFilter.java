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
package com.oceanbase.odc.server.web.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.oceanbase.odc.common.util.ExceptionUtils;
import com.oceanbase.odc.service.common.response.Error;
import com.oceanbase.odc.service.common.response.ErrorResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.util.WebResponseUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class ExceptionHandlerFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (Throwable e) {
            handle(e, request, response);
        }
    }

    private void handle(Throwable throwable, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        log.warn("Request failed, uri: {}", request.getRequestURI(), throwable);
        ErrorResponse errorResponse =
                Responses.error(HttpStatus.BAD_REQUEST, Error.ofBadRequest(ExceptionUtils.getRootCause(throwable)));
        WebResponseUtils.writeJsonObjectWithBadRequestStatus(errorResponse, request, response);
    }

}
