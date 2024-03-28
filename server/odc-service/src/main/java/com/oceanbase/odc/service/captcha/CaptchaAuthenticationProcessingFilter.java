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
package com.oceanbase.odc.service.captcha;

import static com.oceanbase.odc.service.captcha.CaptchaConstants.SESSION_KEY_VERIFICATION_CODE;

import java.io.IOException;
import java.util.Objects;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.service.iam.util.FailedLoginAttemptLimiter;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/5/31 下午4:44
 * @Description: []
 */
@Slf4j
public class CaptchaAuthenticationProcessingFilter extends OncePerRequestFilter {


    private final AuthenticationFailureHandler authenticationFailureHandler;

    private final LoadingCache<String, FailedLoginAttemptLimiter> clientAddressLoginAttemptCache;

    private RequestMatcher requiresAuthenticationRequestMatcher;

    public CaptchaAuthenticationProcessingFilter(AuthenticationFailureHandler authenticationFailureHandler,
            LoadingCache<String, FailedLoginAttemptLimiter> clientAddressLoginAttemptCache) {
        Verify.notNull(authenticationFailureHandler, "authenticationFailureHandler");
        Verify.notNull(clientAddressLoginAttemptCache, "clientAddressLoginAttemptCache");
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.clientAddressLoginAttemptCache = clientAddressLoginAttemptCache;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
            FilterChain filterChain)
            throws IOException, ServletException {
        if (requiresAuthenticationRequestMatcher.matches(httpServletRequest)) {
            try {
                validate(httpServletRequest);
            } catch (CaptchaAuthenticationException ex) {
                // 验证码验证失败，调用失败处理器
                authenticationFailureHandler.onAuthenticationFailure(httpServletRequest, httpServletResponse, ex);
                return;
            }
        }
        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    private boolean validate(HttpServletRequest httpServletRequest) throws CaptchaAuthenticationException {
        HttpSession session = httpServletRequest.getSession();
        VerificationCode verificationCodeInSession =
                (VerificationCode) session.getAttribute(SESSION_KEY_VERIFICATION_CODE);
        String verificationCode = httpServletRequest.getParameter("verificationCode");

        if (StringUtils.isBlank(verificationCode)) {
            throw new CaptchaAuthenticationException(ErrorCodes.BlankVerificationCode, null);
        }
        if (Objects.isNull(verificationCodeInSession)) {
            throw new CaptchaAuthenticationException(ErrorCodes.MissingVerificationCode, null);
        }
        if (verificationCodeInSession.isExpired()) {
            session.removeAttribute(SESSION_KEY_VERIFICATION_CODE);
            throw new CaptchaAuthenticationException(ErrorCodes.ExpiredVerificationCode, null);
        }
        if (!StringUtils.equals(verificationCodeInSession.getValue(), verificationCode)) {
            throw new CaptchaAuthenticationException(ErrorCodes.WrongVerificationCode, null);
        }
        session.removeAttribute(SESSION_KEY_VERIFICATION_CODE);
        return true;
    }

    /**
     * Sets the URL that determines if authentication is required
     *
     * @param filterProcessesUrl
     */
    public void setFilterProcessesUrl(String filterProcessesUrl) {
        setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher(
                filterProcessesUrl));
    }

    public final void setRequiresAuthenticationRequestMatcher(
            RequestMatcher requestMatcher) {
        Verify.notNull(requestMatcher, "requestMatcher");
        this.requiresAuthenticationRequestMatcher = requestMatcher;
    }
}
