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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;

import com.oceanbase.odc.metadb.dbobject.DBColumnEntity;
import com.oceanbase.odc.metadb.dbobject.DBColumnRepository;
import com.oceanbase.odc.metadb.dbobject.DBObjectEntity;
import com.oceanbase.odc.metadb.dbobject.DBObjectRepository;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.db.schema.syncer.DBSchemaSyncer;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.NonNull;

/**
 * @author gaoda.xy
 * @date 2024/4/10 10:05
 */
public abstract class AbstractDBColumnSyncer implements DBSchemaSyncer {

    @Autowired
    private DBObjectRepository dbObjectRepository;

    @Autowired
    private DBColumnRepository dbColumnRepository;

    @Override
    public void sync(@NonNull DBSchemaAccessor accessor, @NonNull Database database) {
        Map<String, Set<String>> latestObject2Columns = getLatestObjectToColumns(accessor, database);
        Map<String, DBObjectEntity> existingObject2Entity =
                dbObjectRepository.findByDatabaseIdAndType(database.getId(), getObjectType()).stream()
                        .collect(Collectors.toMap(DBObjectEntity::getName, e -> e, (e1, e2) -> e1));
        if (CollectionUtils.isEmpty(existingObject2Entity.entrySet())) {
            return;
        }
        Set<Long> existingObjectIds =
                existingObject2Entity.values().stream().map(DBObjectEntity::getId).collect(Collectors.toSet());
        Map<Long, List<DBColumnEntity>> existingObjectId2ColumnEntities =
                dbColumnRepository.findByDatabaseIdAndObjectIdIn(database.getId(), existingObjectIds).stream()
                        .collect(Collectors.groupingBy(DBColumnEntity::getObjectId));
        // Insert columns that are not in the existing column list
        List<DBColumnEntity> toBeInserted = new ArrayList<>();
        List<DBColumnEntity> toBeDeleted = new ArrayList<>();
        for (Entry<String, DBObjectEntity> entry : existingObject2Entity.entrySet()) {
            String objectName = entry.getKey();
            DBObjectEntity objectEntity = entry.getValue();
            Set<String> latestColumns = latestObject2Columns.getOrDefault(objectName, new HashSet<>());
            List<DBColumnEntity> existingColumns =
                    existingObjectId2ColumnEntities.getOrDefault(objectEntity.getId(), new ArrayList<>());
            Set<String> existingColumnNames =
                    existingColumns.stream().map(DBColumnEntity::getName).collect(Collectors.toSet());
            for (String latestColumn : latestColumns) {
                if (!existingColumnNames.contains(latestColumn)) {
                    DBColumnEntity columnEntity = new DBColumnEntity();
                    columnEntity.setName(latestColumn);
                    columnEntity.setDatabaseId(database.getId());
                    columnEntity.setObjectId(objectEntity.getId());
                    columnEntity.setOrganizationId(database.getOrganizationId());
                    toBeInserted.add(columnEntity);
                }
            }
            for (DBColumnEntity existingColumn : existingColumns) {
                if (!latestColumns.contains(existingColumn.getName())) {
                    toBeDeleted.add(existingColumn);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(toBeInserted)) {
            dbColumnRepository.batchCreate(toBeInserted);
        }
        if (CollectionUtils.isNotEmpty(toBeDeleted)) {
            dbColumnRepository
                    .deleteByIds(toBeDeleted.stream().map(DBColumnEntity::getId).collect(Collectors.toList()));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    abstract Map<String, Set<String>> getLatestObjectToColumns(@NonNull DBSchemaAccessor accessor,
            @NonNull Database database);

}
