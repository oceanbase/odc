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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.oceanbase.odc.service.integration.IntegrationService;
import com.oceanbase.odc.service.integration.model.SSOIntegrationConfig;

@Component
@Profile("alipay")
@ConditionalOnProperty(value = {"odc.iam.auth.type"}, havingValue = "local")
public class AddableClientRegistrationManager implements ClientRegistrationRepository, InitializingBean {
    private final Map<String, SSOIntegrationConfig> configRegistrations = new ConcurrentHashMap<>();
    private final Map<String, ClientRegistration> clientRegistrations = new ConcurrentHashMap<>();

    public final Cache<String, SSOIntegrationConfig> testConfigRegistrations =
            Caffeine.newBuilder().maximumSize(100).expireAfterWrite(10, TimeUnit.MINUTES).build();
    public final Cache<String, ClientRegistration> testClientRegistrations =
            Caffeine.newBuilder().maximumSize(100).expireAfterWrite(10, TimeUnit.MINUTES).build();

    @Autowired
    private IntegrationService integrationService;

    @Override
    public void afterPropertiesSet() {
        SSOIntegrationConfig sSoClientRegistration = integrationService.getSSoIntegrationConfig();
        if (sSoClientRegistration != null) {
            configRegistrations.put(sSoClientRegistration.resolveRegistrationId(), sSoClientRegistration);
            clientRegistrations.put(sSoClientRegistration.resolveRegistrationId(),
                    sSoClientRegistration.toClientRegistration());
        }
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        Assert.hasText(registrationId, "registrationId cannot be empty");
        ClientRegistration clientRegistration = this.clientRegistrations.get(registrationId);
        return clientRegistration != null ? clientRegistration
                : this.testClientRegistrations.get(registrationId, (key) -> null);
    }

    public SSOIntegrationConfig findConfigByRegistrationId(String registrationId) {
        Assert.hasText(registrationId, "registrationId cannot be empty");
        SSOIntegrationConfig ssoIntegrationConfig = configRegistrations.get(registrationId);
        return ssoIntegrationConfig != null ? ssoIntegrationConfig
                : testConfigRegistrations.get(registrationId, (key) -> null);
    }

    public void addToRegister(SSOIntegrationConfig config) {
        ClientRegistration clientRegistration = config.toClientRegistration();
        clientRegistrations.put(clientRegistration.getRegistrationId(), clientRegistration);
        configRegistrations.put(clientRegistration.getRegistrationId(), config);
    }

    public void addTestToRegister(SSOIntegrationConfig config, String type) {
        ClientRegistration clientRegistration = config.toTestClientRegistration(type);
        testConfigRegistrations.put(clientRegistration.getRegistrationId(), config);
        testClientRegistrations.put(clientRegistration.getRegistrationId(), clientRegistration);
    }

    public void removeRegister(String registrationId) {
        clientRegistrations.remove(registrationId);
        configRegistrations.remove(registrationId);
    }

}
