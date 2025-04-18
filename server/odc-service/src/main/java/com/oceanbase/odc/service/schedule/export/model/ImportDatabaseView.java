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
package com.oceanbase.odc.service.schedule.export.model;

import javax.annotation.Nullable;

import com.oceanbase.odc.core.shared.constant.ConnectType;

import lombok.Data;

@Data
public class ImportDatabaseView {
    private String cloudProvider;
    private String region;
    private ConnectType type;
    private String instanceId;
    private String instanceNickName;
    private String tenantId;
    private String tenantNickName;
    private String host;
    private Integer port;
    private String username;
    // export datasource name
    private String name;
    // matched Datasource name, null means not matched
    @Nullable
    private String matchedDatasourceName;

    private String databaseName;
}
