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
import java.util.List;

import org.pf4j.Extension;

import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.plugin.schema.api.TypeExtensionPoint;
import com.oceanbase.odc.plugin.schema.oboracle.utils.DBAccessorUtil;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleObjectOperator;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBPLObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBType;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.template.DBObjectTemplate;
import com.oceanbase.tools.dbbrowser.template.oracle.OracleTypeTemplate;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2023/6/29
 * @since 4.2.0
 */
@Extension
public class OBOracleTypeExtension implements TypeExtensionPoint {

    @Override
    public List<DBPLObjectIdentity> list(@NonNull Connection connection, @NonNull String schemaName) {
        return getSchemaAccessor(connection).listTypes(schemaName);
    }

    @Override
    public DBType getDetail(@NonNull Connection connection, @NonNull String schemaName, @NonNull String typeName) {
        return getSchemaAccessor(connection).getType(schemaName, typeName);
    }

    @Override
    public void drop(@NonNull Connection connection, String schemaName, @NonNull String typeName) {
        OracleObjectOperator operator = new OracleObjectOperator(JdbcOperationsUtil.getJdbcOperations(connection));
        operator.drop(DBObjectType.TYPE, null, typeName);
    }

    @Override
    public String generateCreateTemplate(@NonNull DBType type) {
        DBObjectTemplate<DBType> template = new OracleTypeTemplate();
        return template.generateCreateObjectTemplate(type);
    }

    protected DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return DBAccessorUtil.getSchemaAccessor(connection);
    }
}
