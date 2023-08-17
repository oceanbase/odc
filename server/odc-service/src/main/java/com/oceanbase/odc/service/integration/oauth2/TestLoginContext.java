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
package com.oceanbase.odc.service.integration.oauth2;

import static com.oceanbase.odc.service.integration.model.SSOIntegrationConfig.parseRegistrationName;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

public final class TestLoginContext {

    private static final String REGISTRATION_ID_URI_VARIABLE_NAME = "registrationId";
    private static final AntPathRequestMatcher authorizationRequestMatcher = new AntPathRequestMatcher(
            "/login/oauth2/code" + "/{" + REGISTRATION_ID_URI_VARIABLE_NAME + "}");
    private static final ThreadLocal<SecurityContext> TEST_SECURITY_CONTEXT_HOLDER = new ThreadLocal<>();

    private TestLoginContext() {}

    public static boolean isTestLoginRequest(HttpServletRequest request) {
        String registrationId = resolveRegistrationId(request);
        if (registrationId == null) {
            return false;
        }
        return "test".equals(parseRegistrationName(registrationId));
    }

    public static void stashTestLoginSecurityContext(HttpServletRequest request) {
        if (isTestLoginRequest(request)) {
            TEST_SECURITY_CONTEXT_HOLDER.set(SecurityContextHolder.getContext());
        }
    }

    public static void restoreTestLoginSecurityContext(HttpServletRequest request) {
        if (isTestLoginRequest(request)) {
            SecurityContextHolder.setContext(TEST_SECURITY_CONTEXT_HOLDER.get());
        }
    }

    public static void removeSecurityContext() {
        TEST_SECURITY_CONTEXT_HOLDER.remove();
    }

    private static String resolveRegistrationId(HttpServletRequest request) {
        if (authorizationRequestMatcher.matches(request)) {
            return authorizationRequestMatcher.matcher(request).getVariables()
                    .get(REGISTRATION_ID_URI_VARIABLE_NAME);
        }
        return null;
    }
}
