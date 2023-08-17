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

import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.service.db.browser.DBObjectEditorFactory;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.db.browser.DBTableIndexEditorFactory;
import com.oceanbase.odc.service.db.model.OdcDBTableIndex;
import com.oceanbase.tools.dbbrowser.editor.DBTableIndexEditor;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.NonNull;

@Service
@SkipAuthorize("inside connect session")
public class DBTableIndexService {

    public List<OdcDBTableIndex> list(@NonNull ConnectionSession connectionSession,
            String schemaName, String tableName) {
        DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
        List<DBTableIndex> indices = accessor.listTableIndexes(schemaName, tableName);
        return indices.stream().map(OdcDBTableIndex::new).collect(Collectors.toList());
    }

    public String getDeleteSql(@NonNull ConnectionSession connectionSession,
            OdcDBTableIndex index) {
        DBObjectEditorFactory<DBTableIndexEditor> factory = new DBTableIndexEditorFactory(
                connectionSession.getConnectType(), ConnectionSessionUtil.getVersion(connectionSession));
        return factory.create().generateDropObjectDDL(index);
    }

}
