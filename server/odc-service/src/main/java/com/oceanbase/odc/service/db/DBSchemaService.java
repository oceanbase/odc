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
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.tools.dbbrowser.model.DBDatabase;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@SkipAuthorize("inside connect session")
public class DBSchemaService {

    public List<DBDatabase> listDatabases(ConnectionSession connectionSession) {
        DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
        return accessor.listDatabases();
    }

    public List<String> showDatabases(ConnectionSession connectionSession) {
        DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
        return accessor.showDatabases();
    }



    public DBDatabase detail(ConnectionSession connectionSession, String dbName) {
        DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
        return accessor.getDatabase(dbName);
    }

}
