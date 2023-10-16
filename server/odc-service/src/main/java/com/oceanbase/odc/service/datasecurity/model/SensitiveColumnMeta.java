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

package com.oceanbase.odc.service.datasecurity.model;

import java.util.Objects;

import com.oceanbase.odc.metadb.datasecurity.SensitiveColumnEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author gaoda.xy
 * @date 2023/9/14 10:33
 */
@Data
@EqualsAndHashCode
@AllArgsConstructor
public class SensitiveColumnMeta {
    private Long databaseId;
    private String tableName;
    private String columnName;

    public SensitiveColumnMeta(SensitiveColumnEntity entity) {
        this.databaseId = entity.getDatabaseId();
        this.tableName = entity.getTableName();
        this.columnName = entity.getColumnName();
    }

    @Override
    public int hashCode() {
        return Objects.hash(databaseId, tableName.toLowerCase(), columnName.toLowerCase());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SensitiveColumnMeta) {
            SensitiveColumnMeta other = (SensitiveColumnMeta) obj;
            return Objects.equals(databaseId, other.databaseId)
                    && Objects.equals(tableName.toLowerCase(), other.tableName.toLowerCase())
                    && Objects.equals(columnName.toLowerCase(), other.columnName.toLowerCase());
        }
        return false;
    }

}
