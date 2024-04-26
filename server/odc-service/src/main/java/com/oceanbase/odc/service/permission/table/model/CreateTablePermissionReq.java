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
package com.oceanbase.odc.service.permission.table.model;

import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.oceanbase.odc.core.shared.constant.AuthorizationType;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;

import lombok.Data;

/**
 * ClassName: CreateTablePermissionReq.java Package:
 * com.oceanbase.odc.service.permission.table.model Description:
 *
 * @Author: fenghao
 * @Create 2024/3/11 20:26
 * @Version 1.0
 */
@Data
public class CreateTablePermissionReq {
    @NotEmpty
    private List<TablePermission> tables;

    private Long creatorId;

    private Long ticketId;

    @NotEmpty
    private List<DatabasePermissionType> types;

    private Date expireTime;

    private Long organizationId;

    @NotNull
    private Long userId;

    AuthorizationType authorizationType = AuthorizationType.USER_AUTHORIZATION;

    @Data
    public static class TablePermission {
        @NotNull(message = "operate cannot be null")
        private Long databaseId;
        @NotNull(message = "operate cannot be null")
        private List<String> tableNames;
    }
}
