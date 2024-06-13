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
package com.oceanbase.odc.service.permission.database.model;

import java.util.List;

import com.oceanbase.odc.core.shared.constant.AuthorizationType;

import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * @author gaoda.xy
 * @date 2024/1/4 13:59
 */
@Data
@SuperBuilder
public class QueryDatabasePermissionParams {

    private Long userId;
    private Long ticketId;
    private String fuzzyDatabaseName;
    private String fuzzyDataSourceName;
    private List<DatabasePermissionType> types;
    private AuthorizationType authorizationType;
    private List<ExpirationStatusFilter> statuses;

}
