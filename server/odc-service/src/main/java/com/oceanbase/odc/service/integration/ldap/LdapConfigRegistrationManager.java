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
package com.oceanbase.odc.service.integration.ldap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.integration.IntegrationService;
import com.oceanbase.odc.service.integration.model.LdapParameter;
import com.oceanbase.odc.service.integration.model.SSOIntegrationConfig;

import lombok.NonNull;

@Component
@Profile("alipay")
@ConditionalOnProperty(value = {"odc.iam.auth.type"}, havingValue = "local")
public class LdapConfigRegistrationManager implements InitializingBean {

    @Autowired
    IntegrationService integrationService;

    private final Map<String, SSOIntegrationConfig> configRegistrations = new ConcurrentHashMap<>();

    public final Cache<String, SSOIntegrationConfig> testConfigRegistrations =
            Caffeine.newBuilder().maximumSize(100).expireAfterWrite(10, TimeUnit.MINUTES).build();

    @NonNull
    public SSOIntegrationConfig findByRegistrationId(String registrationId) {
        Verify.notBlank(registrationId, "registrationId cannot be empty");
        SSOIntegrationConfig clientRegistration = this.configRegistrations.get(registrationId);
        SSOIntegrationConfig ssoIntegrationConfig = clientRegistration != null ? clientRegistration
                : this.testConfigRegistrations.get(registrationId, key -> null);
        Preconditions.checkArgument(ssoIntegrationConfig != null && ssoIntegrationConfig.isLdap(),
                "sso type is not ldap");
        return ssoIntegrationConfig;
    }

    public void addTestConfig(SSOIntegrationConfig ssoIntegrationConfig) {
        Preconditions.checkArgument(ssoIntegrationConfig.isLdap());
        LdapParameter ssoParameter = (LdapParameter) ssoIntegrationConfig.getSsoParameter();
        ssoParameter.amendTest();
        testConfigRegistrations.put(ssoIntegrationConfig.resolveRegistrationId(), ssoIntegrationConfig);
    }

    public void addConfig(SSOIntegrationConfig ssoIntegrationConfig) {
        configRegistrations.put(ssoIntegrationConfig.resolveRegistrationId(), ssoIntegrationConfig);
    }

    public void removeConfig(String registrationId) {
        configRegistrations.remove(registrationId);
    }

    @Override
    public void afterPropertiesSet() {
        SSOIntegrationConfig sSoClientRegistration = integrationService.getSSoIntegrationConfig();
        if (sSoClientRegistration != null && sSoClientRegistration.isLdap()) {
            configRegistrations.put(sSoClientRegistration.resolveRegistrationId(), sSoClientRegistration);
        }
    }

}
