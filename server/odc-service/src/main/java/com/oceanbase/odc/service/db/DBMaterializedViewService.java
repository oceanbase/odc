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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.plugin.schema.api.MViewExtensionPoint;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.table.TableService;
import com.oceanbase.odc.service.connection.table.model.QueryTableParams;
import com.oceanbase.odc.service.connection.table.model.Table;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.db.model.AllMVBaseTables;
import com.oceanbase.odc.service.db.model.DatabaseAndMVs;
import com.oceanbase.odc.service.db.model.DatabaseAndTables;
import com.oceanbase.odc.service.db.model.MViewRefreshReq;
import com.oceanbase.odc.service.plugin.SchemaPluginUtil;
import com.oceanbase.tools.dbbrowser.model.DBMViewRefreshParameter;
import com.oceanbase.tools.dbbrowser.model.DBMViewRefreshRecord;
import com.oceanbase.tools.dbbrowser.model.DBMViewRefreshRecordParam;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedView;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/4 14:30
 * @since: 4.3.4
 */
@Slf4j
@Service
@SkipAuthorize("inside connect session")
public class DBMaterializedViewService {

    @Autowired
    private TableService tableService;

    @Autowired
    private DatabaseService databaseService;

    public List<Table> list(@NonNull ConnectionSession connectionSession, @NonNull QueryTableParams params)
            throws SQLException, InterruptedException {
        Database database = databaseService.detail(params.getDatabaseId());
        List<Table> tables = new ArrayList<>();
        Set<String> latestTableNames = connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<List<DBObjectIdentity>>) con -> getDBMViewExtensionPoint(connectionSession)
                        .list(con, database.getName()))
                .stream().map(DBObjectIdentity::getName).collect(Collectors.toCollection(LinkedHashSet::new));
        ConnectionConfig connectionConfig = (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(
                connectionSession);
        tableService.generateListAndSyncDBTablesByTableType(params, database, connectionConfig, tables, null,
                DBObjectType.MATERIALIZED_VIEW, latestTableNames);
        return tables;
    }

    public AllMVBaseTables listAllBases(@NonNull ConnectionSession connectionSession,
            @NonNull String tableNameLike) {
        AllMVBaseTables allResult = new AllMVBaseTables();
        DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
        List<String> databases = accessor.showDatabases();
        List<DatabaseAndTables> tables = databases.stream().map(schema -> {
            List<String> tablesLike = accessor.showTablesLike(schema, tableNameLike).stream()
                    .filter(name -> !StringUtils.endsWith(name.toUpperCase(),
                            OdcConstants.VALIDATE_DDL_TABLE_POSTFIX))
                    .collect(Collectors.toList());
            return tablesLike.size() != 0 ? new DatabaseAndTables(schema, tablesLike)
                    : new DatabaseAndTables();
        }).filter(item -> item.getDatabaseName() != null)
                .sorted(Comparator.comparing(DatabaseAndTables::getDatabaseName)).collect(Collectors.toList());
        List<DBObjectIdentity> mvIdentities = accessor.listAllMViewsLike(tableNameLike);
        Map<String, List<String>> schema2mvs = new HashMap<>();
        mvIdentities.forEach(item -> {
            List<String> views = schema2mvs.computeIfAbsent(item.getSchemaName(), t -> new ArrayList<>());
            views.add(item.getName());
        });
        List<DatabaseAndMVs> mvs = new ArrayList<>();
        schema2mvs.forEach((schema, viewNames) -> {
            DatabaseAndMVs view = new DatabaseAndMVs();
            view.setDatabaseName(schema);
            view.setMvs(viewNames);
            mvs.add(view);
        });
        allResult.setTables(tables);
        allResult.setMvs(mvs);
        return allResult;
    }

    public String getCreateSql(@NonNull ConnectionSession session,
            @NonNull DBMaterializedView resource) {
        return session.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<String>) con -> getDBMViewExtensionPoint(session)
                        .generateCreateTemplate(resource));
    }

    public DBMaterializedView detail(@NonNull ConnectionSession connectionSession, @NotEmpty String schemaName,
            @NotEmpty String mViewName) {
        return connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<DBMaterializedView>) con -> getDBMViewExtensionPoint(connectionSession)
                        .getDetail(con, schemaName, mViewName));
    }

    public Boolean refresh(@NotNull ConnectionSession connectionSession, @NotNull MViewRefreshReq refreshReq) {
        DBMViewRefreshParameter syncDataParameter = refreshReq.convertToDBMViewRefreshParameter();
        return connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<Boolean>) con -> getDBMViewExtensionPoint(connectionSession)
                        .refresh(con, syncDataParameter));
    }

    public List<DBMViewRefreshRecord> listRefreshRecords(@NonNull ConnectionSession connectionSession,
            @NonNull DBMViewRefreshRecordParam param) {
        return connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY).execute(
                (ConnectionCallback<List<DBMViewRefreshRecord>>) con -> getDBMViewExtensionPoint(connectionSession)
                        .listRefreshRecords(con, param));
    }

    private MViewExtensionPoint getDBMViewExtensionPoint(@NonNull ConnectionSession session) {
        return SchemaPluginUtil.getMViewExtension(session.getDialectType());
    }

}
