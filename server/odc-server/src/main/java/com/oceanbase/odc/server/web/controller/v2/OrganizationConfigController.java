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

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.core.shared.exception.NotImplementedException;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.config.model.ConfigurationMeta;

import io.swagger.annotations.ApiOperation;

/**
 * @author yizhuo
 * @date 2025/2/13 08:24
 * @description organization configuration controller
 * @since 4.3.4
 */
@RestController
@RequestMapping("/api/v2/config/organization")
public class OrganizationConfigController {

    /**
     * Get organization configurations
     *
     * @return configuration listResponse
     */
    @ApiOperation(value = "listCurrentOrganizationConfigs",
            notes = "get organization configuration in present organization")
    @RequestMapping(value = "/configurations", method = RequestMethod.GET)
    public ListResponse<Configuration> listCurrentOrganizationConfigs() {
        throw new NotImplementedException("not implemented");
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
    }

}
