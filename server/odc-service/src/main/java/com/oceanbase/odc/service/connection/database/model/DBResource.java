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
package com.oceanbase.odc.service.connection.database.model;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2024/1/4 17:12
 */
@Data
public class DBResource {

    private ResourceType type;
    private DialectType dialectType;
    private Long dataSourceId;
    private String dataSourceName;
    private Long databaseId;
    private String databaseName;
    private Long tableId;
    private String tableName;

    public static DBResource from(ConnectionConfig dataSource, String databaseName, String tableName) {
        DBResource obj = new DBResource();
        obj.setDataSourceId(dataSource.getId());
        obj.setDataSourceName(dataSource.getName());
        obj.setDialectType(dataSource.getDialectType());
        obj.setDatabaseName(databaseName);
        obj.setTableName(tableName);
        if (databaseName != null) {
            obj.setType(ResourceType.ODC_DATABASE);
        }
        if (tableName != null) {
            obj.setType(ResourceType.ODC_TABLE);
        }
        return obj;
    }

}
