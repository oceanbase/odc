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
package com.oceanbase.odc.service.task.config;

import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;

import lombok.extern.slf4j.Slf4j;

/**
 * Check remote ip is valid when task-framework is running in process mode.
 *
 * @author yaobin
 * @date 2024-03-04
 * @since 4.2.4
 */
@Slf4j
@Aspect
@Component
public class JobSecurityCheckerAspect {

    @Autowired
    private TaskFrameworkProperties taskFrameworkProperties;

    @Pointcut("@annotation(com.oceanbase.odc.service.task.config.JobSecurityChecker)")
    public void beforeRequest() {}

    @Before("beforeRequest()")
    public void checkRemoteAddress(JoinPoint point) {
        if (taskFrameworkProperties.getRunMode().isK8s()) {
            return;
        }
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null && request.getHeader("X-Forwarded-For") != null) {
            remoteAddr = request.getHeader("X-Forwarded-For");
        }

        boolean isLocalhost = "127.0.0.1".equals(remoteAddr) || "0:0:0:0:0:0:0:1".equals(remoteAddr)
                || "::1".equals(remoteAddr) || "localhost".equalsIgnoreCase(remoteAddr);
        PreConditions.validHasPermission(isLocalhost, ErrorCodes.AccessDenied,
                "Request access denied, ip=" + remoteAddr);
    }

}
