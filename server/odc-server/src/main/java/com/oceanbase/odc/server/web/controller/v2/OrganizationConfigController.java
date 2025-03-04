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
package com.oceanbase.odc.server.web.controller.v2;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.core.shared.exception.NotImplementedException;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.config.OrganizationConfigMetaService;
import com.oceanbase.odc.service.config.OrganizationConfigService;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.config.model.ConfigurationMeta;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

import io.swagger.annotations.ApiOperation;

/**
 * @author yizhuo
 * @date 2025/2/13 08:24
 * @description organization configuration controller
 * @since 1.8
 */
@RestController
@RequestMapping("/api/v2/config/organization")
public class OrganizationConfigController {
    //@Autowired
    //private OrganizationConfigMetaService organizationConfigMetaService;
    //@Autowired
    //private OrganizationConfigService organizationConfigService;
    //@Autowired
    //private AuthenticationFacade authenticationFacade;

    /**
     * Load meta organization configurations
     *
     * @return configuration meta listResponse
     */
    @ApiOperation(value = "listConfigurationMetas",
            notes = "list all organization configuration metas")
    @RequestMapping(value = "/configurationMetas", method = RequestMethod.GET)
    public ListResponse<ConfigurationMeta> listConfigurationMetas() {
        throw new NotImplementedException("not implemented");
        // return Responses.list(organizationConfigMetaService.listAllConfigMetas());
    }

    /**
     * Get organization configurations
     *
     * @param organizationId organization id
     * @return configuration listResponse
     */
    @ApiOperation(value = "listCurrentOrganizationConfigs",
            notes = "get organization configuration in present organization")
    @RequestMapping(value = "/configurations", method = RequestMethod.GET)
    public ListResponse<Configuration> listCurrentOrganizationConfigs(Long organizationId) {
        throw new NotImplementedException("not implemented");
        // return Responses.list(organizationConfigService.listOrganizationConfigurations(organizationId));
    }

    /**
     * Batch update organization configurations
     *
     * @param configurations configurations to update
     * @return configuration listResponse
     */
    @ApiOperation(value = "updateOrganizationConfigs",
            notes = "batch update organization configurations in present organization")
    @RequestMapping(value = "/configurations", method = RequestMethod.PATCH)
    public ListResponse<Configuration> updateOrganizationConfigs(@RequestBody List<Configuration> configurations) {
        throw new NotImplementedException("not implemented");
        // long organizationId = authenticationFacade.currentOrganizationId();
        // return Responses.list(organizationConfigService.batchUpdate(organizationId, configurations));
    }

    /**
     * Update organization configuration
     *
     * @param key key of configuration
     * @param configuration configuration to update
     * @return configuration success response
     */
    @ApiOperation(value = "updateOrganizationConfig",
            notes = "update organization configuration in present organization")
    @RequestMapping(value = "/configurations/{key:[A-Za-z0-9.]+}", method = RequestMethod.PUT)
    public SuccessResponse<Configuration> updateOrganizationConfig(@PathVariable("key") String key,
            @RequestBody Configuration configuration) {
        throw new NotImplementedException("not implemented");
        // long organizationId = authenticationFacade.currentOrganizationId();
        // configuration.setKey(key);
        // return Responses.single(organizationConfigService.update(organizationId, configuration));
    }

    /**
     * Get organization configuration as default status
     *
     * @return configuration listResponse
     */
    @ApiOperation(value = "listDefaultOrganizationConfigs",
            notes = "get default configurations in present organization")
    @RequestMapping(value = "/default/configurations", method = RequestMethod.GET)
    public ListResponse<Configuration> listDefaultOrganizationConfigs() {
        throw new NotImplementedException("not implemented");
        // return Responses.list(organizationConfigService.listDefaultOrganizationConfigurations());
    }

}
