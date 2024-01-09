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
import com.oceanbase.odc.plugin.schema.api.SynonymExtensionPoint;
import com.oceanbase.odc.plugin.schema.oboracle.utils.DBAccessorUtil;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleObjectOperator;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleSynonymEditor;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBSynonym;
import com.oceanbase.tools.dbbrowser.model.DBSynonymType;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2023/6/29
 * @since 4.2.0
 */
@Extension
public class OBOracleSynonymExtension implements SynonymExtensionPoint {

    @Override
    public List<DBObjectIdentity> list(@NonNull Connection connection, @NonNull String schemaName,
            @NonNull DBSynonymType synonymType) {
        return getSchemaAccessor(connection).listSynonyms(schemaName, synonymType);
    }

    @Override
    public DBSynonym getDetail(@NonNull Connection connection, @NonNull String schemaName, @NonNull String synonymName,
            DBSynonymType synonymType) {
        return getSchemaAccessor(connection).getSynonym(schemaName, synonymName, synonymType);
    }

    @Override
    public void dropSynonym(@NonNull Connection connection, String schemaName, @NonNull String synonymName) {
        OracleObjectOperator operator = new OracleObjectOperator(JdbcOperationsUtil.getJdbcOperations(connection));
        operator.drop(DBObjectType.SYNONYM, null, synonymName);
    }

    @Override
    public void dropPublicSynonym(@NonNull Connection connection, String schemaName, @NonNull String synonymName) {
        OracleObjectOperator operator = new OracleObjectOperator(JdbcOperationsUtil.getJdbcOperations(connection));
        operator.drop(DBObjectType.PUBLIC_SYNONYM, null, synonymName);
    }

    @Override
    public String generateCreateDDL(@NonNull DBSynonym synonym) {
        OracleSynonymEditor editor = new OracleSynonymEditor();
        return editor.generateCreateDefinitionDDL(synonym);
    }

    protected DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return DBAccessorUtil.getSchemaAccessor(connection);
    }
}
