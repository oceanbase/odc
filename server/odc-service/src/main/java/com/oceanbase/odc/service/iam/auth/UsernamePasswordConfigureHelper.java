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
package com.oceanbase.odc.service.iam.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.oceanbase.odc.config.CommonSecurityProperties;
import com.oceanbase.odc.core.authority.SecurityManager;
import com.oceanbase.odc.service.encryption.SensitivePropertyHandler;
import com.oceanbase.odc.service.iam.util.FailedLoginAttemptLimiter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UsernamePasswordConfigureHelper {

    private final SecurityManager securityManager;
    private final LoadingCache<String, FailedLoginAttemptLimiter> clientAddressLoginAttemptCache;
    private final SensitivePropertyHandler sensitivePropertyHandler;

    private final AuthenticationSuccessHandler successHandler;

    private final AuthenticationFailureHandler failureHandler;

    private final CommonSecurityProperties commonSecurityProperties;

    public UsernamePasswordConfigureHelper(SecurityManager securityManager,
            LoadingCache<String, FailedLoginAttemptLimiter> clientAddressLoginAttemptCache,
            SensitivePropertyHandler sensitivePropertyHandler, AuthenticationSuccessHandler successHandler,
            AuthenticationFailureHandler failureHandler, CommonSecurityProperties commonSecurityProperties) {
        this.securityManager = securityManager;
        this.clientAddressLoginAttemptCache = clientAddressLoginAttemptCache;
        this.sensitivePropertyHandler = sensitivePropertyHandler;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.commonSecurityProperties = commonSecurityProperties;
    }


    public void configure(HttpSecurity http, AuthenticationManager authenticationManager)
            throws Exception {
        if (commonSecurityProperties.authTypeContainsLocal()) {
            http.addFilterAt(
                    getCustomUsernamePasswordAuthenticationFilter(authenticationManager),
                    UsernamePasswordAuthenticationFilter.class)
                    .formLogin()
                    .loginPage(commonSecurityProperties.getLoginPage()).permitAll()
                    .loginProcessingUrl(commonSecurityProperties.getLoginUri());
        }
        if (commonSecurityProperties.isBasicAuthenticationEnabled()) {
            log.info("Basic authentication is enabled, it is not recommended in production environment.");
            http.httpBasic();
        }
    }

    private CustomUsernamePasswordAuthenticationFilter getCustomUsernamePasswordAuthenticationFilter(
            AuthenticationManager authenticationManager) {
        CustomUsernamePasswordAuthenticationFilter filter =
                new CustomUsernamePasswordAuthenticationFilter(securityManager, clientAddressLoginAttemptCache,
                        sensitivePropertyHandler);
        filter.setAuthenticationManager(authenticationManager);
        filter.setAuthenticationSuccessHandler(successHandler);
        filter.setAuthenticationFailureHandler(failureHandler);
        filter.setFilterProcessesUrl(commonSecurityProperties.getLoginUri());
        return filter;
    }
}
