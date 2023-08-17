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
package com.oceanbase.odc.core.sql.execute.cache.model;

import com.oceanbase.odc.core.sql.execute.cache.table.VirtualElement;

import lombok.NonNull;

public class CommonVirtualElement implements VirtualElement {

    private final Integer columnId;
    private final Long rowId;
    private final String dataType;
    private final String tableId;
    private final String columnName;
    private final Object content;

    public CommonVirtualElement(@NonNull String tableId,
            @NonNull Long rowId,
            @NonNull Integer columnId,
            @NonNull String dataType,
            @NonNull String columnName,
            @NonNull Object content) {
        this.tableId = tableId;
        this.rowId = rowId;
        this.columnId = columnId;
        this.dataType = dataType;
        this.columnName = columnName;
        this.content = content;
    }

    @Override
    public String tableId() {
        return this.tableId;
    }

    @Override
    public Integer columnId() {
        return this.columnId;
    }

    @Override
    public Long rowId() {
        return this.rowId;
    }

    @Override
    public String dataTypeName() {
        return this.dataType;
    }

    @Override
    public String columnName() {
        return this.columnName;
    }

    @Override
    public Object getContent() {
        return this.content;
    }
}

