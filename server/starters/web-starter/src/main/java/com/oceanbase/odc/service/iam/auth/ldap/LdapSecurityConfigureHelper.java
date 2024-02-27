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
package com.oceanbase.odc.service.iam.auth.ldap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.oceanbase.odc.service.encryption.SensitivePropertyHandler;
import com.oceanbase.odc.service.iam.auth.CustomAuthenticationFailureHandler;
import com.oceanbase.odc.service.iam.auth.CustomAuthenticationSuccessHandler;
import com.oceanbase.odc.service.iam.util.FailedLoginAttemptLimiter;
import com.oceanbase.odc.service.integration.ldap.LdapConfigRegistrationManager;
import com.oceanbase.odc.service.integration.oauth2.TestLoginManager;

@Component
public class LdapSecurityConfigureHelper {

    @Autowired
    private SensitivePropertyHandler sensitivePropertyHandler;

    @Autowired
    private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Autowired
    private CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

    @Autowired
    private TestLoginManager testLoginManager;

    @Autowired
    private LoadingCache<String, FailedLoginAttemptLimiter> clientAddressLoginAttemptCache;

    @Autowired
    private LdapConfigRegistrationManager ldapConfigRegistrationManager;

    public void configure(HttpSecurity http, AuthenticationManager authenticationManager)
            throws Exception {
        LdapUsernamePasswordAuthenticationFilter ldapUsernamePasswordAuthenticationFilter =
                new LdapUsernamePasswordAuthenticationFilter(sensitivePropertyHandler,
                        authenticationManager, clientAddressLoginAttemptCache, ldapConfigRegistrationManager);
        ldapUsernamePasswordAuthenticationFilter.setAuthenticationSuccessHandler(customAuthenticationSuccessHandler);
        ldapUsernamePasswordAuthenticationFilter.setAuthenticationFailureHandler(customAuthenticationFailureHandler);
        http.addFilterBefore(ldapUsernamePasswordAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        http.addFilterBefore(
                new LdapAbstractTestLoginAuthenticationFilter(testLoginManager),
                LdapUsernamePasswordAuthenticationFilter.class);
    }
}
