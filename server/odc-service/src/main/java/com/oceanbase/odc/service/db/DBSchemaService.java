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

import java.sql.Connection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.plugin.SchemaPluginUtil;
import com.oceanbase.tools.dbbrowser.model.DBDatabase;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@SkipAuthorize("inside connect session")
public class DBSchemaService {

    public List<DBDatabase> listDatabases(ConnectionSession sess) {
        return sess.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<List<DBDatabase>>) con -> listDatabases(sess.getDialectType(), con));
    }

    public List<DBDatabase> listDatabases(@NonNull DialectType dialectType, @NonNull Connection connection) {
        return SchemaPluginUtil.getDatabaseExtension(dialectType).listDetails(connection);
    }

    public Set<String> showDatabases(@NonNull DialectType dialectType, @NonNull Connection connection) {
        return SchemaPluginUtil.getDatabaseExtension(dialectType).list(connection)
                .stream().map(DBObjectIdentity::getName).collect(Collectors.toSet());
    }

    public DBDatabase detail(@NonNull DialectType dialectType, @NonNull Connection connection, String dbName) {
        return SchemaPluginUtil.getDatabaseExtension(dialectType).getDetail(connection, dbName);
    }

    public DBDatabase detail(ConnectionSession sess, String dbName) {
        return sess.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<DBDatabase>) con -> detail(sess.getDialectType(), con, dbName));
    }

}
