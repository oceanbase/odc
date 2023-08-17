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
package com.oceanbase.odc.service.session.model;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.model.TableIdentity;

import lombok.Data;

@Data
public class ColumnIdentity implements Comparable<ColumnIdentity> {

    private final String schemaName;
    private final String tableName;
    private final String columnName;

    public ColumnIdentity(String schemaName, String tableName, String columnName) {
        PreConditions.notNull(schemaName, "schemaName");
        PreConditions.notNull(tableName, "tableName");
        PreConditions.notNull(columnName, "columnName");
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.columnName = columnName;
    }

    public static ColumnIdentity of(String schemaName, String tableName, String columnName) {
        return new ColumnIdentity(schemaName, tableName, columnName);
    }

    public static ColumnIdentity of(TableIdentity tableIdentity, String columnName) {
        PreConditions.notNull(tableIdentity, "tableIdentity");
        return new ColumnIdentity(tableIdentity.getSchemaName(), tableIdentity.getTableName(), columnName);
    }

    @Override
    public int compareTo(ColumnIdentity o) {
        if (o == null) {
            throw new NullPointerException("[ColumnIdentity] cannot compare null object");
        }
        int c1 = schemaName.compareTo(o.schemaName);
        if (c1 > 0) {
            return 1;
        }
        if (c1 < 0) {
            return -1;
        }
        int c2 = tableName.compareTo(o.tableName);
        if (c2 > 0) {
            return 1;
        }
        if (c2 < 0) {
            return -1;
        }
        int c3 = columnName.compareTo(o.columnName);
        return Integer.compare(c3, 0);
    }
}
