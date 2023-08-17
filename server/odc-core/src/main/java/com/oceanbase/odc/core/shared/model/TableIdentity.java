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
package com.oceanbase.odc.core.shared.model;

import javax.validation.constraints.NotNull;

import com.oceanbase.odc.core.shared.PreConditions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class TableIdentity implements Comparable<TableIdentity> {

    private final String schemaName;
    private final String tableName;

    public TableIdentity(String schemaName, String tableName) {
        PreConditions.notNull(schemaName, "schemaName");
        PreConditions.notNull(tableName, "tableName");
        this.schemaName = schemaName;
        this.tableName = tableName;
    }

    public static TableIdentity of(String schemaName, String tableName) {
        return new TableIdentity(schemaName, tableName);
    }

    @Override
    public int compareTo(@NotNull TableIdentity o) {
        if (o == null) {
            throw new NullPointerException("[TableIdentity] cannot compare null object");
        }
        int c1 = schemaName.compareTo(o.schemaName);
        if (c1 > 0) {
            return 1;
        }
        if (c1 < 0) {
            return -1;
        }
        int c2 = tableName.compareTo(o.tableName);
        return Integer.compare(c2, 0);
    }

}
