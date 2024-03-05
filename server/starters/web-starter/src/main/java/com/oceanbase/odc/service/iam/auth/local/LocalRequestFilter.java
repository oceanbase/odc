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
package com.oceanbase.odc.service.iam.auth.local;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.service.common.util.WebRequestUtils;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;

/**
 * @author yaobin
 * @date 2024-03-04
 * @since 4.2.4
 */
@Component
public class LocalRequestFilter extends OncePerRequestFilter {

    private static String[] limitLocalAccessList = new String[] {
            "/api/v2/task/heart",
            "/api/v2/task/result",
            "/api/v2/task/querySensitiveColumn"};

    @Autowired
    private TaskFrameworkProperties taskFrameworkProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Limit only localhost access odc api
        if (taskFrameworkProperties.getRunMode().isProcess() &&
                Arrays.stream(limitLocalAccessList)
                        .anyMatch(url -> StringUtils.containsIgnoreCase(request.getRequestURI(), url))) {
            PreConditions.validHasPermission(WebRequestUtils.isLocalRequest(request),
                    ErrorCodes.AccessDenied,
                    "Current api can only access from localhost, remote address=" + request.getRemoteAddr());
        }
        filterChain.doFilter(request, response);
    }
}
