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
package com.oceanbase.odc.service.integration.saml;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.integration.IntegrationService;
import com.oceanbase.odc.service.integration.model.SSOIntegrationConfig;

@Component
public class AddableRelyingPartyRegistrationRepository implements RelyingPartyRegistrationRepository {

    public final Cache<String, SSOIntegrationConfig> testConfigRegistrations =
            Caffeine.newBuilder().maximumSize(100).expireAfterWrite(10, TimeUnit.MINUTES).build();

    @Autowired
    IntegrationService integrationService;

    @Override
    public RelyingPartyRegistration findByRegistrationId(String registrationId) {
        SSOIntegrationConfig configByRegistrationId = findConfigByRegistrationId(registrationId);
        return configByRegistrationId == null ? null
                : SamlRegistrationConfigHelper.asRegistration(configByRegistrationId);
    }

    public SSOIntegrationConfig findConfigByRegistrationId(String registrationId) {
        Verify.notBlank(registrationId, "registrationId cannot be empty");
        SSOIntegrationConfig sSoClientRegistration = integrationService.getSSoIntegrationConfig();
        if (sSoClientRegistration == null) {
            return testConfigRegistrations.get(registrationId, key -> null);
        }
        Verify.notNull(sSoClientRegistration, "Saml sSoClientRegistration");
        SamlParameter parameter = (SamlParameter) sSoClientRegistration.getSsoParameter();
        if (Objects.equals(registrationId, parameter.getRegistrationId())) {
            return sSoClientRegistration;
        }
        return testConfigRegistrations.get(registrationId, key -> null);
    }

    public void addTestConfig(SSOIntegrationConfig ssoConfig) {
        SamlParameter parameter = (SamlParameter) ssoConfig.getSsoParameter();
        parameter.amendTest();
        testConfigRegistrations.put(ssoConfig.resolveRegistrationId(), ssoConfig);
    }
}
