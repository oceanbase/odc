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

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.core.shared.constant.AuthorizationType;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.PaginatedResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.permission.database.DatabasePermissionService;
import com.oceanbase.odc.service.permission.database.model.CreateDatabasePermissionReq;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.permission.database.model.QueryDatabasePermissionParams;
import com.oceanbase.odc.service.permission.database.model.UserDatabasePermission;

import io.swagger.annotations.ApiOperation;

/**
 * @author gaoda.xy
 * @date 2024/1/4 10:15
 */
@RestController
@RequestMapping("/api/v2/collaboration/projects/{projectId:[\\d]+}/databasePermissions")
public class DatabasePermissionController {

    @Autowired
    private DatabasePermissionService service;

    @ApiOperation(value = "listDatabasePermissions", notes = "List database permissions")
    @RequestMapping(value = "", method = RequestMethod.GET)
    public PaginatedResponse<UserDatabasePermission> list(@PathVariable Long projectId,
            @RequestParam(name = "user") Long userId,
            @RequestParam(name = "databaseName", required = false) String fuzzyDatabaseName,
            @RequestParam(name = "dataSourceName", required = false) String fuzzyDataSourceName,
            @RequestParam(name = "environmentId", required = false) List<Long> environmentIds,
            @RequestParam(name = "type", required = false) List<DatabasePermissionType> types,
            @RequestParam(name = "authorizationType", required = false) List<AuthorizationType> authorizationTypes,
            @RequestParam(name = "expired", required = false) List<Boolean> expiredList,
            @RequestParam(name = "expiring", required = false) Boolean expiring,
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable) {
        Boolean expired = CollectionUtils.size(expiredList) == 1 ? expiredList.get(0) : null;
        QueryDatabasePermissionParams params = QueryDatabasePermissionParams.builder()
                .userId(userId)
                .fuzzyDatabaseName(fuzzyDatabaseName)
                .fuzzyDataSourceName(fuzzyDataSourceName)
                .environmentIds(environmentIds)
                .types(types)
                .authorizationTypes(authorizationTypes)
                .expired(expired)
                .expiring(expiring)
                .build();
        return Responses.paginated(service.list(projectId, params, pageable));
    }

    @ApiOperation(value = "createDatabasePermissions", notes = "Create database permissions")
    @RequestMapping(value = "", method = RequestMethod.POST)
    public ListResponse<UserDatabasePermission> create(@PathVariable Long projectId,
            @RequestBody CreateDatabasePermissionReq req) {
        return Responses.list(service.create(projectId, req));
    }

    @ApiOperation(value = "reclaimDatabasePermission", notes = "Reclaim database permission")
    @RequestMapping(value = "/{id:[\\d]+}/revoke", method = RequestMethod.POST)
    public SuccessResponse<UserDatabasePermission> revoke(@PathVariable Long projectId,
            @PathVariable(name = "id") Long id) {
        return Responses.success(service.revoke(projectId, id));
    }

    @ApiOperation(value = "releaseDatabasePermission", notes = "Release database permission")
    @RequestMapping(value = "/{id:[\\d]+}/relinquish", method = RequestMethod.POST)
    public SuccessResponse<UserDatabasePermission> relinquish(@PathVariable Long projectId,
            @PathVariable(name = "id") Long id) {
        return Responses.success(service.relinquish(projectId, id));
    }

}
