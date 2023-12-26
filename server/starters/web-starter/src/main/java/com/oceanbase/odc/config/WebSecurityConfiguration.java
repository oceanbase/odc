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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.LocaleResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.oceanbase.odc.service.bastion.model.BastionProperties;
import com.oceanbase.odc.service.captcha.CaptchaAuthenticationProcessingFilter;
import com.oceanbase.odc.service.iam.auth.CustomAuthenticationEntryPoint;
import com.oceanbase.odc.service.iam.auth.CustomAuthenticationFailureHandler;
import com.oceanbase.odc.service.iam.auth.CustomAuthenticationSuccessHandler;
import com.oceanbase.odc.service.iam.auth.CustomInvalidSessionStrategy;
import com.oceanbase.odc.service.iam.auth.CustomLogoutSuccessHandler;
import com.oceanbase.odc.service.iam.auth.UsernamePasswordConfigureHelper;
import com.oceanbase.odc.service.iam.auth.bastion.BastionAuthenticationProcessingFilter;
import com.oceanbase.odc.service.iam.auth.bastion.BastionAuthenticationProvider;
import com.oceanbase.odc.service.iam.auth.bastion.BastionUserDetailService;
import com.oceanbase.odc.service.iam.auth.local.TestLoginAuthenticationFilter;
import com.oceanbase.odc.service.iam.auth.oauth2.OAuth2SecurityConfigureHelper;
import com.oceanbase.odc.service.iam.util.FailedLoginAttemptLimiter;

import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2021/7/30
 */

@Slf4j
@Profile("alipay")
@Configuration
@ConditionalOnProperty(value = {"odc.iam.auth.type"}, havingValue = "local")
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Value("${odc.iam.authentication.captcha.enabled:false}")
    private boolean captchaEnabled;

    @Autowired
    private LoadingCache<String, FailedLoginAttemptLimiter> clientAddressLoginAttemptCache;

    @Autowired
    private BastionUserDetailService bastionUserDetailService;

    @Autowired
    private CommonSecurityProperties commonSecurityProperties;

    @Autowired
    private CsrfConfigureHelper csrfConfigureHelper;

    @Autowired
    private CorsConfigureHelper corsConfigureHelper;

    @Autowired
    private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Autowired
    private CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LocaleResolver localeResolver;

    @Autowired
    private BastionProperties bastionProperties;

    @Autowired
    private UsernamePasswordConfigureHelper usernamePasswordConfigureHelper;

    @Autowired
    private DaoAuthenticationProvider daoAuthenticationProvider;

    @Autowired
    private OAuth2SecurityConfigureHelper oauth2SecurityConfigureHelper;

    private BastionAuthenticationProvider bastionAuthenticationProvider() {
        return new BastionAuthenticationProvider(bastionUserDetailService);
    }

    @Override
    public void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(daoAuthenticationProvider)
                .authenticationProvider(bastionAuthenticationProvider());
    }

    @Override
    public void configure(WebSecurity web) {
        // 允许访问静态资源
        web.ignoring().antMatchers(commonSecurityProperties.getStaticResources())
                .and().ignoring().antMatchers(commonSecurityProperties.getAuthWhitelist());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        corsConfigureHelper.configure(http);

        usernamePasswordConfigureHelper.configure(http, authenticationManager());
        oauth2SecurityConfigureHelper.configure(http);
        // @formatter:off
        http.exceptionHandling()
                .authenticationEntryPoint(new CustomAuthenticationEntryPoint(commonSecurityProperties.getLoginPage(),localeResolver))
            .and()
                .authorizeRequests()
                .anyRequest().authenticated()
            .and()
                .logout()
                .logoutUrl(commonSecurityProperties.getLogoutUri())
                .logoutSuccessHandler(new CustomLogoutSuccessHandler())
                .deleteCookies(commonSecurityProperties.getSessionCookieKey())
                .invalidateHttpSession(true).permitAll()
            .and()
                .sessionManagement()
                // SessionCreationPolicy.ALWAYS --> SessionCreationPolicy.IF_REQUIRED，防止登出后再次访问页面生成session造成无法跳转至登录页
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation()
                .migrateSession()
                .invalidSessionStrategy(new CustomInvalidSessionStrategy(commonSecurityProperties.getLoginPage(), localeResolver));

        // @formatter:on
        csrfConfigureHelper.configure(http);

        http.addFilterBefore(
                new TestLoginAuthenticationFilter(),
                OAuth2LoginAuthenticationFilter.class);

        if (bastionProperties.getAccount().isAutoLoginEnabled()) {
            http.addFilterBefore(bastionAuthenticationProcessingFilter(authenticationManager()),
                    UsernamePasswordAuthenticationFilter.class);
        }

        if (captchaEnabled) {
            http.addFilterBefore(getCaptchaAuthenticationProcessingFilter(),
                    UsernamePasswordAuthenticationFilter.class);
        }

    }

    private CaptchaAuthenticationProcessingFilter getCaptchaAuthenticationProcessingFilter() {
        CaptchaAuthenticationProcessingFilter captchaAuthenticationProcessingFilter =
                new CaptchaAuthenticationProcessingFilter(customAuthenticationFailureHandler,
                        clientAddressLoginAttemptCache);
        captchaAuthenticationProcessingFilter.setFilterProcessesUrl(commonSecurityProperties.getLoginUri());
        return captchaAuthenticationProcessingFilter;
    }


    private BastionAuthenticationProcessingFilter bastionAuthenticationProcessingFilter(
            AuthenticationManager authenticationManager) {
        BastionAuthenticationProcessingFilter filter =
                new BastionAuthenticationProcessingFilter(objectMapper);
        filter.setAuthenticationManager(authenticationManager);
        filter.setAuthenticationSuccessHandler(customAuthenticationSuccessHandler);
        filter.setAuthenticationFailureHandler(customAuthenticationFailureHandler);
        return filter;
    }
}
