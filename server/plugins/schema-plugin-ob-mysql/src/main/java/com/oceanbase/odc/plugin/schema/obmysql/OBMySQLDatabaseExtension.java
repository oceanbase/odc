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
package com.oceanbase.odc.plugin.schema.obmysql;

import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;

import org.pf4j.Extension;

import com.oceanbase.odc.plugin.schema.api.DatabaseExtensionPoint;
import com.oceanbase.odc.plugin.schema.obmysql.utils.DBAccessorUtil;
import com.oceanbase.tools.dbbrowser.model.DBDatabase;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2023/6/28
 * @since 4.2.0
 */
@Extension
public class OBMySQLDatabaseExtension implements DatabaseExtensionPoint {

    @Override
    public List<DBObjectIdentity> list(@NonNull Connection connection) {
        return getSchemaAccessor(connection).showDatabases().stream().map(item -> {
            DBObjectIdentity identity = new DBObjectIdentity();
            identity.setName(item);
            identity.setType(DBObjectType.DATABASE);
            return identity;
        }).collect(Collectors.toList());
    }

    @Override
    public DBDatabase getDetail(@NonNull Connection connection, @NonNull String dbName) {
        return getSchemaAccessor(connection).getDatabase(dbName);
    }

    @Override
    public List<DBDatabase> listDetails(@NonNull Connection connection) {
        DBSchemaAccessor accessor = getSchemaAccessor(connection);
        return accessor.showDatabases().stream().map(accessor::getDatabase).collect(Collectors.toList());
    }

    protected DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return DBAccessorUtil.getSchemaAccessor(connection);
    }

}
