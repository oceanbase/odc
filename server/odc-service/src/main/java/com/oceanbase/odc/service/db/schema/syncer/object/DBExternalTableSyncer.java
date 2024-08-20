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

import java.sql.Connection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.oceanbase.odc.plugin.schema.api.ExternalTableExtensionPoint;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.NonNull;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2024/8/19 17:33
 * @since: 4.3.3
 */
@Component
public class DBExternalTableSyncer extends AbstractDBObjectSyncer<ExternalTableExtensionPoint> {
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.EXTERNAL_TABLE;
    }

    @Override
    Set<String> getLatestObjectNames(@NonNull ExternalTableExtensionPoint extensionPoint,
        @NonNull Connection connection, @NonNull Database database) {
        return extensionPoint.list(connection, database.getName()).stream().map(DBObjectIdentity::getName)
            .collect(Collectors.toSet());
    }

    @Override
    Class<ExternalTableExtensionPoint> getExtensionPointClass() {
        return ExternalTableExtensionPoint.class;
    }
}
