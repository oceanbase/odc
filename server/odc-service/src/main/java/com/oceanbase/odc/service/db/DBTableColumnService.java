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
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.db.browser.DBTableColumnEditorFactory;
import com.oceanbase.odc.service.db.model.OdcDBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

@Service
@SkipAuthorize("inside connect session")
public class DBTableColumnService {

    public List<OdcDBTableColumn> list(ConnectionSession connectionSession,
            String databaseName, String tableName) {
        DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
        List<DBTableColumn> columns = accessor.listTableColumns(databaseName, tableName);
        return columns.stream().map(OdcDBTableColumn::new).collect(Collectors.toList());
    }

    public String getCreateSql(ConnectionSession session, OdcDBTableColumn column) {
        DBTableColumnEditorFactory factory = new DBTableColumnEditorFactory(
                session.getConnectType(), ConnectionSessionUtil.getVersion(session));
        return factory.create().generateCreateObjectDDL(column);
    }

    public String getDeleteSql(ConnectionSession session, OdcDBTableColumn column) {
        DBTableColumnEditorFactory factory = new DBTableColumnEditorFactory(
                session.getConnectType(), ConnectionSessionUtil.getVersion(session));
        return factory.create().generateDropObjectDDL(column);
    }

}
