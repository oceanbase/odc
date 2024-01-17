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
package com.oceanbase.odc.service.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.integration.model.ApprovalProperties;
import com.oceanbase.odc.service.integration.model.IntegrationConfig;
import com.oceanbase.odc.service.integration.model.IntegrationType;
import com.oceanbase.odc.service.integration.model.SqlInterceptorProperties;

@Component
public class IntegrationConfigurationValidatorDelegate {

    @Autowired
    private IntegrationConfigurationValidator configurationValidator;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    public void preProcessConfig(IntegrationConfig config) {
        if (config.getType() == IntegrationType.APPROVAL) {
            configurationValidator.check(ApprovalProperties.from(config));
        } else if (config.getType() == IntegrationType.SQL_INTERCEPTOR) {
            configurationValidator.check(SqlInterceptorProperties.from(config));
        } else if (config.getType() == IntegrationType.SSO) {
            configurationValidator.checkAndFillConfig(config, authenticationFacade.currentOrganizationId(),
                    config.getEnabled(), null);
        }
    }
}
