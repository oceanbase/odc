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
package com.oceanbase.odc.service.db;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.common.model.ResourceSql;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.session.ConnectConsoleService;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleSequenceEditor;
import com.oceanbase.tools.dbbrowser.model.DBSequence;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.NonNull;

@Service
@SkipAuthorize("inside connect session")
public class DBSequenceService {
    @Autowired
    private ConnectConsoleService consoleService;

    public List<DBSequence> list(ConnectionSession connectionSession, String dbName) {
        DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
        return accessor.listSequences(dbName).stream().map(item -> {
            DBSequence sequence = new DBSequence();
            sequence.setName(item.getName());
            return sequence;
        }).collect(Collectors.toList());
    }

    public DBSequence detail(ConnectionSession connectionSession, String schemaName, String sequenceName) {
        DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
        return accessor.getSequence(schemaName, sequenceName);
    }

    public ResourceSql getCreateSql(@NonNull ConnectionSession session,
            @NonNull DBSequence sequence) {
        dialectCheck(session);
        OracleSequenceEditor editor = new OracleSequenceEditor();
        String ddl = editor.generateCreateDefinitionDDL(sequence);
        return ResourceSql.ofSql(ddl);
    }

    public ResourceSql getUpdateSql(@NonNull ConnectionSession session,
            @NonNull DBSequence sequence) {
        dialectCheck(session);
        OracleSequenceEditor editor = new OracleSequenceEditor();
        DBSequence oldOne = new DBSequence();
        oldOne.setName(sequence.getName());
        String sql = editor.generateUpdateObjectDDL(oldOne, sequence);
        return ResourceSql.ofSql(sql);
    }

    private void dialectCheck(@NonNull ConnectionSession session) {
        if (session.getDialectType() != DialectType.OB_ORACLE) {
            throw new UnsupportedOperationException("Sequence is not supported for " + session.getDialectType());
        }
    }

}
