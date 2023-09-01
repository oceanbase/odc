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
import com.oceanbase.odc.plugin.schema.api.SequenceExtensionPoint;
import com.oceanbase.odc.plugin.schema.oboracle.utils.DBAccessorUtil;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleObjectOperator;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleSequenceEditor;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBSequence;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2023/6/29
 * @since 4.2.0
 */
@Extension
public class OBOracleSequenceExtension implements SequenceExtensionPoint {

    @Override
    public List<DBObjectIdentity> list(@NonNull Connection connection, @NonNull String schemaName) {
        return DBAccessorUtil.getSchemaAccessor(connection).listSequences(schemaName);
    }

    @Override
    public DBSequence getDetail(@NonNull Connection connection, @NonNull String schemaName,
            @NonNull String sequenceName) {
        return DBAccessorUtil.getSchemaAccessor(connection).getSequence(schemaName, sequenceName);
    }

    @Override
    public void drop(@NonNull Connection connection, String schemaName, @NonNull String sequenceName) {
        OracleObjectOperator operator = new OracleObjectOperator(JdbcOperationsUtil.getJdbcOperations(connection));
        operator.drop(DBObjectType.SEQUENCE, null, sequenceName);
    }

    @Override
    public String generateCreateDDL(@NonNull DBSequence sequence) {
        OracleSequenceEditor editor = new OracleSequenceEditor();
        return editor.generateCreateDefinitionDDL(sequence);
    }

    @Override
    public String generateUpdateDDL(@NonNull DBSequence oldSequence, @NonNull DBSequence newSequence) {
        OracleSequenceEditor editor = new OracleSequenceEditor();
        return editor.generateUpdateObjectDDL(oldSequence, newSequence);
    }
}
