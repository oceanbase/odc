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
package com.oceanbase.odc;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.authority.DefaultLoginSecurityManager;
import com.oceanbase.odc.core.authority.SecurityManager;
import com.oceanbase.odc.core.authority.session.SecuritySession;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.service.common.util.WebRequestUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/8/2 17:52
 * @Description: []
 */
@Component
@Profile("clientMode")
@Slf4j
public class DesktopSecurityFilter implements Filter {

    @Autowired
    private SecurityManager securityManager;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // Limit only localhost access odc api in client mode
        PreConditions.validHasPermission(WebRequestUtils.isLocalRequest(request),
                ErrorCodes.AccessDenied, "Request access denied, remote address=" + request.getRemoteAddr());

        SecuritySession session = securityManager.getSession(null);
        if (session == null) {
            log.info("Can not get the session from request for security framework, requestURI={}",
                    ((HttpServletRequest) request).getRequestURI());
            DefaultLoginSecurityManager.removeContext();
            DefaultLoginSecurityManager.removeSecurityContext();
        }
        chain.doFilter(request, response);
    }

}
