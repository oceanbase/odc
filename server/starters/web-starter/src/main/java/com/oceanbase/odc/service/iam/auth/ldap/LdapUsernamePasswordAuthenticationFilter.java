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

import static com.oceanbase.odc.core.shared.constant.ErrorCodes.UserWrongPasswordOrNotFound;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.Validate;
import org.springframework.ldap.NamingException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.oceanbase.odc.common.lang.Holder;
import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.exception.AttemptLoginOverLimitException;
import com.oceanbase.odc.core.shared.exception.OverLimitException;
import com.oceanbase.odc.service.common.response.Error;
import com.oceanbase.odc.service.common.util.WebRequestUtils;
import com.oceanbase.odc.service.encryption.SensitivePropertyHandler;
import com.oceanbase.odc.service.iam.util.FailedLoginAttemptLimiter;
import com.oceanbase.odc.service.integration.ldap.LdapConfigRegistrationManager;
import com.oceanbase.odc.service.integration.model.LdapContextHolder;
import com.oceanbase.odc.service.integration.model.LdapContextHolder.LdapContext;
import com.oceanbase.odc.service.integration.model.LdapParameter;
import com.oceanbase.odc.service.integration.model.SSOIntegrationConfig;
import com.oceanbase.odc.service.integration.password.LoginFailedLimitException;

public class LdapUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final SensitivePropertyHandler sensitivePropertyHandler;
    private final LoadingCache<String, FailedLoginAttemptLimiter> clientAddressLoginAttemptCache;
    private final LdapConfigRegistrationManager ldapConfigRegistrationManager;

    public LdapUsernamePasswordAuthenticationFilter(SensitivePropertyHandler sensitivePropertyHandler,
            AuthenticationManager authenticationManager,
            LoadingCache<String, FailedLoginAttemptLimiter> clientAddressLoginAttemptCache,
            LdapConfigRegistrationManager ldapConfigRegistrationManager) {
        super();
        this.sensitivePropertyHandler = sensitivePropertyHandler;
        this.setAuthenticationManager(authenticationManager);
        this.clientAddressLoginAttemptCache = clientAddressLoginAttemptCache;
        this.ldapConfigRegistrationManager = ldapConfigRegistrationManager;
        this.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/api/v2/iam/ldap/login", "POST"));
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        LdapContext context = LdapContextHolder.getContext();
        if (StringUtils.isBlank(context.getRegistrationId())) {
            throw new AuthenticationServiceException("registrationId is empty");
        }
        TraceContextHolder.setAccountName(context.getUsername());
        SSOIntegrationConfig ssoIntegrationConfig = ldapConfigRegistrationManager.findByRegistrationId(
                context.getRegistrationId());
        LdapParameter ssoParameter = (LdapParameter) ssoIntegrationConfig.getSsoParameter();
        FailedLoginAttemptLimiter failedLoginAttemptLimiter = clientAddressLoginAttemptCache.get(
                ldapClientAddressKey(request),
                key -> new FailedLoginAttemptLimiter(ssoParameter.getLoginFailedLimit(), TimeUnit.MILLISECONDS.convert(
                        ssoParameter.getLockTimeSeconds(), TimeUnit.SECONDS)));
        Validate.notNull(failedLoginAttemptLimiter, "Failed to get failedLoginAttemptLimiter");
        final Holder<Authentication> authenticationHolder = new Holder<>();
        try {
            if (context.isTest()) {
                authenticate(request, authenticationHolder);
            } else {
                failedLoginAttemptLimiter.attemptFailedByException(() -> authenticate(request, authenticationHolder),
                        Arrays.asList(NamingException.class, BadCredentialsException.class));
            }
        } catch (NamingException | BadCredentialsException e) {
            Object remainTime = failedLoginAttemptLimiter.getRemainAttempt() < 0
                    ? "unlimited"
                    : failedLoginAttemptLimiter.getRemainAttempt();
            Error error = Error.of(UserWrongPasswordOrNotFound, new Object[] {remainTime});
            throw new LoginFailedLimitException(error);
        } catch (OverLimitException | AttemptLoginOverLimitException e) {
            throw new AuthenticationServiceException("AttemptAuthentication over limit", e);
        }
        return authenticationHolder.getValue();
    }

    private void authenticate(HttpServletRequest request, Holder<Authentication> authenticationHolder) {
        LdapContext context = LdapContextHolder.getContext();
        String decrypt = sensitivePropertyHandler.decrypt(context.getPassword());
        LdapPasswordAuthenticationToken authRequest =
                new LdapPasswordAuthenticationToken(context.getUsername(), decrypt);
        setDetails(request, authRequest);
        Authentication authenticate = this.getAuthenticationManager().authenticate(authRequest);
        authenticationHolder.setValue(authenticate);
    }

    // Isolated from local login
    public static String ldapClientAddressKey(HttpServletRequest request) {
        return "ldap-" + WebRequestUtils.getClientAddress(request);
    }
}
