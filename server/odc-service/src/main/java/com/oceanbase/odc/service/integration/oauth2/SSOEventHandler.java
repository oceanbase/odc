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

import static com.oceanbase.odc.service.integration.model.IntegrationType.SSO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.integration.IntegrationConfigurationProcessor;
import com.oceanbase.odc.service.integration.IntegrationEvent;
import com.oceanbase.odc.service.integration.IntegrationEventHandler;
import com.oceanbase.odc.service.integration.IntegrationService;
import com.oceanbase.odc.service.integration.ldap.LdapConfigRegistrationManager;
import com.oceanbase.odc.service.integration.model.IntegrationConfig;
import com.oceanbase.odc.service.integration.model.IntegrationType;
import com.oceanbase.odc.service.integration.model.SSOIntegrationConfig;

@Component
public class SSOEventHandler implements IntegrationEventHandler {

    @Autowired(required = false)
    private AddableClientRegistrationManager addableClientRegistrationManager;

    @Autowired(required = false)
    private LdapConfigRegistrationManager ldapConfigRegistrationManager;

    @Autowired
    private IntegrationConfigurationProcessor integrationConfigurationProcessor;

    @Autowired
    private IntegrationService integrationService;

    @Override
    public boolean support(IntegrationEvent integrationEvent) {
        return IntegrationType.SSO.equals(integrationEvent.getCurrentIntegrationType());
    }

    @Override
    public void preCreate(IntegrationEvent integrationEvent) {
        if (Boolean.TRUE.equals(integrationEvent.getCurrentConfig().getEnabled())) {
            IntegrationConfig currentConfig = integrationEvent.getCurrentConfig();
            SSOIntegrationConfig decryptConfiguration =
                    getDecryptConfiguration(currentConfig, currentConfig.getEncryption().getSecret());
            addConfig(decryptConfiguration);
        }
    }

    @Override
    public void preDelete(IntegrationEvent integrationEvent) {
        SSOIntegrationConfig decryptConfiguration = getDecryptConfiguration(integrationEvent.getCurrentConfig(), null);
        removeConfig(decryptConfiguration);
    }

    @Override
    public void preUpdate(IntegrationEvent integrationEvent) {
        IntegrationConfig preConfig = integrationEvent.getPreConfig();
        IntegrationConfig currentConfig = integrationEvent.getCurrentConfig();
        integrationConfigurationProcessor.checkNotEnabledInDbBeforeSave(currentConfig.getEnabled(),
                currentConfig.getOrganizationId(), currentConfig.getId());
        // current config will not have secret when it is updated, secret can't change, so use preConfig
        String decryptSecret = integrationService.decodeSecret(preConfig.getEncryption().getSecret(),
                integrationEvent.getSalt(), preConfig.getOrganizationId());
        SSOIntegrationConfig ssoIntegrationConfig = getDecryptConfiguration(currentConfig, decryptSecret);
        if (Boolean.TRUE.equals(integrationEvent.getCurrentConfig().getEnabled())) {
            addConfig(ssoIntegrationConfig);
        } else {
            removeConfig(ssoIntegrationConfig);
        }
    }

    private void addConfig(SSOIntegrationConfig ssoIntegrationConfig) {
        Verify.notNull(addableClientRegistrationManager, "addableClientRegistrationManager");
        Verify.notNull(ldapConfigRegistrationManager, "ldapConfigRegistrationManager");
        if (ssoIntegrationConfig.isLdap()) {
            ldapConfigRegistrationManager.addConfig(ssoIntegrationConfig);
        } else if (ssoIntegrationConfig.isOauth2OrOidc()) {
            addableClientRegistrationManager.addToRegister(ssoIntegrationConfig);
        }
    }

    private void removeConfig(SSOIntegrationConfig ssoIntegrationConfig) {
        Verify.notNull(addableClientRegistrationManager, "addableClientRegistrationManager");
        Verify.notNull(ldapConfigRegistrationManager, "ldapConfigRegistrationManager");
        if (ssoIntegrationConfig.isOauth2OrOidc()) {
            addableClientRegistrationManager.removeRegister(ssoIntegrationConfig.resolveRegistrationId());
        } else if (ssoIntegrationConfig.isLdap()) {
            ldapConfigRegistrationManager.removeConfig(ssoIntegrationConfig.resolveRegistrationId());
        }
    }

    private SSOIntegrationConfig getDecryptConfiguration(IntegrationConfig config, String decryptConfiguration) {
        Verify.verify(config.getType() == SSO, "wrong integration type");
        SSOIntegrationConfig ssoIntegrationConfig =
                JsonUtils.fromJson(config.getConfiguration(), SSOIntegrationConfig.class);
        if (!ssoIntegrationConfig.isSaml()) {
            ssoIntegrationConfig.fillDecryptSecret(decryptConfiguration);
        }
        return ssoIntegrationConfig;
    }
}
