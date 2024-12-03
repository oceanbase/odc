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
package com.oceanbase.odc.service.connection.table;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.datasource.SingleConnectionDataSource;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.ConflictException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.dbobject.DBObjectEntity;
import com.oceanbase.odc.metadb.dbobject.DBObjectRepository;
import com.oceanbase.odc.plugin.connect.api.InformationExtensionPoint;
import com.oceanbase.odc.plugin.schema.api.TableExtensionPoint;
import com.oceanbase.odc.plugin.schema.api.ViewExtensionPoint;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.table.model.QueryTableParams;
import com.oceanbase.odc.service.connection.table.model.Table;
import com.oceanbase.odc.service.db.schema.DBSchemaSyncService;
import com.oceanbase.odc.service.db.schema.syncer.DBSchemaSyncer;
import com.oceanbase.odc.service.db.schema.syncer.object.DBExternalTableSyncer;
import com.oceanbase.odc.service.db.schema.syncer.object.DBTableSyncer;
import com.oceanbase.odc.service.db.schema.syncer.object.DBViewSyncer;
import com.oceanbase.odc.service.feature.VersionDiffConfigService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.permission.DBResourcePermissionHelper;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;
import com.oceanbase.odc.service.plugin.SchemaPluginUtil;
import com.oceanbase.odc.service.session.factory.OBConsoleDataSourceFactory;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.NonNull;

/**
 *
 * @Author: fenghao
 * @Create 2024/3/12 21:21
 * @Version 1.0
 */
@Service
@Validated
public class TableService {

    @Autowired
    private VersionDiffConfigService versionDiffConfigService;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private DBObjectRepository dbObjectRepository;

    @Autowired
    private DBTableSyncer dbTableSyncer;

    @Autowired
    private DBExternalTableSyncer dbExternalTableSyncer;

    @Autowired
    private DBViewSyncer dbViewSyncer;

    @Autowired
    private JdbcLockRegistry lockRegistry;

    @Autowired
    private DBSchemaSyncService dbSchemaSyncService;

    @Autowired
    private DBResourcePermissionHelper dbResourcePermissionHelper;

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("permission check inside")
    public List<Table> list(@NonNull @Valid QueryTableParams params) throws SQLException, InterruptedException {
        List<DBObjectType> types = params.getTypes();
        if (CollectionUtils.isEmpty(types)) {
            return Collections.emptyList();
        }
        Database database = databaseService.detail(params.getDatabaseId());
        ConnectionConfig dataSource = database.getDataSource();
        OBConsoleDataSourceFactory factory = new OBConsoleDataSourceFactory(dataSource, true);
        List<Table> tables = new ArrayList<>();
        try (SingleConnectionDataSource ds = (SingleConnectionDataSource) factory.getDataSource();
                Connection conn = ds.getConnection()) {
            TableExtensionPoint tableExtension = SchemaPluginUtil.getTableExtension(dataSource.getDialectType());
            if (tableExtension == null) {
                throw new UnsupportedOperationException("the dialect " + dataSource.getDialectType()
                        + " doesn't support the database object type " + DBObjectType.TABLE);
            }
            if (types.contains(DBObjectType.TABLE)) {
                Set<String> latestTableNames = tableExtension.list(conn, database.getName(), DBObjectType.TABLE)
                        .stream().map(DBObjectIdentity::getName).collect(Collectors.toCollection(LinkedHashSet::new));
                generateListAndSyncDBTablesByTableType(params, database, dataSource, tables, conn, DBObjectType.TABLE,
                        latestTableNames);
            }
            if (types.contains(DBObjectType.EXTERNAL_TABLE)) {
                InformationExtensionPoint point =
                        ConnectionPluginUtil.getInformationExtension(dataSource.getDialectType());
                String databaseProductVersion = point.getDBVersion(conn);
                if (versionDiffConfigService.isExternalTableSupported(dataSource.getDialectType(),
                        databaseProductVersion)) {
                    Set<String> latestExternalTableNames =
                            tableExtension.list(conn, database.getName(), DBObjectType.EXTERNAL_TABLE)
                                    .stream().map(DBObjectIdentity::getName)
                                    .collect(Collectors.toCollection(LinkedHashSet::new));
                    generateListAndSyncDBTablesByTableType(params, database, dataSource, tables, conn,
                            DBObjectType.EXTERNAL_TABLE, latestExternalTableNames);
                }
            }
            if (types.contains(DBObjectType.VIEW)) {
                ViewExtensionPoint viewExtension = SchemaPluginUtil.getViewExtension(dataSource.getDialectType());
                if (viewExtension != null) {
                    Set<String> latestViewNames = viewExtension.list(conn, database.getName())
                            .stream().map(DBObjectIdentity::getName)
                            .collect(Collectors.toCollection(LinkedHashSet::new));
                    generateListAndSyncDBTablesByTableType(params, database, dataSource, tables, conn,
                            DBObjectType.VIEW,
                            latestViewNames);
                }
            }
        }
        return tables;
    }

