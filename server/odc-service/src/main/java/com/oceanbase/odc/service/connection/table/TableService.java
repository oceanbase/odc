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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import javax.validation.Valid;

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
import com.oceanbase.odc.plugin.schema.api.TableExtensionPoint;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.table.model.QueryTableParams;
import com.oceanbase.odc.service.connection.table.model.Table;
import com.oceanbase.odc.service.db.schema.DBSchemaSyncService;
import com.oceanbase.odc.service.db.schema.syncer.object.DBTableSyncer;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.permission.DBResourcePermissionHelper;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
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
    private DatabaseService databaseService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private DBObjectRepository dbObjectRepository;

    @Autowired
    private DBTableSyncer dbTableSyncer;

    @Autowired
    private JdbcLockRegistry lockRegistry;

    @Autowired
    private DBSchemaSyncService dbSchemaSyncService;

    @Autowired
    private DBResourcePermissionHelper dbResourcePermissionHelper;

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("permission check inside")
    public List<Table> list(@NonNull @Valid QueryTableParams params) throws SQLException, InterruptedException {
        Database database = databaseService.detail(params.getDatabaseId());
        ConnectionConfig dataSource = database.getDataSource();
        OBConsoleDataSourceFactory factory = new OBConsoleDataSourceFactory(dataSource, true);
        try (SingleConnectionDataSource ds = (SingleConnectionDataSource) factory.getDataSource();
                Connection conn = ds.getConnection()) {
            TableExtensionPoint point = SchemaPluginUtil.getTableExtension(dataSource.getDialectType());
            Set<String> latestTableNames = point.list(conn, database.getName())
                    .stream().map(DBObjectIdentity::getName).collect(Collectors.toSet());
            if (authenticationFacade.currentUser().getOrganizationType() == OrganizationType.INDIVIDUAL) {
                return latestTableNames.stream().map(tableName -> {
                    Table table = new Table();
                    table.setName(tableName);
                    table.setAuthorizedPermissionTypes(new HashSet<>(DatabasePermissionType.all()));
                    return table;
                }).collect(Collectors.toList());
            }
            List<DBObjectEntity> tables =
                    dbObjectRepository.findByDatabaseIdAndType(params.getDatabaseId(), DBObjectType.TABLE);
            Set<String> existTableNames = tables.stream().map(DBObjectEntity::getName).collect(Collectors.toSet());
            if (latestTableNames.size() != existTableNames.size() || !existTableNames.containsAll(latestTableNames)) {
                syncDBTables(conn, database, dataSource.getDialectType());
                tables = dbObjectRepository.findByDatabaseIdAndType(params.getDatabaseId(), DBObjectType.TABLE);
            }
            return entitiesToModels(tables, database, params.getIncludePermittedAction());
        }
    }

    private void syncDBTables(Connection connection, Database database, DialectType dialectType)
            throws InterruptedException {
        Lock lock = lockRegistry
                .obtain(dbSchemaSyncService.getSyncDBObjectLockKey(database.getDataSource().getId(), database.getId()));
        if (!lock.tryLock(3, TimeUnit.SECONDS)) {
            throw new ConflictException(ErrorCodes.ResourceSynchronizing,
                    new Object[] {ResourceType.ODC_TABLE.getLocalizedMessage()}, "Can not acquire jdbc lock");
        }
        try {
            if (dbTableSyncer.supports(dialectType)) {
                dbTableSyncer.sync(connection, database, dialectType);
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
            if (includePermittedAction) {
                table.setAuthorizedPermissionTypes(id2Types.get(entity.getId()));
            }
            tables.add(table);
        }
        return tables;
    }

}
