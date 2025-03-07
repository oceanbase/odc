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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.metadb.dbobject.DBObjectRepository;
import com.oceanbase.odc.plugin.schema.api.MVExtensionPoint;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.table.TableService;
import com.oceanbase.odc.service.connection.table.model.QueryTableParams;
import com.oceanbase.odc.service.connection.table.model.Table;
import com.oceanbase.odc.service.db.model.DBViewResponse;
import com.oceanbase.odc.service.db.model.MVSyncDataReq;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.plugin.SchemaPluginUtil;
import com.oceanbase.odc.service.session.ConnectConsoleService;
import com.oceanbase.tools.dbbrowser.model.DBMVSyncDataParameter;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBView;

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
    private ConnectConsoleService consoleService;

    @Autowired
    private TableService tableService;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private DBObjectRepository dbObjectRepository;

    public List<Table> list(ConnectionSession connectionSession, QueryTableParams params)
            throws SQLException, InterruptedException {
        Database database = databaseService.detail(params.getDatabaseId());
        List<Table> tables = new ArrayList<>();
        Set<String> latestTableNames = connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<List<DBObjectIdentity>>) con -> getDBMVExtensionPoint(connectionSession)
                        .list(con, database.getName()))
                .stream().map(DBObjectIdentity::getName).collect(Collectors.toCollection(LinkedHashSet::new));
        ConnectionConfig connectionConfig = (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(
                connectionSession);
        tableService.generateListAndSyncDBTablesByTableType(params, database, connectionConfig, tables, null,
                DBObjectType.MATERIALIZED_VIEW, latestTableNames);
        return tables;
    }

    public DBViewResponse detail(ConnectionSession connectionSession, String schemaName, String viewName) {
        return new DBViewResponse(connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<DBView>) con -> getDBMVExtensionPoint(connectionSession)
                        .getDetail(con, schemaName, viewName)));
    }

    public Boolean syncData(@NotNull ConnectionSession connectionSession, @NotNull MVSyncDataReq mvSyncDataReq) {
        DBMVSyncDataParameter dbmvSyncDataParameter = mvSyncDataReq.convertToDBMVSyncDataParameter();
        return connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<Boolean>) con -> getDBMVExtensionPoint(connectionSession)
                        .syncMVData(con, dbmvSyncDataParameter));
    }

    private MVExtensionPoint getDBMVExtensionPoint(@NonNull ConnectionSession session) {
        return SchemaPluginUtil.getMVExtension(session.getDialectType());
    }

}
