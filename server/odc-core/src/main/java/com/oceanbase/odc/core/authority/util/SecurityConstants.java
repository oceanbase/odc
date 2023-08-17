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
package com.oceanbase.odc.core.authority.util;

import com.oceanbase.odc.core.authority.session.SecuritySession;

/**
 * Some constaint value for security framework
 *
 * @author yh263208
 * @date 2021-07-14 19>58
 * @since ODC_release_3.2.0
 */
public class SecurityConstants {
    /**
     * Parameter name for timeout
     */
    public static final String CONTEXT_SESSION_TIMEOUT_KEY = "timeoutmilliseconds";
    /**
     * Parameter name for {@link SecuritySession} host
     */
    public static final String CONTEXT_SESSION_HOST_KEY = "host";
    /**
     * Default setting's value for {@link SecuritySession} timeout(milli-second)
     */
    public static final long DEFAULT_SESSION_TIMEOUT_MILLIS = 28800000;
    /**
     * Default setting's value for {@link SecuritySession} scan interval
     */
    public static final long DEFAULT_SCAN_INTERVAL_MILLIS = 1000;
    /**
     * Parameter name for {@link SecuritySession} in {@code Map<String, Object> contexts} Used to store
     * ID for a certain {@link SecuritySession}
     */
    public static final String CONTEXT_SESSION_ID_KEY = "SECURITY_SESSION_ID";
    /**
     * Parameter name for {@link javax.servlet.ServletRequest} in {@code Map<String, Object> contexts}
     * Used to store a {@link javax.servlet.ServletRequest}>
     */
    public static final String CONTEXT_SERVLET_REQUEST_KEY = "SERVLET_REQUEST";
    /**
     * Parameter name for {@link javax.servlet.ServletResponse} in {@code Map<String, Object> contexts}
     * Used to store a {@link javax.servlet.ServletResponse}
     */
    public static final String CONTEXT_SERVLET_RESPONSE_KEY = "SERVLET_RESPONSE";
    /**
     * Parameter name for {@link SecuritySession} in
     * {@link javax.servlet.http.HttpSession#getAttribute(String)} Used to store a <code>Session</code>
     */
    public static final String HTTPSESSION_SECURITY_SESSION_KEY = "SECURITY_SESSION";
    /**
     * Name for {@link javax.servlet.http.Cookie} object in
     * {@link javax.servlet.http.HttpServletRequest}
     */
    public static final String CUSTOM_COOKIE_NAME = "ODC_SECURITY_TOKEN";
    /**
     * Key in {@link SecuritySession} object for current {@link javax.security.auth.Subject}
     */
    public static final String SECURITY_SESSION_SUBJECT_KEY = "ODC_SECURITY_SUBJECT";

}
