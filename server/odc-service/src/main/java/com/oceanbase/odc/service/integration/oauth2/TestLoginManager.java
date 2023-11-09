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
import static com.oceanbase.odc.service.integration.oauth2.TestLoginContext.isTestLoginRequest;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
import com.oceanbase.odc.service.integration.model.IntegrationConfig;
import com.oceanbase.odc.service.integration.model.IntegrationType;
import com.oceanbase.odc.service.integration.model.SSOIntegrationConfig;

import lombok.extern.slf4j.Slf4j;

@Service
@Authenticated
@Slf4j
public class TestLoginManager {

    public final Cache<String, String> testLoginInfoCache =
            Caffeine.newBuilder().maximumSize(100).expireAfterWrite(10, TimeUnit.MINUTES).build();

    @Autowired(required = false)
    private AddableClientRegistrationManager addableClientRegistrationManager;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @SkipAuthorize
    public void saveOauth2TestIdIfNeed(@NotBlank String loginInfo) {
        HttpServletRequest currentRequest = WebRequestUtils.getCurrentRequest();
        Verify.notNull(currentRequest, "currentRequest");
        if (!isTestLoginRequest(currentRequest)) {
            return;
        }
        String testId = currentRequest.getParameter(TEST_LOGIN_ID_PARAM);
        Verify.notNull(testId, "testId");
        testLoginInfoCache.put(testId, loginInfo);
    }

    @PreAuthenticate(actions = "create", resourceType = "ODC_INTEGRATION", isForAll = true)
    public SSOTestInfo getTestLoginUrl(IntegrationConfig config, String type) {
        SSOIntegrationConfig ssoConfig = SSOIntegrationConfig.of(config, authenticationFacade.currentOrganizationId());
        if (config.getEncryption().getSecret() == null) {
            Optional<IntegrationEntity> integration = integrationService.findByTypeAndOrganizationIdAndName(
                    IntegrationType.SSO, authenticationFacade.currentOrganizationId(), config.getName());
            Verify.verify(integration.isPresent(), "lack of secret");
            IntegrationEntity integrationEntity = integration.get();
            ssoConfig.fillDecryptSecret(integrationService.decodeSecret(integrationEntity.getSecret(),
                    integrationEntity.getSalt(), integrationEntity.getOrganizationId()));
        }
        if (addableClientRegistrationManager == null) {
            throw new UnsupportedOperationException("add test sso is not support");
        }
        addableClientRegistrationManager.addTestToRegister(ssoConfig, type);
        String testId = UUID.randomUUID().toString();
        String redirectUrl = UrlUtils.appendQueryParameter(ssoConfig.resolveLoginRedirectUrl(),
                TEST_LOGIN_ID_PARAM, testId);
        return new SSOTestInfo(redirectUrl, testId);
    }

    @Nullable
    @PreAuthenticate(actions = "create", resourceType = "ODC_INTEGRATION", isForAll = true)
    public String getTestUserInfo(String testId) {
        return testLoginInfoCache.get(testId, key -> null);
    }

    @SkipAuthorize
    public void abortIfTestLoginTest() {
        HttpServletRequest currentRequest = WebRequestUtils.getCurrentRequest();
        if (currentRequest == null) {
            return;
        }
        if (isTestLoginRequest(currentRequest)) {
            throw new TestLoginTerminateException();
        }
    }

    @SkipAuthorize
    public void abortIfTestLoginInfo() {
        HttpServletRequest currentRequest = WebRequestUtils.getCurrentRequest();
        if (currentRequest == null) {
            return;
        }
        if (isTestLoginRequest(currentRequest) && "info".equals(currentRequest.getParameter("type"))) {
            throw new TestLoginTerminateException();
        }
    }

}
