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
package com.oceanbase.odc.service.permission;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.dbobject.DBObjectEntity;
import com.oceanbase.odc.metadb.dbobject.DBObjectRepository;
import com.oceanbase.odc.metadb.iam.UserDatabasePermissionEntity;
import com.oceanbase.odc.metadb.iam.UserDatabasePermissionRepository;
import com.oceanbase.odc.metadb.iam.UserTablePermissionEntity;
import com.oceanbase.odc.metadb.iam.UserTablePermissionRepository;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.connection.database.model.DBResource;
import com.oceanbase.odc.service.connection.database.model.UnauthorizedDBResource;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

/**
 * @author gaoda.xy
 * @date 2024/4/28 16:02
 */
@Component
@SkipAuthorize("odc internal usage")
public class DBResourcePermissionHelper {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private UserDatabasePermissionRepository userDatabasePermissionRepository;

    @Autowired
    private UserTablePermissionRepository userTablePermissionRepository;

    @Autowired
    private DBObjectRepository dbObjectRepository;

    private static final Set<String> ORACLE_DATA_DICTIONARY = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    private static final Set<String> MYSQL_DATA_DICTIONARY = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    static {
        ORACLE_DATA_DICTIONARY.add("SYS");
        MYSQL_DATA_DICTIONARY.add("information_schema");
    }

    /**
     * Check user authority for the databases. Throw exception if the user has no specified permission.
     *
     * @param databaseIds database ids
     * @param permissionTypes permission types
     */
    public void checkDBPermissions(Collection<Long> databaseIds, Collection<DatabasePermissionType> permissionTypes) {
        if (CollectionUtils.isEmpty(databaseIds) || CollectionUtils.isEmpty(permissionTypes)) {
            return;
        }
        if (authenticationFacade.currentUser().getOrganizationType() == OrganizationType.INDIVIDUAL) {
            return;
        }
        List<DatabaseEntity> entities = databaseRepository.findByIdIn(databaseIds);
        List<Long> toCheckDatabaseIds = new ArrayList<>();
        Set<Long> projectIds = getPermittedProjectIds();
        for (DatabaseEntity e : entities) {
            if (e.getProjectId() == null) {
                throw new AccessDeniedException("Database is not belong to any project");
            }
            if (!projectIds.contains(e.getProjectId())) {
                toCheckDatabaseIds.add(e.getId());
            }
        }
        Map<Long, Set<DatabasePermissionType>> id2PermissionTypes = getInnerDBPermissionTypes(databaseIds);
        for (Long databaseId : toCheckDatabaseIds) {
            Set<DatabasePermissionType> authorized = id2PermissionTypes.get(databaseId);
            Set<DatabasePermissionType> unAuthorized =
                    permissionTypes.stream().filter(e -> CollectionUtils.isEmpty(authorized) || !authorized.contains(e))
                            .collect(Collectors.toSet());
            if (CollectionUtils.isNotEmpty(unAuthorized)) {
                throw new BadRequestException(ErrorCodes.DatabaseAccessDenied,
                        new Object[] {unAuthorized.stream().map(DatabasePermissionType::getLocalizedMessage)
                                .collect(Collectors.joining(","))},
                        "Lack permission for the database with id " + databaseId);
            }
        }
    }

