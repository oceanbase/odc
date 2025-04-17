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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.web.servlet.LocaleResolver;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.oceanbase.odc.core.authority.SecurityManager;
import com.oceanbase.odc.service.iam.LoginHistoryService;
import com.oceanbase.odc.service.iam.auth.CustomAuthenticationEntryPoint;
import com.oceanbase.odc.service.iam.auth.CustomAuthenticationFailureHandler;
import com.oceanbase.odc.service.iam.auth.CustomAuthenticationSuccessHandler;
import com.oceanbase.odc.service.iam.auth.CustomInvalidSessionStrategy;
import com.oceanbase.odc.service.iam.auth.CustomLogoutSuccessHandler;
import com.oceanbase.odc.service.iam.auth.play.PlaysiteOpenAPIClient;
import com.oceanbase.odc.service.iam.auth.play.PlaysiteOpenAPIConstants;
import com.oceanbase.odc.service.iam.auth.play.PlaysitePreAuthenticatedProcessingFilter;
import com.oceanbase.odc.service.iam.util.FailedLoginAttemptLimiter;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2021/12/17 下午7:24
 * @Description: [Trial lab security configurations]
 */
@Profile("alipay")
@Slf4j
@Configuration
@ConditionalOnProperty(value = "odc.iam.auth.type", havingValue = "alipay")
public class PlaySiteSecurityConfiguration {
    private static final String LOGIN_PAGE = "/index.html";
    @Autowired
    @Qualifier("alipayUserDetailService")
    public AuthenticationUserDetailsService userDetailsService;
    @Autowired
    private LoadingCache<String, FailedLoginAttemptLimiter> clientAddressLoginAttemptCache;
    @Autowired
    private CommonSecurityProperties commonSecurityProperties;

    @Autowired
    private PlaysiteOpenAPIClient alipayOpenAPIClient;

    @Autowired
    private CsrfConfigureHelper csrfConfigureHelper;

    @Autowired
    private CorsConfigureHelper corsConfigureHelper;

    @Autowired
    private LoginHistoryService loginHistoryService;

    @Autowired
    private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Autowired
    private CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

    @Autowired
    private LocaleResolver localeResolver;

    @Autowired
    private SecurityManager securityManager;

    @Bean
    public PreAuthenticatedAuthenticationProvider preAuthProvider() {
        PreAuthenticatedAuthenticationProvider preAuthProvider = new PreAuthenticatedAuthenticationProvider();
        preAuthProvider.setPreAuthenticatedUserDetailsService(userDetailsService);
        return preAuthProvider;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers(commonSecurityProperties.getStaticResources())
                .and()
                .ignoring().requestMatchers(commonSecurityProperties.getAuthWhitelist());
    }

    @Bean
    public SecurityFilterChain localFilterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(preAuthProvider());
        configure(http);
        return http.build();
    }


    protected void configure(HttpSecurity http) throws Exception {
        corsConfigureHelper.configure(http);
        // @formatter:off
        http.exceptionHandling(e->e.authenticationEntryPoint(new CustomAuthenticationEntryPoint(LOGIN_PAGE,localeResolver)));

        http.authorizeHttpRequests(auth ->
            auth.anyRequest().authenticated() // 任何请求都需要认证
        );

        http.formLogin(l->l.loginPage(LOGIN_PAGE).permitAll());
        http.logout(l->l.logoutUrl(commonSecurityProperties.getLogoutUri())
            .deleteCookies(PlaysiteOpenAPIConstants.OB_OFFICIAL_WEBSITE_TOKEN_COOKIE_NAME, commonSecurityProperties.getSessionCookieKey())
            .invalidateHttpSession(true)
            .permitAll()
            .logoutSuccessHandler(new CustomLogoutSuccessHandler()));

        // Never: Spring Security will never create an HttpSession, but will use the existing one
      http.sessionManagement(s-> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
          .sessionFixation()
          .migrateSession()
          .invalidSessionStrategy(new CustomInvalidSessionStrategy(commonSecurityProperties.getLoginPage(), localeResolver)));
                
        // @formatter:on        
        csrfConfigureHelper.configure(http);

        AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManager.class);
        // 调用 Alipay OpenAPI 获取用户信息，并认证登录
        http.addFilterAt(getAuthenticationFilter(authenticationManager),
                AbstractPreAuthenticatedProcessingFilter.class);
    }

    private PlaysitePreAuthenticatedProcessingFilter getAuthenticationFilter(
            AuthenticationManager authenticationManager) {
        PlaysitePreAuthenticatedProcessingFilter filter =
                new PlaysitePreAuthenticatedProcessingFilter(alipayOpenAPIClient, securityManager, localeResolver);
        filter.setAuthenticationManager(authenticationManager);
        filter.setAuthenticationSuccessHandler(customAuthenticationSuccessHandler);
        filter.setAuthenticationFailureHandler(customAuthenticationFailureHandler);
        return filter;
    }
}
