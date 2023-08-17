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
package com.oceanbase.odc.service.iam.model;

import java.util.Date;

import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserPermissionEntity;

import lombok.Data;
import lombok.NonNull;

/**
 * @author gaoda.xy
 * @date 2022/12/19 10:56
 */
@Data
public class UserPermissionResp {
    private Long id;
    private Long userId;
    private String userName;
    private String userAccountName;
    private Long permissionId;
    private String resourceIdentifier;
    private String action;
    private Long creatorId;
    private Long organizationId;
    private Date createTime;
    private Date updateTime;

    public UserPermissionResp(@NonNull UserPermissionEntity userPermission, @NonNull UserEntity user,
            @NonNull PermissionEntity permission) {
        this.id = userPermission.getId();
        this.userId = userPermission.getUserId();
        this.permissionId = userPermission.getPermissionId();
        this.creatorId = userPermission.getCreatorId();
        this.organizationId = userPermission.getOrganizationId();
        this.createTime = userPermission.getCreateTime();
        this.updateTime = userPermission.getUpdateTime();
        this.userName = user.getName();
        this.userAccountName = user.getAccountName();
        this.resourceIdentifier = permission.getResourceIdentifier();
        this.action = permission.getAction();
    }
}