    /**
     * Get user authority for the databases.
     *
     * @param databaseIds database ids
     * @return database id to permission types
     */
    public Map<Long, Set<DatabasePermissionType>> getDBPermissions(Collection<Long> databaseIds) {
        Map<Long, Set<DatabasePermissionType>> ret = new HashMap<>();
        if (CollectionUtils.isEmpty(databaseIds)) {
            return ret;
        }
        if (authenticationFacade.currentUser().getOrganizationType() == OrganizationType.INDIVIDUAL) {
            for (Long databaseId : databaseIds) {
                ret.put(databaseId, new HashSet<>(DatabasePermissionType.all()));
            }
            return ret;
        }
        List<DatabaseEntity> entities = databaseRepository.findByIdIn(databaseIds);
        Set<Long> projectIds = getPermittedProjectIds();
        Map<Long, Set<DatabasePermissionType>> id2PermissionTypes = getInnerDBPermissionTypes(databaseIds);
        for (DatabaseEntity e : entities) {
            if (e.getProjectId() == null) {
                ret.put(e.getId(), new HashSet<>());
            } else if (projectIds.contains(e.getProjectId())) {
                ret.put(e.getId(), new HashSet<>(DatabasePermissionType.all()));
            } else {
                if (id2PermissionTypes.containsKey(e.getId())) {
                    ret.put(e.getId(), id2PermissionTypes.get(e.getId()));
                } else {
                    ret.put(e.getId(), new HashSet<>());
                }
            }
        }
        return ret;
    }

    /**
     * Get user authority for the tables.
     *
     * @param tableIds table ids
     * @return table id to permission types
     */
    public Map<Long, Set<DatabasePermissionType>> getTablePermissions(Collection<Long> tableIds) {
        Map<Long, Set<DatabasePermissionType>> ret = new HashMap<>();
        if (CollectionUtils.isEmpty(tableIds)) {
            return ret;
        }
        if (authenticationFacade.currentUser().getOrganizationType() == OrganizationType.INDIVIDUAL) {
            for (Long tableId : tableIds) {
                ret.put(tableId, new HashSet<>(DatabasePermissionType.all()));
            }
            return ret;
        }
        List<DBObjectEntity> tbEntities = dbObjectRepository.findByIdIn(tableIds);
        Set<Long> dbIds = tbEntities.stream().map(DBObjectEntity::getDatabaseId).collect(Collectors.toSet());
        Map<Long, DatabaseEntity> dbId2Entity = databaseRepository.findByIdIn(dbIds).stream()
                .collect(Collectors.toMap(DatabaseEntity::getId, e -> e));
        Set<Long> projectIds = getPermittedProjectIds();
        Map<Long, List<UserDatabasePermissionEntity>> dbId2Permissions = userDatabasePermissionRepository
                .findNotExpiredByUserIdAndDatabaseIdIn(authenticationFacade.currentUserId(), dbIds)
                .stream().collect(Collectors.groupingBy(UserDatabasePermissionEntity::getDatabaseId));
        Map<Long, List<UserTablePermissionEntity>> tbId2Permissions = userTablePermissionRepository
                .findNotExpiredByUserIdAndTableIdIn(authenticationFacade.currentUserId(), tableIds)
                .stream().collect(Collectors.groupingBy(UserTablePermissionEntity::getTableId));
        for (DBObjectEntity e : tbEntities) {
            if (!dbId2Entity.containsKey(e.getDatabaseId())
                    || dbId2Entity.get(e.getDatabaseId()).getProjectId() == null) {
                ret.put(e.getId(), new HashSet<>());
            } else if (projectIds.contains(dbId2Entity.get(e.getDatabaseId()).getProjectId())) {
                ret.put(e.getId(), new HashSet<>(DatabasePermissionType.all()));
            } else {
                Set<DatabasePermissionType> authorized = new HashSet<>();
                List<UserDatabasePermissionEntity> dbPermissions = dbId2Permissions.get(e.getDatabaseId());
                if (CollectionUtils.isNotEmpty(dbPermissions)) {
                    authorized.addAll(dbPermissions.stream().map(p -> DatabasePermissionType.from(p.getAction()))
                            .collect(Collectors.toSet()));
                }
                List<UserTablePermissionEntity> permissions = tbId2Permissions.get(e.getId());
                if (CollectionUtils.isNotEmpty(permissions)) {
                    authorized.addAll(permissions.stream().map(p -> DatabasePermissionType.from(p.getAction()))
                            .collect(Collectors.toSet()));
                }
                ret.put(e.getId(), authorized);
            }
        }
        return ret;
    }

