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

import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.config.CommonSecurityProperties;
import com.oceanbase.odc.service.common.util.WebRequestUtils;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

import lombok.extern.slf4j.Slf4j;

/**
 * for trace framework internal usage
 * 
 * @author yizhou.xw
 * @version : WebTraceUtils.java, v 0.1 2021-07-30 17:02
 */
@Slf4j
@Component
public class WebTraceUtils {
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private HttpServletRequest servletRequest;
    @Autowired
    private CommonSecurityProperties commonSecurityProperties;

    String currentUserId() {
        try {
            return "" + authenticationFacade.currentUserId();
        } catch (Exception ex) {
            log.warn("get current user failed, message={}", ex.getMessage());
            return "N/A";
        }
    }

    boolean isAuthRequired() {
        if (Objects.isNull(servletRequest)) {
            return false;
        }
        boolean urlInWhiteList = Arrays.stream(commonSecurityProperties.getAuthWhitelist())
                .anyMatch(url -> StringUtils.containsIgnoreCase(servletRequest.getRequestURI(), url));
        return !urlInWhiteList;
    }

    String clientAddress() {
        return WebRequestUtils.getClientAddress(servletRequest);
    }

    String userAgent() {
        return WebRequestUtils.getUserAgent(servletRequest);
    }

    String fullURL() {
        return WebRequestUtils.getRequestFullURL(servletRequest);
    }

    String method(MethodParameter returnType) {
        Executable executable = returnType.getExecutable();
        String controllerClassName = returnType.getExecutable().getDeclaringClass().getSimpleName();
        String methodName = executable.getName();
        return controllerClassName + "." + methodName;
    }
}
