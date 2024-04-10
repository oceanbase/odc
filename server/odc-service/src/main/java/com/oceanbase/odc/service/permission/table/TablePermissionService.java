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
package com.oceanbase.odc.service.permission.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.TimeUtils;
import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.AuthorizationType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.PermissionType;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.iam.PermissionRepository;
import com.oceanbase.odc.metadb.iam.UserPermissionEntity;
import com.oceanbase.odc.metadb.iam.UserPermissionRepository;
import com.oceanbase.odc.metadb.iam.UserTablePermissionEntity;
import com.oceanbase.odc.metadb.iam.UserTablePermissionRepository;
import com.oceanbase.odc.metadb.iam.UserTablePermissionSpec;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.collaboration.project.model.Project;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.database.model.DatabaseSyncStatus;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.table.TableService;
import com.oceanbase.odc.service.connection.table.model.Table;
import com.oceanbase.odc.service.iam.PermissionService;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.permission.database.DatabasePermissionHelper;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.permission.database.model.ExpirationStatusFilter;
import com.oceanbase.odc.service.permission.table.model.CreateTablePermissionReq;
import com.oceanbase.odc.service.permission.table.model.CreateTablePermissionReq.TablePermission;
import com.oceanbase.odc.service.permission.table.model.QueryTablePermissionParams;
import com.oceanbase.odc.service.session.model.UnauthorizedResource;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;

import lombok.extern.slf4j.Slf4j;

/**
 * ClassName: TablePermissionService Package: com.oceanbase.odc.service.permission.table
 * Description:
 *
 * @Author: fenghao
 * @Create 2024/3/11 20:49
 * @Version 1.0
 */
@Service
@Validated
@Authenticated
@Slf4j
public class TablePermissionService {
    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectPermissionValidator projectPermissionValidator;

    @Autowired
    @Lazy
    private DatabaseService databaseService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private UserTablePermissionRepository userTablePermissionRepository;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    @Lazy
    private TableService tableService;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private DatabasePermissionHelper databasePermissionHelper;

    @Value("${odc.iam.permission.expired-retention-time-seconds:7776000}")
    private long expiredRetentionTimeSeconds;

    private static final UserTablePermissionMapper mapper = UserTablePermissionMapper.INSTANCE;
    private static final int EXPIRING_DAYS = 7;

