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
package com.oceanbase.odc.service.datasecurity.accessor;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author gaoda.xy
 * @date 2023/6/12 14:14
 */
public class DatasourceColumnAccessor implements ColumnAccessor {

    private final DBSchemaAccessor accessor;

    private final LoadingCache<DatabaseTable, List<DBTableColumn>> databaseTable2Columns = Caffeine.newBuilder()
            .maximumSize(1000).expireAfterWrite(3, TimeUnit.MINUTES).build(this::queryColumns);

    public DatasourceColumnAccessor(ConnectionSession session) {
        this.accessor = DBSchemaAccessors.create(session);
    }

    @Override
    public List<String> getColumns(String databaseName, String objectName) {
        DatabaseTable dt = new DatabaseTable(databaseName, objectName);
        return Objects.requireNonNull(databaseTable2Columns.get(dt)).stream().map(DBTableColumn::getName)
                .collect(Collectors.toList());
    }

    private List<DBTableColumn> queryColumns(DatabaseTable databaseTable) {
        List<DBTableColumn> columns =
                accessor.listBasicTableColumns(databaseTable.getDatabaseName(), databaseTable.getObjectName());
        if (CollectionUtils.isEmpty(columns)) {
            // If the table is not found, then try to query view
            columns = accessor.getView(databaseTable.getDatabaseName(), databaseTable.getObjectName()).getColumns();
        }
        return columns;
    }

    @Data
    @EqualsAndHashCode
    @AllArgsConstructor
    private static class DatabaseTable {
        /**
         * Name of database or schema
         */
        private String databaseName;
        /**
         * Name of table or view
         */
        private String objectName;
    }

}
