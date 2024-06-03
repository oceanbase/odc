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
import com.oceanbase.odc.plugin.schema.api.SequenceExtensionPoint;
import com.oceanbase.odc.plugin.schema.obmysql.utils.DBAccessorUtil;
import com.oceanbase.tools.dbbrowser.editor.DBObjectOperator;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLObjectOperator;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBSequence;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

@Extension
public class OBMySQLSequenceExtension implements SequenceExtensionPoint {

    @Override
    public List<DBObjectIdentity> list(Connection connection, String schemaName) {
        return getSchemaAccessor(connection).listSequences(schemaName);
    }

    @Override
    public DBSequence getDetail(Connection connection, String schemaName, String sequenceName) {
        return getSchemaAccessor(connection).getSequence(schemaName, sequenceName);
    }

    @Override
    public void drop(Connection connection, String schemaName, String sequenceName) {
        DBObjectOperator operator = getOperator(connection);
        operator.drop(DBObjectType.SEQUENCE, schemaName, sequenceName);
    }

    @Override
    public String generateCreateDDL(DBSequence sequence) {
        // todo OBMySQLSequenceEditor
        throw new UnsupportedOperationException();
    }

    @Override
    public String generateUpdateDDL(DBSequence oldSequence, DBSequence newSequence) {
        // todo OBMySQLSequenceEditor
        throw new UnsupportedOperationException();
    }

    protected DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return DBAccessorUtil.getSchemaAccessor(connection);
    }

    protected DBObjectOperator getOperator(Connection connection) {
        return new MySQLObjectOperator(JdbcOperationsUtil.getJdbcOperations(connection));
    }

}
