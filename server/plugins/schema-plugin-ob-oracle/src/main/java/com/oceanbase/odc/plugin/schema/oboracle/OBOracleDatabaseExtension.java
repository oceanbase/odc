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
package com.oceanbase.odc.plugin.schema.oboracle;

import java.sql.Connection;
import java.util.Map;

import org.pf4j.Extension;

import com.oceanbase.odc.plugin.schema.model.SchemaPluginConstants;
import com.oceanbase.odc.plugin.schema.obmysql.OBMySQLDatabaseExtension;
import com.oceanbase.odc.plugin.schema.oboracle.utils.DBAccessorUtil;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;

/**
 * @author jingtian
 * @date 2023/6/29
 * @since 4.2.0
 */
@Extension
public class OBOracleDatabaseExtension extends OBMySQLDatabaseExtension {
    @Override
    protected DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return DBAccessorUtil.getSchemaAccessor(connection);
    }

    @Override
    public String getCreateDatabaseSql(String databaseName, Map<String, String> parameters) {
        OracleSqlBuilder sqlBuilder = new OracleSqlBuilder();
        if (!parameters.containsKey(SchemaPluginConstants.CREATE_USER_PASSWORD)) {
            throw new IllegalStateException("password cannot be null for ob oracle creating user");
        }
        sqlBuilder.append("CREATE USER ").identifier(databaseName).append(" IDENTIFIED BY ")
                .identifier(parameters.get(SchemaPluginConstants.CREATE_USER_PASSWORD));
        return sqlBuilder.toString();
    }
}
