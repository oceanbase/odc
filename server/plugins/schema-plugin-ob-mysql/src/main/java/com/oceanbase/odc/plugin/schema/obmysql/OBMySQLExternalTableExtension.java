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

import com.oceanbase.odc.plugin.schema.api.ExternalTableExtensionPoint;
import com.oceanbase.odc.plugin.schema.obmysql.utils.DBAccessorUtil;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2024/8/19 17:31
 * @since: 4.3.3
 */
@Extension
public class OBMySQLExternalTableExtension implements ExternalTableExtensionPoint {

    @Override
    public List<DBObjectIdentity> list(Connection connection, String schemaName) {
        return getSchemaAccessor(connection).showExternalTables(schemaName).stream().map(item -> {
            DBObjectIdentity identity = new DBObjectIdentity();
            identity.setType(DBObjectType.EXTERNAL_TABLE);
            identity.setSchemaName(schemaName);
            identity.setName(item);
            return identity;
        }).collect(Collectors.toList());
    }

    @Override
    public List<String> showNamesLike(Connection connection, String schemaName, String tableNameLike) {
        return null;
    }

    @Override
    public DBTable getDetail(Connection connection, String schemaName, String tableName) {
        return null;
    }

    @Override
    public void drop(Connection connection, String schemaName, String tableName) {

    }

    @Override
    public String generateCreateDDL(Connection connection, DBTable table) {
        return null;
    }

    @Override
    public String generateUpdateDDL(Connection connection, DBTable oldTable, DBTable newTable) {
        return null;
    }

    protected DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return DBAccessorUtil.getSchemaAccessor(connection);
    }
}
