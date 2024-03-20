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
package com.oceanbase.odc.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.oceanbase.odc.core.authority.SecurityManager;
import com.oceanbase.odc.service.encryption.SensitivePropertyHandler;
import com.oceanbase.odc.service.iam.auth.CustomAuthenticationFailureHandler;
import com.oceanbase.odc.service.iam.auth.CustomAuthenticationSuccessHandler;
import com.oceanbase.odc.service.iam.auth.CustomJwtAuthenticationSuccessHandler;
import com.oceanbase.odc.service.iam.auth.UsernamePasswordConfigureHelper;
import com.oceanbase.odc.service.iam.auth.local.LocalDaoAuthenticationProvider;
import com.oceanbase.odc.service.iam.util.FailedLoginAttemptLimiter;

@Configuration
public class SecurityBeanConfiguration {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SecurityManager securityManager;

    @Autowired
    private LoadingCache<String, FailedLoginAttemptLimiter> clientAddressLoginAttemptCache;

    @Autowired
    private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Autowired
    private CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

    @Autowired
    private SensitivePropertyHandler sensitivePropertyHandler;

    @Autowired
    private CommonSecurityProperties commonSecurityProperties;

    @Autowired
    private CustomJwtAuthenticationSuccessHandler customJwtAuthenticationSuccessHandler;


    @Bean
    public LocalDaoAuthenticationProvider authenticationProvider() {
        LocalDaoAuthenticationProvider provider = new LocalDaoAuthenticationProvider();
        provider.setHideUserNotFoundExceptions(false);
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public UsernamePasswordConfigureHelper usernamePasswordConfigureHelper() {
        return new UsernamePasswordConfigureHelper(securityManager, clientAddressLoginAttemptCache,
                sensitivePropertyHandler, customAuthenticationSuccessHandler, customAuthenticationFailureHandler,
                commonSecurityProperties);
    }

    @Bean
    public UsernamePasswordConfigureHelper jwtUsernamePasswordConfigureHelper() {
        return new UsernamePasswordConfigureHelper(securityManager, clientAddressLoginAttemptCache,
                sensitivePropertyHandler, customJwtAuthenticationSuccessHandler, customAuthenticationFailureHandler,
                commonSecurityProperties);
    }
}
