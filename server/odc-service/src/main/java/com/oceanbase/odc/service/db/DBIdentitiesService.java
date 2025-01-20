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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.db.model.SchemaIdentities;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

@Service
@SkipAuthorize("inside connect session")
public class DBIdentitiesService {

    public List<SchemaIdentities> list(ConnectionSession session, List<DBObjectType> types) {
        if (CollectionUtils.isEmpty(types)) {
            return Collections.emptyList();
        }
        DBSchemaAccessor schemaAccessor = DBSchemaAccessors.create(session);
        Map<String, SchemaIdentities> all = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (types.contains(DBObjectType.VIEW)) {
            listViews(schemaAccessor, all);
        }
        if (types.contains(DBObjectType.TABLE)) {
            listTables(schemaAccessor, all);
        }
        schemaAccessor.showDatabases().forEach(db -> all.computeIfAbsent(db, SchemaIdentities::of));
        return new ArrayList<>(all.values());
    }

    void listTables(DBSchemaAccessor schemaAccessor, Map<String, SchemaIdentities> all) {
        schemaAccessor.listTables(null, null)
                .forEach(i -> all.computeIfAbsent(i.getSchemaName(), SchemaIdentities::of).add(i));
    }

    void listViews(DBSchemaAccessor schemaAccessor, Map<String, SchemaIdentities> all) {
        schemaAccessor.listAllUserViews()
                .forEach(i -> all.computeIfAbsent(i.getSchemaName(), SchemaIdentities::of).add(i));
        schemaAccessor.listAllSystemViews()
                .forEach(i -> all.computeIfAbsent(i.getSchemaName(), SchemaIdentities::of).add(i));
    }

}
