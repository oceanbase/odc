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

import com.oceanbase.odc.core.sql.execute.cache.KeyValueRepository;
import com.oceanbase.odc.core.sql.execute.cache.table.VirtualElement;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Virtual elements used to encapsulate binary objects, such as blobs, clobs, etc.
 *
 * @author yh263208
 * @date 2021-11-02 21:35
 * @since ODC_release_3.2.2
 */
@Slf4j
public class CachedBinaryVirtualElement implements VirtualElement {

    private final Integer columnId;
    private final Long rowId;
    private final String dataType;
    private final String tableId;
    private final String columnName;
    private final KeyValueRepository keyValueRepository;

    public CachedBinaryVirtualElement(@NonNull String tableId,
            @NonNull Long rowId,
            @NonNull Integer columnId,
            @NonNull String dataType,
            @NonNull String columnName,
            @NonNull BinaryContentMetaData metaData,
            @NonNull KeyValueRepository keyValueRepository) {
        this.keyValueRepository = keyValueRepository;
        this.tableId = tableId;
        this.rowId = rowId;
        this.columnId = columnId;
        this.dataType = dataType;
        this.columnName = columnName;
        keyValueRepository.put(getStorageKey(), metaData);
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

    public String getStorageKey() {
        return "/" + this.tableId + "/" + this.rowId + "/" + this.columnId;
    }

    @Override
    public BinaryContentMetaData getContent() {
        Object value = this.keyValueRepository.get(getStorageKey());
        if (value == null) {
            return null;
        }
        return (BinaryContentMetaData) value;
    }

}
