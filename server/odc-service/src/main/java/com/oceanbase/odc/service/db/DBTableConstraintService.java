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

import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.service.db.browser.DBObjectEditorFactory;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.db.browser.DBTableConstraintEditorFactory;
import com.oceanbase.tools.dbbrowser.editor.DBTableConstraintEditor;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.NonNull;

@Service
@SkipAuthorize("inside connect session")
public class DBTableConstraintService {

    public List<DBTableConstraint> list(@NonNull ConnectionSession connectionSession,
            String schemaName, String tableName) {
        DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
        return accessor.listTableConstraints(schemaName, tableName);
    }

    public String getDeleteSql(@NonNull ConnectionSession connectionSession,
            DBTableConstraint constraint) {
        DBObjectEditorFactory<DBTableConstraintEditor> factory = new DBTableConstraintEditorFactory(
                connectionSession.getConnectType(), ConnectionSessionUtil.getVersion(connectionSession));
        return factory.create().generateDropObjectDDL(constraint);
    }

}
