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

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.connection.CloudMetadataClient;
import com.oceanbase.odc.service.connection.model.OBDatabaseUser;
import com.oceanbase.odc.service.connection.model.OBInstance;
import com.oceanbase.odc.service.connection.model.OBTenant;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api/v2/cloud/metadata")
public class CloudMetadataController {
    @Autowired
    private CloudMetadataClient cloudMetadataClient;

    @ApiOperation(value = "listInstances", notes = "Cloud Metadata list instances")
    @RequestMapping(value = "/clusters", method = RequestMethod.GET)
    public ListResponse<OBInstance> listInstances(
            @RequestParam(required = false, name = "organizationId") Long organizationId) {
        if (Objects.nonNull(organizationId)) {
            return Responses.list(cloudMetadataClient.listInstances(organizationId));
        } else {
            return Responses.list(cloudMetadataClient.listInstances());
        }
    }

    @ApiOperation(value = "listTenants", notes = "Cloud Metadata list tenants by instanceId")
    @RequestMapping(value = "/clusters/{instanceId}/tenants", method = RequestMethod.GET)
    public ListResponse<OBTenant> listTenants(@PathVariable String instanceId) {
        return Responses.list(cloudMetadataClient.listTenants(instanceId));
    }

    @ApiOperation(value = "listDatabaseUsers", notes = "Cloud Metadata list database users by instanceId and tenantId")
    @RequestMapping(value = "/clusters/{instanceId}/tenants/{tenantId}/users", method = RequestMethod.GET)
    public ListResponse<OBDatabaseUser> listDatabaseUsers(@PathVariable String instanceId,
            @PathVariable String tenantId) {
        return Responses.list(cloudMetadataClient.listDatabaseUsers(instanceId, tenantId));
    }

}
