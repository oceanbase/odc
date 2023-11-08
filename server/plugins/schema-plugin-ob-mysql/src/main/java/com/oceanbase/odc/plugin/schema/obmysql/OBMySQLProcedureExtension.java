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

import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.plugin.schema.api.ProcedureExtensionPoint;
import com.oceanbase.odc.plugin.schema.obmysql.utils.DBAccessorUtil;
import com.oceanbase.tools.dbbrowser.editor.DBObjectOperator;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLObjectOperator;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBPLObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.template.DBObjectTemplate;
import com.oceanbase.tools.dbbrowser.template.mysql.MySQLProcedureTemplate;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2023/6/29
 * @since 4.2.0
 */
@Extension
public class OBMySQLProcedureExtension implements ProcedureExtensionPoint {

    @Override
    public List<DBPLObjectIdentity> list(@NonNull Connection connection, @NonNull String schemaName) {
        return getSchemaAccessor(connection).listProcedures(schemaName);
    }

    @Override
    public DBProcedure getDetail(@NonNull Connection connection, @NonNull String schemaName,
            @NonNull String procedureName) {
        return getSchemaAccessor(connection).getProcedure(schemaName, procedureName);
    }

    @Override
    public void drop(@NonNull Connection connection, String schemaName, @NonNull String procedureName) {
        getOperator(connection).drop(DBObjectType.PROCEDURE, null, procedureName);
    }

    @Override
    public String generateCreateTemplate(@NonNull DBProcedure procedure) {
        return getTemplate().generateCreateObjectTemplate(procedure);
    }

    protected DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return DBAccessorUtil.getSchemaAccessor(connection);
    }

    protected DBObjectOperator getOperator(Connection connection) {
        return new MySQLObjectOperator(JdbcOperationsUtil.getJdbcOperations(connection));
    }

    protected DBObjectTemplate<DBProcedure> getTemplate() {
        return new MySQLProcedureTemplate();
    }
}
