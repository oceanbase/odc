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
package com.oceanbase.odc.service.config;

import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SkipAuthorize("odc internal usage")
public class OrganizationConfigUtils {

    @Autowired
    private OrganizationConfigService organizationConfigService;
    @Autowired
    private AuthenticationFacade authenticationFacade;

    public String getOrganizationConfig(String key) {
        long currentOrgId = authenticationFacade.currentOrganizationId();
        Map<String, Configuration> orgIdToConfigurations =
                organizationConfigService.getOrgConfigurationsFromCache(currentOrgId);
        Configuration configuration = orgIdToConfigurations.get(key);
        if (Objects.isNull(configuration)) {
            throw new IllegalStateException("organization configuration not found: " + key);
        }
        return configuration.getValue();
    }

    public void checkQueryLimitValidity(DatabaseChangeParameters parameters) {
        if (parameters.getQueryLimit() == null) {
            parameters.setQueryLimit(getDefaultQueryLimit());
        }
        Verify.notGreaterThan(parameters.getQueryLimit(), getDefaultMaxQueryLimit(),
            "query limit value");
    }

    public Integer getDefaultMaxQueryLimit() {
        return Integer.parseInt(getOrganizationConfig(OrganizationConfigKeys.DEFAULT_MAX_QUERY_LIMIT));
    }

    public Integer getDefaultQueryLimit() {
        return Integer.parseInt(getOrganizationConfig(OrganizationConfigKeys.DEFAULT_QUERY_LIMIT));
    }

    public boolean getDefaultRollbackPlanEnabled() {
        return getOrganizationConfig(OrganizationConfigKeys.DEFAULT_ROLLBACK_PLAN_ENABLED)
                .equalsIgnoreCase("false");
    }

    public boolean getDefaultImportTaskStructureReplacementEnabled() {
        return getOrganizationConfig(OrganizationConfigKeys.DEFAULT_IMPORT_TASK_STRUCTURE_REPLACEMENT_ENABLED)
                .equalsIgnoreCase("true");
    }

    public String getDefaultTaskDescription() {
        return getOrganizationConfig(OrganizationConfigKeys.DEFAULT_TASK_DESCRIPTION_PROMPT);
    }
}
