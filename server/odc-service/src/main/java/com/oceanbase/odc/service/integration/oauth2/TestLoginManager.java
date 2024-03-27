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

import static com.oceanbase.odc.core.shared.constant.OdcConstants.TEST_LOGIN_ID_PARAM;
import static com.oceanbase.odc.service.integration.model.SSOIntegrationConfig.parseRegistrationName;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.metadb.integration.IntegrationEntity;
import com.oceanbase.odc.service.common.util.UrlUtils;
import com.oceanbase.odc.service.common.util.WebRequestUtils;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.auth.TestLoginTerminateException;
import com.oceanbase.odc.service.integration.IntegrationService;
import com.oceanbase.odc.service.integration.ldap.LdapConfigRegistrationManager;
import com.oceanbase.odc.service.integration.model.IntegrationConfig;
import com.oceanbase.odc.service.integration.model.IntegrationType;
import com.oceanbase.odc.service.integration.model.LdapContextHolder;
import com.oceanbase.odc.service.integration.model.LdapContextHolder.LdapContext;
import com.oceanbase.odc.service.integration.model.SSOIntegrationConfig;

import lombok.extern.slf4j.Slf4j;

@Service
@Authenticated
@Slf4j
public class TestLoginManager {

    public final Cache<String, String> testLoginInfoCache =
            Caffeine.newBuilder().maximumSize(100).expireAfterWrite(10, TimeUnit.MINUTES).build();


    private static final String REGISTRATION_ID_URI_VARIABLE_NAME = "registrationId";
    private static final AntPathRequestMatcher authorizationRequestMatcher = new AntPathRequestMatcher(
            "/login/oauth2/code" + "/{" + REGISTRATION_ID_URI_VARIABLE_NAME + "}");
    private static final AntPathRequestMatcher LDAP_REQUEST_MATCHER =
            new AntPathRequestMatcher("/api/v2/iam/ldap/login", "POST");

    @Autowired(required = false)
    private AddableClientRegistrationManager addableClientRegistrationManager;

    @Autowired(required = false)
    private LdapConfigRegistrationManager ldapConfigRegistrationManager;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @SkipAuthorize
    public void saveOauth2TestIdIfNeed(@NotBlank String loginInfo) {
        HttpServletRequest currentRequest = WebRequestUtils.getCurrentRequest();
        Verify.notNull(currentRequest, "currentRequest");
        if (!isOAuthTestLoginRequest(currentRequest)) {
            return;
        }
        String testId = currentRequest.getParameter(TEST_LOGIN_ID_PARAM);
        Verify.notNull(testId, "testId");
        testLoginInfoCache.put(testId, loginInfo);
    }

    @SkipAuthorize
    public void saveLdapTestIdIfNeed(@NotBlank String loginInfo) {
        LdapContext context = LdapContextHolder.getContext();
        if (context.isTest()) {
            testLoginInfoCache.put(context.getTestId(), loginInfo);
        }
    }

    @PreAuthenticate(actions = "create", resourceType = "ODC_INTEGRATION", isForAll = true)
    public SSOTestInfo getSSOTestInfo(IntegrationConfig config, String type) {
        SSOIntegrationConfig ssoConfig = SSOIntegrationConfig.of(config, authenticationFacade.currentOrganizationId());
        Verify.verify(ssoConfig.isOauth2OrOidc() || ssoConfig.isLdap(), "not support sso type");
        if (config.getEncryption().getSecret() == null) {
            Optional<IntegrationEntity> integration = integrationService.findByTypeAndOrganizationIdAndName(
                    IntegrationType.SSO, authenticationFacade.currentOrganizationId(), config.getName());
            Verify.verify(integration.isPresent(), "lack of secret");
            IntegrationEntity integrationEntity = integration.get();
            ssoConfig.fillDecryptSecret(integrationService.decodeSecret(integrationEntity.getSecret(),
                    integrationEntity.getSalt(), integrationEntity.getOrganizationId()));
        }
        String testId = UUID.randomUUID().toString();
        String redirectUrl = null;
        String testRegistrationId = null;
        if (ssoConfig.isOauth2OrOidc()) {
            if (addableClientRegistrationManager == null) {
                throw new UnsupportedOperationException("add test sso is not support");
            }
            addableClientRegistrationManager.addTestToRegister(ssoConfig, type);
            redirectUrl = UrlUtils.appendQueryParameter(ssoConfig.resolveLoginRedirectUrl(),
                    TEST_LOGIN_ID_PARAM, testId);
        } else if (ssoConfig.isLdap()) {
            if (ldapConfigRegistrationManager == null) {
                throw new UnsupportedOperationException("add test sso is not support");
            }
            ldapConfigRegistrationManager.addTestConfig(ssoConfig);
            testRegistrationId = ssoConfig.resolveRegistrationId();
        }
        return new SSOTestInfo(redirectUrl, testId, testRegistrationId);
    }

    @Nullable
    @PreAuthenticate(actions = "create", resourceType = "ODC_INTEGRATION", isForAll = true)
    public String getTestUserInfo(String testId) {
        return testLoginInfoCache.get(testId, key -> null);
    }

    @SkipAuthorize
    public void abortIfOAuthTestLoginTest() {
        HttpServletRequest currentRequest = WebRequestUtils.getCurrentRequest();
        if (currentRequest == null) {
            return;
        }
        if (isOAuthTestLoginRequest(currentRequest)) {
            throw new TestLoginTerminateException();
        }
    }

    @SkipAuthorize
    public void abortIfOAuthTestLoginInfo() {
        HttpServletRequest currentRequest = WebRequestUtils.getCurrentRequest();
        if (currentRequest == null) {
            return;
        }
        if (isOAuthTestLoginRequest(currentRequest) && "info".equals(currentRequest.getParameter("type"))) {
            throw new TestLoginTerminateException();
        }
    }

    @SkipAuthorize
    public void abortIfLdapTestLogin() {
        LdapContext context = LdapContextHolder.getContext();
        if (context.isTest()) {
            throw new TestLoginTerminateException();
        }
    }

    @SkipAuthorize
    public static boolean isOAuthTestLoginRequest(HttpServletRequest request) {
        String registrationId = resolveRegistrationId(request);
        if (registrationId == null) {
            return false;
        }
        return "test".equals(parseRegistrationName(registrationId));
    }

    @SkipAuthorize
    public LdapContext loadLdapContext(HttpServletRequest request) {
        boolean isLdapLogin = LDAP_REQUEST_MATCHER.matches(request);
        if (!isLdapLogin) {
            return null;
        }
        String registrationId = request.getParameter(REGISTRATION_ID_URI_VARIABLE_NAME);
        if (StringUtils.isBlank(registrationId)) {
            SSOIntegrationConfig sSoIntegrationConfig = integrationService.getSSoIntegrationConfig();
            registrationId = sSoIntegrationConfig.resolveRegistrationId();
        }
        String testId = request.getParameter("testId");
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        LdapContext ldapContext = new LdapContext(registrationId, testId, username, password);
        LdapContextHolder.setParameter(ldapContext);
        return ldapContext;
    }


    private static String resolveRegistrationId(HttpServletRequest request) {
        if (authorizationRequestMatcher.matches(request)) {
            return authorizationRequestMatcher.matcher(request).getVariables()
                    .get(REGISTRATION_ID_URI_VARIABLE_NAME);
        }
        return null;
    }

}