    private static final Set<String> ORACLE_DATA_DICTIONARY = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    private static final Set<String> MYSQL_DATA_DICTIONARY = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("permission check inside")
    public Page<UserTablePermission> list(@NotNull Long projectId, @NotNull QueryTablePermissionParams params,
            Pageable pageable) {
        if (params.getUserId() == null || params.getUserId() != authenticationFacade.currentUserId()) {
            projectPermissionValidator.checkProjectRole(projectId,
                    Arrays.asList(ResourceRoleName.OWNER, ResourceRoleName.DBA));
        } else {
            projectPermissionValidator.checkProjectRole(projectId, ResourceRoleName.all());
        }
        Date expiredTime = new Date(System.currentTimeMillis() - expiredRetentionTimeSeconds * 1000);
        permissionService.deleteExpiredPermission(expiredTime);
        Date expireTimeThreshold = TimeUtils.getStartOfDay(new Date());
        Specification<UserTablePermissionEntity> spec = Specification
                .where(UserTablePermissionSpec.projectIdEqual(projectId))
                .and(UserTablePermissionSpec.organizationIdEqual(authenticationFacade.currentOrganizationId()))
                .and(UserTablePermissionSpec.userIdEqual(params.getUserId()))
                .and(UserTablePermissionSpec.ticketIdEqual(params.getTicketId()))
                .and(UserTablePermissionSpec.databaseNameLike(params.getFuzzyDatabaseName()))
                .and(UserTablePermissionSpec.dataSourceNameLike(params.getFuzzyDataSourceName()))
                .and(UserTablePermissionSpec.typeIn(params.getTypes()))
                .and(UserTablePermissionSpec.authorizationTypeEqual(params.getAuthorizationType()))
                .and(UserTablePermissionSpec.filterByExpirationStatus(params.getStatuses(), expireTimeThreshold));
        return userTablePermissionRepository.findAll(spec, pageable).map(
                e -> {
                    UserTablePermission permission = mapper.entityToModel(e);
                    Date expireTime = permission.getExpireTime();
                    if (expireTime.before(expireTimeThreshold)) {
                        permission.setStatus(ExpirationStatusFilter.EXPIRED);
                    } else if (expireTime.before(DateUtils.addDays(expireTimeThreshold, EXPIRING_DAYS))) {
                        permission.setStatus(ExpirationStatusFilter.EXPIRING);
                    } else {
                        permission.setStatus(ExpirationStatusFilter.NOT_EXPIRED);
                    }
                    return permission;
                });
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("permission check inside")
    public List<UserTablePermission> listWithoutPage(@NotNull Long projectId,
            @NotNull QueryTablePermissionParams params) {
        if (params.getUserId() == null || params.getUserId() != authenticationFacade.currentUserId()) {
            projectPermissionValidator.checkProjectRole(projectId,
                    Arrays.asList(ResourceRoleName.OWNER, ResourceRoleName.DBA));
        } else {
            projectPermissionValidator.checkProjectRole(projectId, ResourceRoleName.all());
        }
        Date expiredTime = new Date(System.currentTimeMillis() - expiredRetentionTimeSeconds * 1000);
        permissionService.deleteExpiredPermission(expiredTime);
        Date expireTimeThreshold = TimeUtils.getStartOfDay(new Date());
        Specification<UserTablePermissionEntity> spec = Specification
                .where(UserTablePermissionSpec.projectIdEqual(projectId))
                .and(UserTablePermissionSpec.organizationIdEqual(authenticationFacade.currentOrganizationId()))
                .and(UserTablePermissionSpec.userIdEqual(params.getUserId()))
                .and(UserTablePermissionSpec.ticketIdEqual(params.getTicketId()))
                .and(UserTablePermissionSpec.databaseNameLike(params.getFuzzyDatabaseName()))
                .and(UserTablePermissionSpec.dataSourceNameLike(params.getFuzzyDataSourceName()))
                .and(UserTablePermissionSpec.typeIn(params.getTypes()))
                .and(UserTablePermissionSpec.authorizationTypeEqual(params.getAuthorizationType()))
                .and(UserTablePermissionSpec.filterByExpirationStatus(params.getStatuses(), expireTimeThreshold));
        return userTablePermissionRepository.findAll(spec).stream().map(
                e -> {
                    UserTablePermission permission = mapper.entityToModel(e);
                    Date expireTime = permission.getExpireTime();
                    if (expireTime.before(expireTimeThreshold)) {
                        permission.setStatus(ExpirationStatusFilter.EXPIRED);
                    } else if (expireTime.before(DateUtils.addDays(expireTimeThreshold, EXPIRING_DAYS))) {
                        permission.setStatus(ExpirationStatusFilter.EXPIRING);
                    } else {
                        permission.setStatus(ExpirationStatusFilter.NOT_EXPIRED);
                    }
                    return permission;
                }).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("odc internal usage")
    public List<UserTablePermission> listWithoutPageByDataSourceId(@NotNull Long dataSourceId,
            @NotNull QueryTablePermissionParams params) {
        Date expiredTime = new Date(System.currentTimeMillis() - expiredRetentionTimeSeconds * 1000);
        permissionService.deleteExpiredPermission(expiredTime);
        Date expireTimeThreshold = TimeUtils.getStartOfDay(new Date());
        Specification<UserTablePermissionEntity> spec = Specification
                .where(UserTablePermissionSpec.dataSourceId(dataSourceId))
                .and(UserTablePermissionSpec.organizationIdEqual(authenticationFacade.currentOrganizationId()))
                .and(UserTablePermissionSpec.userIdEqual(params.getUserId()))
                .and(UserTablePermissionSpec.filterByExpirationStatus(params.getStatuses(), expireTimeThreshold));
        return userTablePermissionRepository.findAll(spec).stream().map(
                e -> {
                    UserTablePermission permission = mapper.entityToModel(e);
                    Date expireTime = permission.getExpireTime();
                    if (expireTime.before(expireTimeThreshold)) {
                        permission.setStatus(ExpirationStatusFilter.EXPIRED);
                    } else if (expireTime.before(DateUtils.addDays(expireTimeThreshold, EXPIRING_DAYS))) {
                        permission.setStatus(ExpirationStatusFilter.EXPIRING);
                    } else {
                        permission.setStatus(ExpirationStatusFilter.NOT_EXPIRED);
                    }
                    return permission;
                }).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER", "DBA"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public List<UserTablePermission> batchCreate(@NotNull Long projectId,
            @NotNull @Valid CreateTablePermissionReq req) {
        Set<Long> projectIds = projectService.getMemberProjectIds(req.getUserId());
        if (!projectIds.contains(projectId)) {
            throw new AccessDeniedException();
        }
        List<TablePermission> tables = req.getTables();
        // 检查要授权的数据库是否存在
        Map<Long, Database> id2Database = databaseService
                .listDatabasesByIds(tables.stream().map(t -> t.getDatabaseId()).collect(
                        Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(Database::getId, d -> d, (d1, d2) -> d1));
        for (Long databaseId : tables.stream().map(t -> t.getDatabaseId()).collect(
                Collectors.toList())) {
            if (!id2Database.containsKey(databaseId)) {
                throw new NotFoundException(ResourceType.ODC_DATABASE, "id", databaseId);
            }
            Project project = id2Database.get(databaseId).getProject();
            if (project == null || !Objects.equals(project.getId(), projectId)) {
                throw new AccessDeniedException();
            }
        }

        // 检查要授权的表是否存在,不存在就写入connect_table表

        // connect_table已有的表
        List<Table> tableList = new ArrayList<>();
        // connect_table新增的表
        List<Table> newTableList = new ArrayList<>();
        tables.forEach(t -> {
            List<String> tableNames = t.getTableNames();
            tableNames.forEach(tableName -> {
                Table table1 = new Table();
                table1.setDatabaseId(t.getDatabaseId());
                Table table = tableService.getByDatabaseIdAndName(t.getDatabaseId(), tableName);
                if (table == null) {
                    table1.setName(tableName);
                    table1.setSyncStatus(DatabaseSyncStatus.SUCCEEDED);
                    newTableList.add(table1);
                } else {
                    tableList.add(table);
                }
            });
        });
        if (newTableList.size() > 0) {
            List<Table> finalNewTableList = tableService.saveAll(newTableList);
            tableList.addAll(finalNewTableList);
        }

        // 写入权限
        List<PermissionEntity> permissionEntities = new ArrayList<>();
        Long organizationId = authenticationFacade.currentOrganizationId();
        Long creatorId = authenticationFacade.currentUserId();
        Date expireTime = req.getExpireTime() == null ? TimeUtils.getMySQLMaxDatetime()
                : TimeUtils.getEndOfDay(req.getExpireTime());
        for (Table table : tableList) {
            for (DatabasePermissionType permissionType : req.getTypes()) {
                PermissionEntity entity = new PermissionEntity();
                entity.setAction(permissionType.getAction());
                entity.setResourceIdentifier(ResourceType.ODC_DATABASE.name() + ":" + table.getDatabaseId() + "/"
                        + ResourceType.ODC_TABLE.name() + ":" + table.getId());
                entity.setType(PermissionType.PUBLIC_RESOURCE);
                entity.setCreatorId(creatorId);
                entity.setOrganizationId(organizationId);
                entity.setBuiltIn(false);
                entity.setExpireTime(expireTime);
                entity.setAuthorizationType(AuthorizationType.USER_AUTHORIZATION);
                entity.setResourceType(ResourceType.ODC_TABLE);
                entity.setResourceId(table.getId());
                permissionEntities.add(entity);
            }
        }
        List<PermissionEntity> permissionEntitieList = permissionRepository.batchCreate(permissionEntities);
        List<UserPermissionEntity> userPermissionEntities = permissionEntities2UserPermissionEntities(
                permissionEntitieList, req, creatorId, organizationId);
        userPermissionRepository.batchCreate(userPermissionEntities);
        return permissionEntitieList.stream().map(PermissionEntity::getId).map(UserTablePermission::from)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("odc internal usage")
    public List<UserTablePermission> taskBatchCreate(@NotNull Long projectId,
            @NotNull @Valid CreateTablePermissionReq req) {
        Set<Long> projectIds = projectService.getMemberProjectIds(req.getUserId());
        if (!projectIds.contains(projectId)) {
            throw new AccessDeniedException();
        }
        List<TablePermission> tables = req.getTables();
        // 检查要授权的表是否存在,不存在就写入connect_table表
        // connect_table已有的表
        List<Table> tableList = new ArrayList<>();
        // connect_table新增的表
        List<Table> newTableList = new ArrayList<>();
        tables.forEach(t -> {
            List<String> tableNames = t.getTableNames();
            tableNames.forEach(tableName -> {
                Table table1 = new Table();
                table1.setDatabaseId(t.getDatabaseId());
                Table table = tableService.getByDatabaseIdAndName(t.getDatabaseId(), tableName);
                if (table == null) {
                    table1.setName(tableName);
                    table1.setSyncStatus(DatabaseSyncStatus.SUCCEEDED);
                    newTableList.add(table1);
                } else {
                    tableList.add(table);
                }
            });
        });
        if (newTableList.size() > 0) {
            List<Table> finalNewTableList = tableService.saveAll(newTableList);
            tableList.addAll(finalNewTableList);
        }

        // 写入权限
        List<PermissionEntity> permissionEntities = new ArrayList<>();
        Date expireTime = req.getExpireTime() == null ? TimeUtils.getMySQLMaxDatetime()
                : TimeUtils.getEndOfDay(req.getExpireTime());
        for (Table table : tableList) {
            for (DatabasePermissionType permissionType : req.getTypes()) {
                PermissionEntity entity = new PermissionEntity();
                entity.setAction(permissionType.getAction());
                entity.setResourceIdentifier(ResourceType.ODC_DATABASE.name() + ":" + table.getDatabaseId() + "/"
                        + ResourceType.ODC_TABLE.name() + ":" + table.getId());
                entity.setType(PermissionType.PUBLIC_RESOURCE);
                entity.setCreatorId(req.getCreatorId());
                entity.setOrganizationId(req.getOrganizationId());
                entity.setBuiltIn(false);
                entity.setExpireTime(expireTime);
                entity.setAuthorizationType(AuthorizationType.TICKET_APPLICATION);
                entity.setResourceType(ResourceType.ODC_TABLE);
                entity.setResourceId(table.getId());
                entity.setTicketId(req.getTicketId());
                permissionEntities.add(entity);
            }
        }
        List<PermissionEntity> permissionEntitieList = permissionRepository.batchCreate(permissionEntities);
        List<UserPermissionEntity> userPermissionEntities = permissionEntities2UserPermissionEntities(
                permissionEntitieList, req, req.getCreatorId(), req.getOrganizationId());
        userPermissionRepository.batchCreate(userPermissionEntities);
        return permissionEntitieList.stream().map(PermissionEntity::getId).map(UserTablePermission::from)
                .collect(Collectors.toList());
    }

    private static List<UserPermissionEntity> permissionEntities2UserPermissionEntities(
            List<PermissionEntity> permissionEntityList,
            CreateTablePermissionReq req, Long req1, Long req2) {
        List<UserPermissionEntity> userPermissionEntities = new ArrayList<>();
        for (PermissionEntity permissionEntity : permissionEntityList) {
            UserPermissionEntity userPermissionEntity = new UserPermissionEntity();
            userPermissionEntity.setUserId(req.getUserId());
            userPermissionEntity.setPermissionId(permissionEntity.getId());
            userPermissionEntity.setCreatorId(req1);
            userPermissionEntity.setOrganizationId(req2);
            userPermissionEntities.add(userPermissionEntity);
        }
        return userPermissionEntities;
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER", "DBA"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public List<UserTablePermission> batchRevoke(@NotNull Long projectId, @NotEmpty List<Long> ids) {
        List<UserTablePermissionEntity> entities =
                userTablePermissionRepository.findByProjectIdAndIdIn(projectId, ids);
        List<Long> permissionIds =
                entities.stream().map(UserTablePermissionEntity::getId).collect(Collectors.toList());
        permissionRepository.deleteByIds(permissionIds);
        userPermissionRepository.deleteByPermissionIds(permissionIds);
        return permissionIds.stream().map(UserTablePermission::from).collect(Collectors.toList());
    }


    // 对表和库的权限进行过滤
    @SkipAuthorize("odc internal usage")
    public List<UnauthorizedResource> filterUnauthorizedTables(
            Map<RelationFactor, Set<DatabasePermissionType>> tableName2PermissionTypes, @NotNull Long dataSourceId,
            boolean ignoreDataDictionary) {
        if (tableName2PermissionTypes == null || tableName2PermissionTypes.isEmpty()) {
            return Collections.emptyList();
        }
        ConnectionConfig dataSource = connectionService.getBasicWithoutPermissionCheck(dataSourceId);
        // databases 是当前连接的数据库信息
        List<Database> databases = databaseService.listDatabasesByConnectionIds(Collections.singleton(dataSourceId));
        databases.forEach(d -> d.getDataSource().setName(dataSource.getName()));
        // name2Database是一个数据库名字与databases的映射
        Map<String, Database> name2Database = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        databases.forEach(d -> name2Database.put(d.getName(), d));
        // id2Types是数据库id与当前用户拥有的权限关系,已经处理了project权限
        Map<Long, Set<DatabasePermissionType>> id2Types = databasePermissionHelper
                .getPermissions(databases.stream().map(Database::getId).collect(Collectors.toList()));
        // 当前用户与表的权限关系
        QueryTablePermissionParams queryTablePermissionParams = QueryTablePermissionParams.builder()
                .userId(authenticationFacade.currentUserId()).dataSourceId(dataSourceId).build();
        List<UserTablePermission> userTablePermissions = listWithoutPageByDataSourceId(dataSource.getId(),
                queryTablePermissionParams);
        List<UnauthorizedResource> unauthorizedResources = new ArrayList<>();
        Set<Long> involvedProjectIds = projectService.getMemberProjectIds(authenticationFacade.currentUserId());
        for (RelationFactor relationFactor : tableName2PermissionTypes.keySet()) {
            String schemaName = relationFactor.getSchema();
            String tableName = StringUtils.unquoteMySqlIdentifier(relationFactor.getRelation());
            Set<DatabasePermissionType> needs = tableName2PermissionTypes.get(relationFactor);
            if (CollectionUtils.isEmpty(needs)) {
                continue;
            }
            if (name2Database.containsKey(schemaName)) {
                Database database = name2Database.get(schemaName);
                boolean applicable =
                        database.getProject() != null && involvedProjectIds.contains(database.getProject().getId());
                // 用户拥有当前库的权限
                Set<DatabasePermissionType> authorized = id2Types.get(database.getId());
                // 先进行库级别鉴权
                if (CollectionUtils.isNotEmpty(authorized)) {
                    needs.removeIf(p -> authorized.contains(p));
                }

                if (CollectionUtils.isNotEmpty(needs)) {
                    // 如果没有库的权限，需要进行表级别鉴权
                    userTablePermissions.forEach(item -> {
                        // 已经限定在一个数据源内，所以对比库名和表名就可以了
                        if (item.getDatabaseName().equals(schemaName) && item.getTableName().equals(tableName)) {
                            needs.removeIf(p -> item.getType() == p);
                        }
                    });
                }
                if (CollectionUtils.isNotEmpty(needs)) {
                    UnauthorizedResource unauthorizedResource = UnauthorizedResource.from(database, needs, applicable);
                    unauthorizedResource.setTableName(tableName);
                    unauthorizedResources.add(unauthorizedResource);
                }
            } else {
                Database unknownDatabase = new Database();
                unknownDatabase.setName(schemaName);
                unknownDatabase.setDataSource(dataSource);
                unauthorizedResources.add(UnauthorizedResource.from(unknownDatabase, needs, false));
            }
        }
        if (ignoreDataDictionary) {
            DialectType dialectType = dataSource.getDialectType();
            if (dialectType != null) {
                if (dialectType.isOracle()) {
                    unauthorizedResources =
                            unauthorizedResources.stream().filter(d -> !ORACLE_DATA_DICTIONARY.contains(d.getName()))
                                    .collect(Collectors.toList());
                } else if (dialectType.isMysql()) {
                    unauthorizedResources = unauthorizedResources.stream()
                            .filter(d -> !MYSQL_DATA_DICTIONARY.contains(d.getName()))
                            .collect(Collectors.toList());
                }
            }
        }
        if (ignoreDataDictionary) {
            DialectType dialectType = dataSource.getDialectType();
            if (dialectType != null) {
                if (dialectType.isOracle()) {
                    unauthorizedResources =
                            unauthorizedResources.stream().filter(d -> !ORACLE_DATA_DICTIONARY.contains(d.getName()))
                                    .collect(Collectors.toList());
                } else if (dialectType.isMysql()) {
                    unauthorizedResources = unauthorizedResources.stream()
                            .filter(d -> !MYSQL_DATA_DICTIONARY.contains(d.getName()))
                            .collect(Collectors.toList());
                }
            }
        }
        return unauthorizedResources;
    }

    @SkipAuthorize("odc internal usage")
    public List<UnauthorizedResource> filterUnauthorizedTablesByTableNames(
            Map<String, Set<DatabasePermissionType>> tableNames, @NotNull Long databaseId) {
        if (tableNames == null || tableNames.isEmpty()) {
            return Collections.emptyList();
        }
        Database databaseDetail = databaseService.detail(databaseId);
        List<Database> databases = Arrays.asList(databaseDetail);

        // name2Database是一个数据库名字与databases的映射
        Map<String, Database> name2Database = new HashMap<>();
        databases.forEach(d -> name2Database.put(d.getName(), d));

        // id2Types是数据库id与当前用户拥有的权限关系,已经处理了project权限
        Map<Long, Set<DatabasePermissionType>> id2Types = databasePermissionHelper
                .getPermissions(Collections.singleton(databaseId));

        // 当前用户与表的权限关系
        QueryTablePermissionParams queryTablePermissionParams = QueryTablePermissionParams.builder()
                .userId(authenticationFacade.currentUserId()).dataSourceId(databaseDetail.getDataSource().getId())
                .build();
        List<UserTablePermission> userTablePermissions =
                listWithoutPageByDataSourceId(databaseDetail.getDataSource().getId(),
                        queryTablePermissionParams);
        List<UnauthorizedResource> unauthorizedResources = new ArrayList<>();

        Set<Long> involvedProjectIds = projectService.getMemberProjectIds(authenticationFacade.currentUserId());
        boolean applicable =
                databaseDetail.getProject() != null && involvedProjectIds.contains(databaseDetail.getProject().getId());
        for (String tableName : tableNames.keySet()) {
            Set<DatabasePermissionType> needs = tableNames.get(tableName);
            if (CollectionUtils.isEmpty(needs)) {
                continue;
            }
            if (name2Database.containsKey(databaseDetail.getName())) {
                // 用户拥有当前库的权限
                Set<DatabasePermissionType> authorized = id2Types.get(databaseDetail.getId());
                // 先进行库级别鉴权
                if (CollectionUtils.isNotEmpty(authorized)) {
                    needs.removeIf(p -> authorized.contains(p));
                }

                if (CollectionUtils.isNotEmpty(needs)) {
                    // 如果没有库的权限，需要进行表级别鉴权
                    userTablePermissions.forEach(item -> {
                        // 已经限定在一个数据源内，所以对比库名和表名就可以了
                        if (item.getDatabaseName().equals(databaseDetail.getName())
                                && item.getTableName().equals(tableName)) {
                            needs.removeIf(p -> item.getType() == p);
                        }
                    });
                }
                if (CollectionUtils.isNotEmpty(needs)) {
                    UnauthorizedResource unauthorizedResource =
                            UnauthorizedResource.from(databaseDetail, needs, applicable);
                    unauthorizedResource.setTableName(tableName);
                    unauthorizedResources.add(unauthorizedResource);
                }
            } else {
                Database unknownDatabase = new Database();
                unknownDatabase.setName(databaseDetail.getName());
                unknownDatabase.setDataSource(databaseDetail.getDataSource());
                unauthorizedResources.add(UnauthorizedResource.from(unknownDatabase, needs, false));
            }
        }
        return unauthorizedResources;
    }

}
