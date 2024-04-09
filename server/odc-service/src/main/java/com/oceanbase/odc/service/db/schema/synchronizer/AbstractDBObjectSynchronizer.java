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
package com.oceanbase.odc.service.db.schema.synchronizer;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.metadb.dbobject.DBObjectEntity;
import com.oceanbase.odc.metadb.dbobject.DBObjectRepository;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2024/4/9 17:06
 */
@Slf4j
public abstract class AbstractDBObjectSynchronizer implements DBMetadataSynchronizer {

    @Autowired
    protected DBObjectRepository repository;

    @Override
    public void sync(@NonNull DBSchemaAccessor accessor, @NonNull Database database) {
        try {
            Set<String> latestObjectNames = getLatestObjectNames(accessor, database);
            List<DBObjectEntity> existingObjects =
                    repository.findByDatabaseIdAndType(database.getId(), getObjectType());
            // Delete objects that are not in the latest object list
            List<DBObjectEntity> toBeDeleted = existingObjects.stream()
                    .filter(e -> !latestObjectNames.contains(e.getName())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(toBeDeleted)) {
                repository.deleteByIds(toBeDeleted.stream().map(DBObjectEntity::getId).collect(Collectors.toList()));
            }
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
                repository.batchCreate(toBeInserted);
            }
        } catch (Exception e) {
            log.warn("Failed to synchronize {} for database id={}", getObjectType(), database.getId(), e);
        }
    }

    abstract Set<String> getLatestObjectNames(@NonNull DBSchemaAccessor accessor, @NonNull Database database);

    abstract DBObjectType getObjectType();

}
