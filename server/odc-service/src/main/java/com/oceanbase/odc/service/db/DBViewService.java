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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.plugin.schema.api.ViewExtensionPoint;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.db.model.AllTablesAndViews;
import com.oceanbase.odc.service.db.model.DBViewResponse;
import com.oceanbase.odc.service.db.model.DatabaseAndTables;
import com.oceanbase.odc.service.db.model.DatabaseAndViews;
import com.oceanbase.odc.service.plugin.SchemaPluginUtil;
import com.oceanbase.odc.service.session.ConnectConsoleService;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBView;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SkipAuthorize("inside connect session")
public class DBViewService {
    @Autowired
    private ConnectConsoleService consoleService;

    public List<String> listSystemViews(@NonNull ConnectionSession session, String databaseName) {
        DBSchemaAccessor schemaAccessor = DBSchemaAccessors.create(session);
        return schemaAccessor.showSystemViews(databaseName);
    }

    public List<DBView> list(ConnectionSession connectionSession, String dbName) {
        return connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<List<DBObjectIdentity>>) con -> getDBViewExtensionPoint(
                        connectionSession).list(con, dbName))
                .stream().map(identity -> DBView.of(identity.getSchemaName(), identity.getName()))
                .collect(Collectors.toList());
    }

    public DBViewResponse detail(ConnectionSession connectionSession, String schemaName, String viewName) {
        return new DBViewResponse(connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<DBView>) con -> getDBViewExtensionPoint(connectionSession)
                        .getDetail(con, schemaName, viewName)));
    }

    public String getCreateSql(@NonNull ConnectionSession session,
            @NonNull DBView resource) {
        return session.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<String>) con -> getDBViewExtensionPoint(session)
                        .generateCreateTemplate(resource));
    }

    public AllTablesAndViews listAllTableAndView(ConnectionSession connectionSession,
            String tableNameLike) {
        AllTablesAndViews allResult = new AllTablesAndViews();
        DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
        List<DatabaseAndTables> tables = new ArrayList<>();
        if (connectionSession.getConnectType().equals(ConnectType.ODP_SHARDING_OB_MYSQL)) {
            List<String> names = accessor.showTablesLike(null, tableNameLike).stream()
                    .filter(name -> !StringUtils.endsWithIgnoreCase(name, OdcConstants.VALIDATE_DDL_TABLE_POSTFIX))
                    .collect(Collectors.toList());
            DatabaseAndTables databaseAndTables = new DatabaseAndTables(
                    ConnectionSessionUtil.getCurrentSchema(connectionSession), names);
            tables.add(databaseAndTables);
        } else {
            List<String> databases = accessor.showDatabases();
            tables = databases.stream().map(schema -> {
                List<String> tablesLike = accessor.showTablesLike(schema, tableNameLike).stream()
                        .filter(name -> !StringUtils.endsWith(name.toUpperCase(),
                                OdcConstants.VALIDATE_DDL_TABLE_POSTFIX))
                        .collect(Collectors.toList());
                return tablesLike.size() != 0 ? new DatabaseAndTables(schema, tablesLike)
                        : new DatabaseAndTables();
            }).filter(item -> item.getDatabaseName() != null)
                    .sorted(Comparator.comparing(DatabaseAndTables::getDatabaseName)).collect(Collectors.toList());
        }

        List<DBObjectIdentity> viewsIdentities = accessor.listAllViews(tableNameLike);
        Map<String, List<String>> schema2views = new HashMap<>();
        viewsIdentities.forEach(item -> {
            List<String> views = schema2views.computeIfAbsent(item.getSchemaName(), t -> new ArrayList<>());
            views.add(item.getName());
        });
        List<DatabaseAndViews> views = new ArrayList<>();
        schema2views.forEach((schema, viewNames) -> {
            DatabaseAndViews view = new DatabaseAndViews();
            view.setDatabaseName(schema);
            view.setViews(viewNames);
            views.add(view);
        });
        allResult.setTables(tables);
        allResult.setViews(views);
        return allResult;
    }

    private ViewExtensionPoint getDBViewExtensionPoint(@NonNull ConnectionSession session) {
        return SchemaPluginUtil.getViewExtension(session.getDialectType());
    }

}
