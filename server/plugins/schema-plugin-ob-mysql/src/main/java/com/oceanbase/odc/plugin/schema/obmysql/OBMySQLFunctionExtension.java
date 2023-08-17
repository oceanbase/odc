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

import org.pf4j.Extension;

import com.oceanbase.odc.plugin.connect.obmysql.util.JdbcOperationsUtil;
import com.oceanbase.odc.plugin.schema.api.FunctionExtensionPoint;
import com.oceanbase.odc.plugin.schema.obmysql.utils.DBAccessorUtil;
import com.oceanbase.tools.dbbrowser.editor.DBObjectOperator;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLObjectOperator;
import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBPLObjectIdentity;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.template.DBObjectTemplate;
import com.oceanbase.tools.dbbrowser.template.mysql.MySQLFunctionTemplate;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2023/6/29
 * @since 4.2.0
 */
@Extension
public class OBMySQLFunctionExtension implements FunctionExtensionPoint {

    @Override
    public List<DBPLObjectIdentity> list(@NonNull Connection connection, @NonNull String schemaName) {
        return getSchemaAccessor(connection).listFunctions(schemaName);
    }

    @Override
    public DBFunction getDetail(@NonNull Connection connection, @NonNull String schemaName,
            @NonNull String functionName) {
        return getSchemaAccessor(connection).getFunction(schemaName, functionName);
    }

    @Override
    public void drop(@NonNull Connection connection, String schemaName, @NonNull String functionName) {
        getOperator(connection).drop(DBObjectType.FUNCTION, null, functionName);
    }

    @Override
    public String generateCreateTemplate(@NonNull DBFunction function) {
        return getTemplate().generateCreateObjectTemplate(function);
    }

    protected DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return DBAccessorUtil.getSchemaAccessor(connection);
    }

    protected DBObjectOperator getOperator(Connection connection) {
        return new MySQLObjectOperator(JdbcOperationsUtil.getJdbcOperations(connection));
    }

    protected DBObjectTemplate<DBFunction> getTemplate() {
        return new MySQLFunctionTemplate();
    }

}
