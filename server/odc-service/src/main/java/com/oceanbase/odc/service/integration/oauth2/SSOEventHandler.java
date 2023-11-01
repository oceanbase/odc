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
import com.oceanbase.odc.service.integration.IntegrationConfigurationValidator;
import com.oceanbase.odc.service.integration.IntegrationEvent;
import com.oceanbase.odc.service.integration.IntegrationEventHandler;
import com.oceanbase.odc.service.integration.IntegrationService;
import com.oceanbase.odc.service.integration.model.IntegrationConfig;
import com.oceanbase.odc.service.integration.model.IntegrationType;
import com.oceanbase.odc.service.integration.model.SSOIntegrationConfig;

@Component
public class SSOEventHandler implements IntegrationEventHandler {

    @Autowired(required = false)
    private AddableClientRegistrationManager addableClientRegistrationManager;

    @Autowired
    private IntegrationConfigurationValidator configurationValidator;

    @Autowired
    private IntegrationService integrationService;

    @Override
    public boolean support(IntegrationEvent integrationEvent) {
        return IntegrationType.SSO.equals(integrationEvent.getCurrentIntegrationType());
    }

    @Override
    public void preCreate(IntegrationEvent integrationEvent) {
        Verify.notNull(addableClientRegistrationManager, "addableClientRegistrationManager");
        if (integrationEvent.getCurrentConfig().getEnabled()) {
            IntegrationConfig currentConfig = integrationEvent.getCurrentConfig();
            SSOIntegrationConfig decryptConfiguration =
                    getDecryptConfiguration(currentConfig, currentConfig.getEncryption().getSecret());
            addableClientRegistrationManager.addToRegister(decryptConfiguration);
        }
    }

    @Override
    public void preDelete(IntegrationEvent integrationEvent) {
        Verify.notNull(addableClientRegistrationManager, "addableClientRegistrationManager");
        SSOIntegrationConfig decryptConfiguration = getDecryptConfiguration(integrationEvent.getCurrentConfig(), null);
        addableClientRegistrationManager.removeRegister(decryptConfiguration.resolveRegistrationId());
    }

    @Override
    public void preUpdate(IntegrationEvent integrationEvent) {
        Verify.notNull(addableClientRegistrationManager, "addableClientRegistrationManager");
        IntegrationConfig preConfig = integrationEvent.getPreConfig();
        IntegrationConfig currentConfig = integrationEvent.getCurrentConfig();
        configurationValidator.checkNotEnabledInDbBeforeSave(currentConfig.getEnabled(),
                currentConfig.getOrganizationId(), currentConfig.getId());
        // current config will not have secret when it is updated, secret can't change, so use preConfig
        String decryptSecret = integrationService.decodeSecret(preConfig.getEncryption().getSecret(),
                integrationEvent.getSalt(), preConfig.getOrganizationId());
        SSOIntegrationConfig ssoIntegrationConfig = getDecryptConfiguration(currentConfig, decryptSecret);
        if (Boolean.TRUE.equals(integrationEvent.getCurrentConfig().getEnabled())) {
            addableClientRegistrationManager.addToRegister(ssoIntegrationConfig);
        } else {
            addableClientRegistrationManager.removeRegister(ssoIntegrationConfig.resolveRegistrationId());
        }
    }

    private SSOIntegrationConfig getDecryptConfiguration(IntegrationConfig config, String decryptConfiguration) {
        Verify.verify(config.getType() == SSO, "wrong integration type");
        SSOIntegrationConfig ssoIntegrationConfig =
                JsonUtils.fromJson(config.getConfiguration(), SSOIntegrationConfig.class);
        ssoIntegrationConfig.fillDecryptSecret(decryptConfiguration);
        return ssoIntegrationConfig;
    }
}
