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
package com.oceanbase.odc.core.migrate.resource.model;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * {@link DataRecord}
 *
 * @author yh263208
 * @date 2022-04-21 11:28
 * @since ODC_release_3.3.1
 */
@Getter
@ToString
@EqualsAndHashCode(of = "name2DataSpecs")
public class DataRecord {

    private final boolean allowDuplicated;
    private final String tableName;
    private final List<String> uniqueKeys;
    @Getter(AccessLevel.NONE)
    private final Map<String, DataSpec> name2DataSpecs;

    public DataRecord(@NonNull TableMetaData meta, @NonNull List<DataSpec> dataSpecs) {
        this(meta.getTable(), meta.isAllowDuplicate(), meta.getUniqueKeys(), dataSpecs);
    }

    private DataRecord(@NonNull String table, boolean allowDuplicated, List<String> uniqueKeys,
            @NonNull List<DataSpec> dataSpecs) {
        this.name2DataSpecs = dataSpecs.stream().collect(Collectors.toMap(DataSpec::getName, d -> d));
        this.tableName = table;
        this.allowDuplicated = allowDuplicated;
        this.uniqueKeys = uniqueKeys;
    }

    public static DataRecord copyFrom(@NonNull ResultSet resultSet, @NonNull DataRecord record) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        List<DataSpec> specs = new LinkedList<>();
        for (int j = 0; j < columnCount; j++) {
            String columnName = metaData.getColumnLabel(j + 1).toLowerCase();
            Object value = resultSet.getObject(j + 1);
            DataSpec src = record.name2DataSpecs.get(columnName);
            if (src == null) {
                throw new NullPointerException("DataSpec not found by name " + columnName);
            }
            specs.add(DataSpec.copyFrom(src, value));
        }
        return new DataRecord(record.getTableName(), record.isAllowDuplicated(), record.uniqueKeys, specs);
    }

    public Set<String> getKeys() {
        return name2DataSpecs.keySet();
    }

    public Collection<DataSpec> getData() {
        return name2DataSpecs.values();
    }

    public List<DataSpec> getUniqueKeyData() {
        if (CollectionUtils.isEmpty(uniqueKeys)) {
            return name2DataSpecs.values().stream().filter(dataSpec -> !dataSpec.isIgnore())
                    .collect(Collectors.toList());
        }
        return uniqueKeys.stream().map(s -> {
            DataSpec dataSpec = name2DataSpecs.get(s);
            if (dataSpec == null) {
                throw new NullPointerException("Spec dose not exist with name " + s);
            }
            if (dataSpec.isIgnore()) {
                throw new IllegalStateException("Spec is marked as ignore");
            }
            return dataSpec;
        }).collect(Collectors.toList());
    }

    public void refresh() {
        this.name2DataSpecs.values().stream()
                .filter(spec -> "id".equals(spec.getName())).forEach(DataSpec::refresh);
    }

}
