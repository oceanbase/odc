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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBView;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SkipAuthorize("inside connect session")
public class DBViewService {
    @Autowired
    private DBTableService dbTableService;

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
        List<String> existedDatabases = accessor.showDatabases();;
        if (connectionSession.getConnectType().equals(ConnectType.ODP_SHARDING_OB_MYSQL)) {
            List<String> names = accessor.showTablesLike(null, tableNameLike).stream()
                    .filter(name -> !StringUtils.endsWithIgnoreCase(name, OdcConstants.VALIDATE_DDL_TABLE_POSTFIX))
                    .collect(Collectors.toList());
            DatabaseAndTables databaseAndTables = new DatabaseAndTables(
                    ConnectionSessionUtil.getCurrentSchema(connectionSession), names);
            tables.add(databaseAndTables);
        } else {
            tables = dbTableService.generateDatabaseAndTables(accessor, tableNameLike, existedDatabases);
        }
        allResult.setTables(tables);
        allResult.setViews(generateDatabaseAndViews(accessor, tableNameLike, existedDatabases));
        return allResult;
    }

    private ViewExtensionPoint getDBViewExtensionPoint(@NonNull ConnectionSession session) {
        return SchemaPluginUtil.getViewExtension(session.getDialectType());
    }

    private List<DatabaseAndViews> generateDatabaseAndViews(@NotNull DBSchemaAccessor accessor,
            @NotNull String viewNameLike,
            @NonNull List<String> existedDatabases) {
        List<DBObjectIdentity> existedViewIdentities = accessor.listAllViews(viewNameLike);
        Map<String, List<String>> schema2ExistedViews = new HashMap<>();
        existedViewIdentities.forEach(item -> {
            schema2ExistedViews.computeIfAbsent(item.getSchemaName(), t -> new ArrayList<>()).add(item.getName());
        });
        return existedDatabases.stream()
                .map(schema -> new DatabaseAndViews(schema, Optional.ofNullable(schema2ExistedViews.get(schema))
                        .orElse(Collections.emptyList())))
                .collect(Collectors.toList());
    }

}