    /**
     * Filter and return unauthorized database resources (ODC_DATABASE, ODC_TABLE) and permission types.
     * 
     * @param resource2Types Resource to permission types. The resource is defined by {@link DBResource}
     *        and warning that only {@link DBResource#getDataSourceId()},
     *        {@link DBResource#getDatabaseName()}, {@link DBResource#getTableName()} and
     *        {@link DBResource#getType()} are used. Other fields are autofilled by this method.
     *        {@link DBResource#getDataSourceId()}, {@link DBResource#getDatabaseName()} and
     *        {@link DBResource#getTableName()} could not be null.
     * @param ignoreDataDictionary Ignore data dictionary schema
     * @return Unauthorized database resources and permission types
     */
    public List<UnauthorizedDBResource> filterUnauthorizedDBResources(
            Map<DBResource, Set<DatabasePermissionType>> resource2Types, boolean ignoreDataDictionary) {
        if (resource2Types == null || resource2Types.isEmpty()) {
            return Collections.emptyList();
        }
        // Pre-handle DBResource (fill in databaseId, databaseName, tableId, tableName)
        Set<Long> dsIds = resource2Types.keySet().stream().map(DBResource::getDataSourceId).collect(Collectors.toSet());
        List<DatabaseEntity> dbEntities = databaseRepository.findByConnectionIdIn(dsIds);
        Map<Long, DatabaseEntity> dbId2Entity =
                dbEntities.stream().collect(Collectors.toMap(DatabaseEntity::getId, e -> e));
        Map<Long, Map<String, DatabaseEntity>> dsId2dbName2dbEntity = new HashMap<>();
        for (DatabaseEntity dbEntity : dbEntities) {
            dsId2dbName2dbEntity
                    .computeIfAbsent(dbEntity.getConnectionId(), k -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER))
                    .put(dbEntity.getName(), dbEntity);
        }
        resource2Types.forEach((k, v) -> {
            if (dsId2dbName2dbEntity.containsKey(k.getDataSourceId())) {
                DatabaseEntity dbEntity = dsId2dbName2dbEntity.get(k.getDataSourceId()).get(k.getDatabaseName());
                if (dbEntity != null) {
                    k.setDatabaseId(dbEntity.getId());
                    k.setDatabaseName(dbEntity.getName());
                }
            }
        });
        Set<Long> dbIds = resource2Types.keySet().stream().map(DBResource::getDatabaseId).collect(Collectors.toSet());
        List<DBObjectEntity> tbEntities = dbObjectRepository.findByDatabaseIdInAndType(dbIds, DBObjectType.TABLE);
        Map<Long, DBObjectEntity> tbId2Entity =
                tbEntities.stream().collect(Collectors.toMap(DBObjectEntity::getId, e -> e));
        Map<Long, Map<String, DBObjectEntity>> dbId2tbName2tbEntity = new HashMap<>();
        for (DBObjectEntity tbEntity : tbEntities) {
            dbId2tbName2tbEntity
                    .computeIfAbsent(tbEntity.getDatabaseId(), k -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER))
                    .put(tbEntity.getName(), tbEntity);
        }
        resource2Types.forEach((k, v) -> {
            if (k.getTableName() != null && k.getDatabaseId() != null
                    && dbId2tbName2tbEntity.containsKey(k.getDatabaseId())) {
                DBObjectEntity tbEntity = dbId2tbName2tbEntity.get(k.getDatabaseId()).get(k.getTableName());
                if (tbEntity != null) {
                    k.setTableId(tbEntity.getId());
                    k.setTableName(tbEntity.getName());
                }
            }
        });
        Set<Long> tbIds = resource2Types.keySet().stream().map(DBResource::getTableId).collect(Collectors.toSet());
        // Get permitted and involved project ids
        Map<Long, Set<ResourceRoleName>> projectIds2Roles = projectService.getProjectId2ResourceRoleNames();
        Set<Long> involvedProjectIds = projectIds2Roles.keySet();
        Set<Long> permittedProjectIds = projectIds2Roles.entrySet().stream()
                .filter(e -> e.getValue().contains(ResourceRoleName.OWNER)
                        || e.getValue().contains(ResourceRoleName.DBA)
                        || e.getValue().contains(ResourceRoleName.DEVELOPER))
                .map(Map.Entry::getKey).collect(Collectors.toSet());
        // Get user permissions
        Map<Long, List<UserDatabasePermissionEntity>> dbId2Permissions = userDatabasePermissionRepository
                .findNotExpiredByUserIdAndDatabaseIdIn(authenticationFacade.currentUserId(), dbIds)
                .stream().collect(Collectors.groupingBy(UserDatabasePermissionEntity::getDatabaseId));
        Map<Long, List<UserTablePermissionEntity>> tbId2Permissions = userTablePermissionRepository
                .findNotExpiredByUserIdAndTableIdIn(authenticationFacade.currentUserId(), tbIds)
                .stream().collect(Collectors.groupingBy(UserTablePermissionEntity::getTableId));
        // Filter unauthorized resources
        List<UnauthorizedDBResource> unauthorizedDBResources = new ArrayList<>();
        for (Map.Entry<DBResource, Set<DatabasePermissionType>> entry : resource2Types.entrySet()) {
            DBResource resource = entry.getKey();
            Set<DatabasePermissionType> needs = entry.getValue();
            if (CollectionUtils.isEmpty(needs)) {
                continue;
            }
            if (resource.getType() == ResourceType.ODC_DATABASE) {
                if (resource.getDatabaseId() == null) {
                    unauthorizedDBResources.add(UnauthorizedDBResource.from(resource, needs, false));
                    continue;
                }
                DatabaseEntity database = dbId2Entity.get(resource.getDatabaseId());
                Set<DatabasePermissionType> authorized = new HashSet<>();
                if (permittedProjectIds.contains(database.getProjectId())) {
                    authorized.addAll(DatabasePermissionType.all());
                } else {
                    List<UserDatabasePermissionEntity> permissions = dbId2Permissions.get(database.getId());
                    if (CollectionUtils.isNotEmpty(permissions)) {
                        authorized.addAll(permissions.stream().map(e -> DatabasePermissionType.from(e.getAction()))
                                .collect(Collectors.toSet()));
                    }
                    if (dbId2tbName2tbEntity.get(database.getId()) != null) {
                        Collection<DBObjectEntity> tbs = dbId2tbName2tbEntity.get(database.getId()).values();
                        for (DBObjectEntity tbEntity : tbs) {
                            List<UserTablePermissionEntity> tablePermissions = tbId2Permissions.get(tbEntity.getId());
                            if (CollectionUtils.isNotEmpty(tablePermissions)) {
                                authorized.add(DatabasePermissionType.ACCESS);
                                break;
                            }
                        }
                    }
                }
                Set<DatabasePermissionType> unauthorized =
                        needs.stream().filter(p -> !authorized.contains(p)).collect(Collectors.toSet());
                if (CollectionUtils.isNotEmpty(unauthorized)) {
                    unauthorizedDBResources.add(UnauthorizedDBResource.from(resource, unauthorized,
                            database.getProjectId() != null && involvedProjectIds.contains(database.getProjectId())));
                }
            } else if (resource.getType() == ResourceType.ODC_TABLE) {
                if (resource.getDatabaseId() == null) {
                    unauthorizedDBResources.add(UnauthorizedDBResource.from(resource, needs, false));
                    continue;
                }
                Set<DatabasePermissionType> authorized = new HashSet<>();
                DatabaseEntity database = dbId2Entity.get(resource.getDatabaseId());
                if (permittedProjectIds.contains(database.getProjectId())) {
                    authorized.addAll(DatabasePermissionType.all());
                } else {
                    List<UserDatabasePermissionEntity> dbPermissions = dbId2Permissions.get(database.getId());
                    if (CollectionUtils.isNotEmpty(dbPermissions)) {
                        authorized.addAll(dbPermissions.stream().map(e -> DatabasePermissionType.from(e.getAction()))
                                .collect(Collectors.toSet()));
                    }
                    if (resource.getTableId() != null) {
                        DBObjectEntity table = tbId2Entity.get(resource.getTableId());
                        List<UserTablePermissionEntity> permissions = tbId2Permissions.get(table.getId());
                        if (CollectionUtils.isNotEmpty(permissions)) {
                            authorized.addAll(permissions.stream().map(e -> DatabasePermissionType.from(e.getAction()))
                                    .collect(Collectors.toSet()));
                        }
                    }
                }
                Set<DatabasePermissionType> unauthorized =
                        needs.stream().filter(p -> !authorized.contains(p)).collect(Collectors.toSet());
                if (CollectionUtils.isNotEmpty(unauthorized)) {
                    unauthorizedDBResources.add(UnauthorizedDBResource.from(resource, unauthorized,
                            database.getProjectId() != null && involvedProjectIds.contains(database.getProjectId())));
                }
            } else {
                throw new IllegalStateException("Unsupported resource type: " + resource.getType());
            }
        }
        // Filter data dictionary schema
        if (ignoreDataDictionary) {
            unauthorizedDBResources = unauthorizedDBResources.stream().filter(e -> {
                if (e.getDialectType().isOracle()) {
                    return !ORACLE_DATA_DICTIONARY.contains(e.getDatabaseName());
                } else if (e.getDialectType().isMysql()) {
                    return !MYSQL_DATA_DICTIONARY.contains(e.getDatabaseName());
                } else {
                    return true;
                }
            }).collect(Collectors.toList());
        }
        return unauthorizedDBResources;
    }

    private Set<Long> getPermittedProjectIds() {
        // OWNER, DBA or DEVELOPER can access all databases inner the project
        Map<Long, Set<ResourceRoleName>> projectIds2Roles = projectService.getProjectId2ResourceRoleNames();
        return projectIds2Roles.entrySet().stream()
                .filter(e -> e.getValue().contains(ResourceRoleName.OWNER)
                        || e.getValue().contains(ResourceRoleName.DBA)
                        || e.getValue().contains(ResourceRoleName.DEVELOPER))
                .map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    private Map<Long, Set<DatabasePermissionType>> getInnerDBPermissionTypes(Collection<Long> databaseIds) {
        Map<Long, Set<DatabasePermissionType>> typesFromDatabase = userDatabasePermissionRepository
                .findNotExpiredByUserIdAndDatabaseIdIn(authenticationFacade.currentUserId(), databaseIds)
                .stream().collect(Collectors.toMap(
                        UserDatabasePermissionEntity::getDatabaseId,
                        e -> {
                            Set<DatabasePermissionType> list = new HashSet<>();
                            list.add(DatabasePermissionType.from(e.getAction()));
                            return list;
                        },
                        (e1, e2) -> {
                            e1.addAll(e2);
                            return e1;
                        }));
        Map<Long, Set<DatabasePermissionType>> typesFromTable = userTablePermissionRepository
                .findNotExpiredByUserIdAndDatabaseIdIn(authenticationFacade.currentUserId(), databaseIds)
                .stream().collect(Collectors.toMap(
                        UserTablePermissionEntity::getDatabaseId,
                        e -> Collections.singleton(DatabasePermissionType.ACCESS),
                        (e1, e2) -> e1));
        typesFromTable.forEach((k, v) -> typesFromDatabase.merge(k, v, (v1, v2) -> {
            v1.addAll(v2);
            return v1;
        }));
        return typesFromDatabase;
    }

}
