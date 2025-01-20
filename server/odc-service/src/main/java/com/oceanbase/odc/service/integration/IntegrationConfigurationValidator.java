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

import static com.oceanbase.odc.service.integration.model.IntegrationType.SSO;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.metadb.integration.IntegrationEntity;
import com.oceanbase.odc.metadb.integration.IntegrationRepository;
import com.oceanbase.odc.service.integration.model.ApprovalProperties;
import com.oceanbase.odc.service.integration.model.IntegrationConfig;
import com.oceanbase.odc.service.integration.model.SSOIntegrationConfig;
import com.oceanbase.odc.service.integration.model.SqlInterceptorProperties;

/**
 * @author gaoda.xy
 * @date 2023/3/29 20:07
 */
@Component
@Validated
public class IntegrationConfigurationValidator {

    @Autowired
    private IntegrationRepository integrationRepository;

    public void check(@NotNull @Valid ApprovalProperties properties) {}

    public void check(@NotNull @Valid SqlInterceptorProperties properties) {}

    public void checkAndFillConfig(@NotNull @Valid IntegrationConfig config, Long organizationId, Boolean enabled,
            @Nullable Long integrationId) {
        SSOIntegrationConfig ssoIntegrationConfig = SSOIntegrationConfig.of(config, organizationId);
        config.setConfiguration(JsonUtils.toJson(ssoIntegrationConfig));
        checkNotEnabledInDbBeforeSave(enabled, organizationId, integrationId);
    }

    public void checkNotEnabledInDbBeforeSave(Boolean enabled, Long organizationId, @Nullable Long integrationId) {
        if (Boolean.TRUE.equals(enabled)) {
            List<IntegrationEntity> dbSSO = integrationRepository.findByTypeAndOrganizationId(SSO,
                    organizationId);
            IntegrationEntity otherEnabledSSO = dbSSO.stream().filter(IntegrationEntity::getEnabled).filter(
                    t -> !Objects.equals(t.getId(), integrationId))
                    .findFirst().orElse(null);
            Verify.verify(otherEnabledSSO == null,
                    "There is another integration that is being enabled, please disabled it!");
        }
    }

}
