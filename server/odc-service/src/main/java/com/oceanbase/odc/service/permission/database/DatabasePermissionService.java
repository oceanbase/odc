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
package com.oceanbase.odc.service.permission.database;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.shared.exception.NotImplementedException;
import com.oceanbase.odc.service.permission.database.model.CreateDatabasePermissionReq;
import com.oceanbase.odc.service.permission.database.model.QueryDatabasePermissionParams;
import com.oceanbase.odc.service.permission.database.model.UserDatabasePermission;

/**
 * @author gaoda.xy
 * @date 2024/1/4 11:24
 */
@Service
@Validated
@Authenticated
public class DatabasePermissionService {

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER", "DBA", "DEVELOPER", "SECURITY_ADMINISTRATOR", "PARTICIPANT"},
            resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public Page<UserDatabasePermission> list(@NotNull Long projectId, @NotNull QueryDatabasePermissionParams params,
            Pageable pageable) {
        throw new NotImplementedException();
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER", "DBA"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public List<UserDatabasePermission> create(@NotNull Long projectId,
            @NotNull @Valid CreateDatabasePermissionReq req) {
        throw new NotImplementedException();
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER", "DBA"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public UserDatabasePermission revoke(@NotNull Long projectId, @NotNull Long permissionId) {
        throw new NotImplementedException();
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER", "DBA", "DEVELOPER", "SECURITY_ADMINISTRATOR", "PARTICIPANT"},
            resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public UserDatabasePermission relinquish(@NotNull Long projectId, @NotNull Long permissionId) {
        throw new NotImplementedException();
    }

}
