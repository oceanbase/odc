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
package com.oceanbase.odc.service.db.schema.syncer.object;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;

import com.oceanbase.odc.metadb.dbobject.DBColumnRepository;
import com.oceanbase.odc.metadb.dbobject.DBObjectEntity;
import com.oceanbase.odc.metadb.dbobject.DBObjectRepository;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.db.schema.syncer.DBSchemaSyncer;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.NonNull;

/**
 * @author gaoda.xy
 * @date 2024/4/9 17:06
 */
public abstract class AbstractDBObjectSyncer implements DBSchemaSyncer {

    @Autowired
    private DBObjectRepository dbObjectRepository;

    @Autowired
    private DBColumnRepository dbColumnRepository;

    @Override
    public void sync(@NonNull DBSchemaAccessor accessor, @NonNull Database database) {
        Set<String> latestObjectNames = getLatestObjectNames(accessor, database);
        List<DBObjectEntity> existingObjects =
                dbObjectRepository.findByDatabaseIdAndType(database.getId(), getObjectType());
        // Insert objects that are not in the existing object list
        Set<String> existingObjectNames =
                existingObjects.stream().map(DBObjectEntity::getName).collect(Collectors.toSet());
        List<DBObjectEntity> toBeInserted = latestObjectNames.stream().filter(e -> !existingObjectNames.contains(e))
                .map(e -> {
                    DBObjectEntity entity = new DBObjectEntity();
                    entity.setName(e);
                    entity.setType(getObjectType());
                    entity.setDatabaseId(database.getId());
                    entity.setOrganizationId(database.getOrganizationId());
                    return entity;
                }).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(toBeInserted)) {
            dbObjectRepository.batchCreate(toBeInserted);
        }
        // Delete objects that are not in the latest object list
        List<DBObjectEntity> toBeDeleted = existingObjects.stream()
                .filter(e -> !latestObjectNames.contains(e.getName())).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(toBeDeleted)) {
            Set<Long> toBeDeletedIds = toBeDeleted.stream().map(DBObjectEntity::getId).collect(Collectors.toSet());
            dbObjectRepository.deleteByIds(toBeDeletedIds);
            dbColumnRepository.deleteByDatabaseIdAndObjectIdIn(database.getId(), toBeDeletedIds);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    abstract Set<String> getLatestObjectNames(@NonNull DBSchemaAccessor accessor, @NonNull Database database);

}
