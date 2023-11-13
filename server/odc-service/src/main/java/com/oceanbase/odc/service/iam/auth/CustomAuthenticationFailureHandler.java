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

import static com.oceanbase.odc.core.shared.constant.OdcConstants.ODC_BACK_URL_PARAM;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedCredentialsNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.AttemptLoginOverLimitException;
import com.oceanbase.odc.core.shared.exception.OverLimitException;
import com.oceanbase.odc.metadb.iam.LoginHistoryEntity.FailedReason;
import com.oceanbase.odc.service.captcha.CaptchaAuthenticationException;
import com.oceanbase.odc.service.common.response.Error;
import com.oceanbase.odc.service.common.response.ErrorResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.util.WebRequestUtils;
import com.oceanbase.odc.service.common.util.WebResponseUtils;
import com.oceanbase.odc.service.iam.LoginHistoryService;
import com.oceanbase.odc.service.iam.model.LoginHistory;
import com.oceanbase.odc.service.iam.util.FailedLoginAttemptLimiter;

import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2021/8/4
 */
@Slf4j
@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final LoadingCache<String, FailedLoginAttemptLimiter> clientAddressLoginAttemptCache;
    private final LoginHistoryService loginHistoryService;
    private final LocaleResolver localeResolver;

    public CustomAuthenticationFailureHandler(
            LoadingCache<String, FailedLoginAttemptLimiter> clientAddressLoginAttemptCache,
            LoginHistoryService loginHistoryService,
            LocaleResolver localeResolver) {
        Validate.notNull(clientAddressLoginAttemptCache, "clientAddressLoginAttemptCache");
        Validate.notNull(loginHistoryService, "loginHistoryService");
        this.clientAddressLoginAttemptCache = clientAddressLoginAttemptCache;
        this.loginHistoryService = loginHistoryService;
        this.localeResolver = localeResolver;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
            AuthenticationException exception) throws IOException {
        log.info("Authentication failed for uri={}", httpServletRequest.getRequestURI(),
                exception);

        FailedLoginAttemptLimiter failedLoginAttemptLimiter =
                clientAddressLoginAttemptCache.get(WebRequestUtils.getClientAddress(httpServletRequest));
        Validate.notNull(failedLoginAttemptLimiter, "Failed to get failedLoginAttemptLimiter");

        // init locale for error message i18n
        Locale currentLocale = LocaleContextHolder.getLocale();
        Locale locale = localeResolver.resolveLocale(httpServletRequest);
        LocaleContextHolder.setLocale(locale);

        // init error reason with system error
        ErrorResponse errorResponse =
                Responses.error(HttpStatus.UNAUTHORIZED, Error.of(ErrorCodes.InternalServerError, new Object[] {}));
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof TestLoginTerminateException) {
            String redirectUrl = httpServletRequest.getParameter(ODC_BACK_URL_PARAM);
            if (redirectUrl != null) {
                httpServletResponse.sendRedirect(redirectUrl);
            }
            log.info("Test login success for uri#{}", httpServletRequest.getRequestURI());
            return;
        }
        Object remainTime = failedLoginAttemptLimiter.getRemainAttempt() <= 0
                ? "unlimited"
                : failedLoginAttemptLimiter.getRemainAttempt();
        if (cause instanceof OverLimitException) {
            errorResponse = Responses.error(HttpStatus.TOO_MANY_REQUESTS, Error.of((OverLimitException) cause));
        } else if (cause instanceof AttemptLoginOverLimitException) {
            errorResponse =
                    Responses.error(HttpStatus.TOO_MANY_REQUESTS, Error.of((AttemptLoginOverLimitException) cause));
        } else if (cause instanceof CredentialsExpiredException) {
            errorResponse = Responses.error(HttpStatus.UNAUTHORIZED,
                    Error.of(ErrorCodes.UserNotActive, new Object[] {remainTime}));
            // Credential expired means the user is not active, we skip failed attempt for this scenario
            failedLoginAttemptLimiter.reduceFailedAttemptCount();
        } else if (cause instanceof DisabledException) {
            errorResponse = Responses.error(HttpStatus.UNAUTHORIZED,
                    Error.of(ErrorCodes.UserNotEnabled, new Object[] {remainTime}));
        } else if (cause instanceof UsernameNotFoundException) {
            errorResponse = Responses.error(HttpStatus.NOT_FOUND,
                    Error.of(ErrorCodes.UserWrongPasswordOrNotFound, new Object[] {remainTime}));
        } else if (cause instanceof BadCredentialsException) {
            errorResponse = Responses.error(HttpStatus.UNAUTHORIZED,
                    Error.of(ErrorCodes.UserWrongPasswordOrNotFound, new Object[] {remainTime}));
        } else if (cause instanceof AuthenticationServiceException) {
            errorResponse = Responses.error(HttpStatus.INTERNAL_SERVER_ERROR,
                    Error.of(ErrorCodes.ExternalServiceError, new Object[] {cause.getLocalizedMessage()}));
            failedLoginAttemptLimiter.reduceFailedAttemptCount();
        } else if (cause instanceof PreAuthenticatedCredentialsNotFoundException) {
            errorResponse = Responses.error(HttpStatus.BAD_REQUEST,
                    Error.of(ErrorCodes.BadRequest, new Object[] {cause.getLocalizedMessage()}));
            failedLoginAttemptLimiter.reduceFailedAttemptCount();
        } else if (cause instanceof IllegalArgumentException) {
            errorResponse = Responses.error(HttpStatus.BAD_REQUEST, Error.of(ErrorCodes.RequestFormatVersionNotMatch,
                    new Object[] {cause.getLocalizedMessage()}));
            failedLoginAttemptLimiter.reduceFailedAttemptCount();
        } else if (cause instanceof CaptchaAuthenticationException) {
            errorResponse = Responses.error(HttpStatus.BAD_REQUEST,
                    Error.of(((CaptchaAuthenticationException) cause).getCode()));
            failedLoginAttemptLimiter.reduceFailedAttemptCount();
        } else {
            failedLoginAttemptLimiter.reduceFailedAttemptCount();
        }

        errorResponse.getError().addDetail(exception);

        // recover to current locale
        LocaleContextHolder.setLocale(currentLocale);

        String accountName = TraceContextHolder.getAccountName();
        if (StringUtils.isNotBlank(accountName)) {
            LoginHistory loginHistory = new LoginHistory();
            loginHistory.setSuccess(false);
            loginHistory.setAccountName(accountName);
            loginHistory.setUserId(TraceContextHolder.getUserId());
            loginHistory.setOrganizationId(TraceContextHolder.getOrganizationId());
            loginHistory.setFailedReason(convertToFailedReason(ErrorCodes.valueOf(errorResponse.getError().getCode())));
            loginHistory.setLoginTime(OffsetDateTime.now());
            loginHistory.setSuccess(false);
            loginHistoryService.record(loginHistory);
        } else {
            log.warn("accountName was blank");
        }
        WebResponseUtils.writeJsonObjectWithUnauthorizedStatus(errorResponse, httpServletRequest, httpServletResponse);
    }

    private FailedReason convertToFailedReason(ErrorCodes errorCode) {
        switch (errorCode) {
            case UserWrongPasswordOrNotFound:
                return FailedReason.USER_NOT_FOUND_OR_BAD_CREDENTIALS;
            case UserNotEnabled:
                return FailedReason.USER_NOT_ENABLED;
            case OverLimit:
                return FailedReason.TOO_MANY_ATTEMPTS;
            default:
                return FailedReason.OTHER;
        }
    }
}
