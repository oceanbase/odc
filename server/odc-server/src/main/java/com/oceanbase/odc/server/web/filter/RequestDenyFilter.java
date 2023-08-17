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
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.exception.BadRequestException;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/5/17 16:42
 * @Description: []
 */

@Slf4j
@Component
public class RequestDenyFilter extends OncePerRequestFilter {
    @Value("${odc.web.security.denied-http-requests:{}}")
    private String deniedHttpRequests;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        List<HttpRequest> blackList = JsonUtils.fromJsonList(deniedHttpRequests, HttpRequest.class);
        if (CollectionUtils.isEmpty(blackList)) {
            filterChain.doFilter(request, response);
        } else {
            boolean denied = blackList.stream().anyMatch(req -> {
                AntPathRequestMatcher matcher =
                        new AntPathRequestMatcher(req.getAntMatcherPath(), req.getMethod(), false);
                return matcher.matches(request);
            });
            if (denied) {
                log.warn("Request has been blocked, uri={}", request.getRequestURI());
                throw new BadRequestException("Not supposed to call this api");
            } else {
                filterChain.doFilter(request, response);
            }
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class HttpRequest {
        private String antMatcherPath;

        private String method;
    }
}
