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
package com.oceanbase.odc.service.db.schema.syncer.column;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.oceanbase.odc.plugin.schema.api.ColumnExtensionPoint;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

import lombok.NonNull;

/**
 * @author gaoda.xy
 * @date 2024/4/10 20:13
 */
@Component
public class DBTableAndViewColumnSyncer extends AbstractDBColumnSyncer<ColumnExtensionPoint> {

    @Override
    Map<String, Set<String>> getLatestObjectToColumns(@NonNull ColumnExtensionPoint extensionPoint,
            @NonNull Connection connection, @NonNull Database database) {
        return extensionPoint.listBasicColumns(connection, database.getName()).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()
                        .stream().map(DBTableColumn::getName).collect(Collectors.toSet())));
    }

    @Override
    public Collection<DBObjectType> getColumnRelatedObjectTypes() {
        return Arrays.asList(DBObjectType.TABLE, DBObjectType.VIEW);
    }

    @Override
    Class<ColumnExtensionPoint> getExtensionPointClass() {
        return ColumnExtensionPoint.class;
    }

}
