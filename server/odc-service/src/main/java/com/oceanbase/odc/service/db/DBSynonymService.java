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
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.session.ConnectConsoleService;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleSynonymEditor;
import com.oceanbase.tools.dbbrowser.model.DBSynonym;
import com.oceanbase.tools.dbbrowser.model.DBSynonymType;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.NonNull;

/**
 * ODC同义词Service对象
 *
 * @author yh263208
 * @date 2020-12-19 20:47
 * @since ODC_release_2.4.0
 */
@Service
@SkipAuthorize("inside connect session")
public class DBSynonymService {
    @Autowired
    private ConnectConsoleService consoleService;

    public List<DBSynonym> list(ConnectionSession connectionSession, String dbName,
            DBSynonymType synonymType) {
        DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
        return accessor.listSynonyms(dbName, synonymType).stream().map(item -> {
            DBSynonym synonym = new DBSynonym();
            synonym.setSynonymName(item.getName());
            return synonym;
        }).collect(Collectors.toList());
    }

    public String generateCreateSql(@NonNull ConnectionSession session, @NonNull DBSynonym synonym) {
        dialectCheck(session);
        OracleSynonymEditor editor = new OracleSynonymEditor();
        return editor.generateCreateDefinitionDDL(synonym);
    }

    public DBSynonym detail(ConnectionSession connectionSession, DBSynonym synonym) {
        DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
        return accessor.getSynonym(synonym.getOwner(), synonym.getSynonymName(), synonym.getSynonymType());
    }

    private void dialectCheck(@NonNull ConnectionSession session) {
        if (session.getDialectType() != DialectType.OB_ORACLE) {
            throw new UnsupportedOperationException("Synonym is not supported for " + session.getDialectType());
        }
    }

}
