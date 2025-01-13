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
package com.oceanbase.odc.service.connection.listener;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.common.event.AbstractEventListener;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.service.connection.database.model.DatabaseSyncStatus;
import com.oceanbase.odc.service.connection.database.model.DatabaseType;
import com.oceanbase.odc.service.connection.event.UpsertDatasourceEvent;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.schema.model.DBObjectSyncStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2024/12/30 10:47
 * @Descripition:
 */
@Slf4j
@Component
public class UpdateDatasourceListener extends AbstractEventListener<UpsertDatasourceEvent> {

    @Autowired
    private DatabaseRepository databaseRepository;

    @Override
    public void onEvent(UpsertDatasourceEvent event) {

        ConnectionConfig connectionConfig = event.getConnectionConfig();
        if (connectionConfig.getDialectType() != DialectType.FILE_SYSTEM) {
            return;
        }
        List<DatabaseEntity> byConnectionId = databaseRepository.findByConnectionId(connectionConfig.getId());
        DatabaseEntity entity = null;
        if (!CollectionUtils.isEmpty(byConnectionId)) {
            List<Long> toBeDelete = byConnectionId.stream().filter(
                    o -> !connectionConfig.getHost().equals(o.getName())).map(DatabaseEntity::getId).collect(
                            Collectors.toList());
            if (!toBeDelete.isEmpty()) {
                databaseRepository.deleteAllById(toBeDelete);
            }
            Optional<DatabaseEntity> existed = byConnectionId.stream().filter(
                    o -> connectionConfig.getHost().equals(o.getName())).findFirst();
            if (existed.isPresent()) {
                entity = existed.get();
            }
        }
        // create or update
        entity = entity == null ? new DatabaseEntity() : entity;
        entity.setDatabaseId(com.oceanbase.odc.common.util.StringUtils.uuid());
        entity.setOrganizationId(connectionConfig.getOrganizationId());
        entity.setName(connectionConfig.getHost());
        entity.setProjectId(connectionConfig.getProjectId());
        entity.setConnectionId(connectionConfig.getId());
        entity.setEnvironmentId(connectionConfig.getEnvironmentId());
        entity.setSyncStatus(DatabaseSyncStatus.SUCCEEDED);
        entity.setExisted(true);
        entity.setObjectSyncStatus(DBObjectSyncStatus.SYNCED);
        entity.setConnectType(connectionConfig.getType());
        entity.setType(DatabaseType.PHYSICAL);
        databaseRepository.save(entity);

    }
}