    private void generateListAndSyncDBTablesByTableType(QueryTableParams params, Database database,
            ConnectionConfig dataSource, List<Table> tables,
            Connection conn, DBObjectType tableType, Set<String> latestTableNames) throws InterruptedException {
        if (authenticationFacade.currentUser().getOrganizationType() == OrganizationType.INDIVIDUAL) {
            tables.addAll(latestTableNames.stream().map(tableName -> {
                Table table = new Table();
                table.setName(tableName);
                table.setAuthorizedPermissionTypes(new HashSet<>(DatabasePermissionType.all()));
                table.setType(tableType);
                return table;
            }).collect(Collectors.toList()));
        } else {
            List<DBObjectEntity> existTables =
                    dbObjectRepository.findByDatabaseIdAndTypeOrderByNameAsc(params.getDatabaseId(),
                            tableType);
            Set<String> existTableNames =
                    existTables.stream().map(DBObjectEntity::getName).collect(Collectors.toSet());
            if (latestTableNames.size() != existTableNames.size()
                    || !existTableNames.containsAll(latestTableNames)) {
                syncDBTables(conn, database, dataSource.getDialectType(), getSyncerByTableType(tableType));
                existTables =
                        dbObjectRepository.findByDatabaseIdAndTypeOrderByNameAsc(params.getDatabaseId(),
                                tableType);
            }
            tables.addAll(entitiesToModels(existTables, database, params.getIncludePermittedAction()));
        }
    }

    private DBSchemaSyncer getSyncerByTableType(@NotNull DBObjectType tableType) {
        switch (tableType) {
            case TABLE:
                return dbTableSyncer;
            case EXTERNAL_TABLE:
                return dbExternalTableSyncer;
            case VIEW:
                return dbViewSyncer;
            default:
                throw new IllegalArgumentException("Unsupported table type: " + tableType);
        }
    }

    // sync normal table
    public void syncDBTables(@NotNull Connection connection, @NotNull Database database,
            @NotNull DialectType dialectType) throws InterruptedException {
        syncDBTables(connection, database, dialectType, dbTableSyncer);
    }

    private void syncDBTables(@NotNull Connection connection, @NotNull Database database,
            @NotNull DialectType dialectType, @NotNull DBSchemaSyncer syncer) throws InterruptedException {
        Lock lock = lockRegistry
                .obtain(dbSchemaSyncService.getSyncDBObjectLockKey(database.getDataSource().getId(), database.getId()));
        if (!lock.tryLock(3, TimeUnit.SECONDS)) {
            throw new ConflictException(ErrorCodes.ResourceSynchronizing,
                    new Object[] {ResourceType.ODC_TABLE.getLocalizedMessage()}, "Can not acquire jdbc lock");
        }
        try {
            if (syncer.supports(dialectType, connection)) {
                syncer.sync(connection, database, dialectType);
            } else {
                throw new UnsupportedException("Unsupported dialect type: " + dialectType);
            }
        } finally {
            lock.unlock();
        }
    }

    private List<Table> entitiesToModels(Collection<DBObjectEntity> entities, Database database,
            boolean includePermittedAction) {
        List<Table> tables = new ArrayList<>();
        if (CollectionUtils.isEmpty(entities)) {
            return tables;
        }
        Map<Long, Set<DatabasePermissionType>> id2Types = dbResourcePermissionHelper
                .getTablePermissions(entities.stream().map(DBObjectEntity::getId).collect(Collectors.toSet()));
        for (DBObjectEntity entity : entities) {
            Table table = new Table();
            table.setId(entity.getId());
            table.setName(entity.getName());
            table.setDatabase(database);
            table.setCreateTime(entity.getCreateTime());
            table.setUpdateTime(entity.getUpdateTime());
            table.setOrganizationId(entity.getOrganizationId());
            table.setType(entity.getType());
            if (includePermittedAction) {
                table.setAuthorizedPermissionTypes(id2Types.get(entity.getId()));
            }
            tables.add(table);
        }
        return tables;
    }

}
