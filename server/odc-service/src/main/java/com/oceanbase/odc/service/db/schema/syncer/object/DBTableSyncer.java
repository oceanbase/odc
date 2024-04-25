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

import com.oceanbase.odc.plugin.schema.api.TableExtensionPoint;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.NonNull;

/**
 * @author gaoda.xy
 * @date 2024/4/9 17:18
 */
@Component
public class DBTableSyncer extends AbstractDBObjectSyncer<TableExtensionPoint> {

    @Override
    protected Set<String> getLatestObjectNames(@NonNull TableExtensionPoint extensionPoint,
            @NonNull Connection connection, @NonNull Database database) {
        return extensionPoint.list(connection, database.getName()).stream().map(DBObjectIdentity::getName)
                .collect(Collectors.toSet());
    }

    @Override
    Class<TableExtensionPoint> getExtensionPointClass() {
        return TableExtensionPoint.class;
    }

    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.TABLE;
    }

}
