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

import java.util.List;

import com.oceanbase.odc.core.shared.constant.AuthorizationType;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.permission.database.model.ExpirationStatusFilter;

import lombok.Builder;
import lombok.Data;

/**
 * ClassName: QueryTablePermissionParams Package: com.oceanbase.odc.service.permission.table.model
 * Description:
 *
 * @Author: fenghao
 * @Create 2024/3/11 20:46
 * @Version 1.0
 */
@Data
@Builder
public class QueryTablePermissionParams {
    private Long userId;
    private Long ticketId;
    private String fuzzyDatabaseName;
    private String fuzzyDataSourceName;
    private List<DatabasePermissionType> types;
    private AuthorizationType authorizationType;
    private List<ExpirationStatusFilter> statuses;

}
