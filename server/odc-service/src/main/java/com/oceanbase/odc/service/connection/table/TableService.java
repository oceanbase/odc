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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.metadb.connection.TableEntity;
import com.oceanbase.odc.metadb.connection.TableRepository;
import com.oceanbase.odc.plugin.schema.api.TableExtensionPoint;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.table.model.Table;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.permission.database.model.ExpirationStatusFilter;
import com.oceanbase.odc.service.permission.table.TablePermissionService;
import com.oceanbase.odc.service.permission.table.UserTablePermission;
import com.oceanbase.odc.service.permission.table.model.QueryTablePermissionParams;
import com.oceanbase.odc.service.plugin.SchemaPluginUtil;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBTable;

import lombok.extern.slf4j.Slf4j;

/**
 * ClassName: TableService Package: com.oceanbase.odc.service.connection.table Description:
 *
 * @Author: fenghao
 * @Create 2024/3/12 21:21
 * @Version 1.0
 */
@Service
@Validated
@Slf4j
@Authenticated
public class TableService {
    private final TableMapper tableMapper = TableMapper.INSTANCE;
    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private TablePermissionService tablePermissionService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @SkipAuthorize("permission check inside")
    public List<Table> getByDatabaseId(Long databaseId) {
        return tableRepository.findByDatabaseId(databaseId).stream()
                .map(TableMapper.INSTANCE::entityToModel)
                .collect(Collectors.toList());
    }

    @SkipAuthorize("permission check inside")
    public List<Table> getByDatabaseIds(Set<Long> databaseIds) {
        return tableRepository.findByDatabaseIdIn(databaseIds).stream()
                .map(TableMapper.INSTANCE::entityToModel)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("permission check inside")
    public Table save(Table table) {
        return TableMapper.INSTANCE
                .entityToModel(tableRepository.saveAndFlush(TableMapper.INSTANCE.modelToEntity(table)));
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("permission check inside")
    public List<Table> saveAll(List<Table> tables) {
        List<TableEntity> tableEntityList = tableRepository
                .saveAllAndFlush(tables.stream().map(TableMapper.INSTANCE::modelToEntity).collect(Collectors.toList()));
        return tableEntityList.stream().map(TableMapper.INSTANCE::entityToModel).collect(Collectors.toList());
    }

    @SkipAuthorize("permission check inside")
    public Table getByDatabaseIdAndName(Long databaseId, String tableName) {
        TableEntity tableEntity = tableRepository.findByDatabaseIdAndName(databaseId, tableName);
        if (tableEntity == null) {
            return null;
        } else {
            return TableMapper.INSTANCE.entityToModel(tableEntity);
        }
    }

    public List<Table> listTablesWithoutPage(@NotNull ConnectionSession connectionSession, Long databaseId) {
        List<Table> listTables = new ArrayList<>();
        Database databaseDetail = databaseService.detail(databaseId);
        List<DBTable> DBTableList = connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<List<DBObjectIdentity>>) con -> getTableExtensionPoint(connectionSession)
                        .list(con, databaseDetail.getName()))
                .stream().map(item -> {
                    DBTable table = new DBTable();
                    table.setName(item.getName());
                    table.setSchemaName(databaseDetail.getName());
                    return table;
                }).collect(Collectors.toList());
        QueryTablePermissionParams params = QueryTablePermissionParams.builder()
                .userId(authenticationFacade.currentUserId())
                .databaseId(databaseDetail.getId())
                .statuses(Arrays.asList(ExpirationStatusFilter.NOT_EXPIRED))
                .build();
        List<UserTablePermission> userTablePermissionList =
                tablePermissionService.listWithoutPage(databaseDetail.getProject().getId(), params);
        listTables = dbTablesToTables(DBTableList, userTablePermissionList);
        return listTables;
    }

    private TableExtensionPoint getTableExtensionPoint(@NotNull ConnectionSession connectionSession) {
        return SchemaPluginUtil.getTableExtension(connectionSession.getDialectType());
    }


    private Table dbTableToTable(DBTable dbTable) {
        Table table = new Table();
        table.setName(dbTable.getName());
        return table;
    }

    private List<Table> dbTablesToTables(List<DBTable> dbTables, List<UserTablePermission> userTablePermissionList) {
        List<Table> tableList = dbTables.stream().map(this::dbTableToTable).collect(Collectors.toList());
        Map<String, Set<DatabasePermissionType>> tableNameToPermissionTypes = new HashMap<>();
        for (UserTablePermission userTablePermission : userTablePermissionList) {
            if (tableNameToPermissionTypes.get(userTablePermission.getTableName()) == null) {
                tableNameToPermissionTypes.put(userTablePermission.getTableName(),
                        new HashSet<>(Arrays.asList(userTablePermission.getType())));;
            } else {
                Set<DatabasePermissionType> databasePermissionTypes = tableNameToPermissionTypes.get(
                        userTablePermission.getTableName());
                databasePermissionTypes.add(userTablePermission.getType());
                tableNameToPermissionTypes.put(userTablePermission.getTableName(), databasePermissionTypes);
            }
        }
        tableList = tableList.stream().map(
                item -> {
                    Set<DatabasePermissionType> databasePermissionTypes =
                            tableNameToPermissionTypes.get(item.getName());
                    if (databasePermissionTypes != null) {
                        item.setAuthorizedPermissionTypes(databasePermissionTypes);
                    }
                    return item;
                }).collect(Collectors.toList());


        return tableList;
    }

}
