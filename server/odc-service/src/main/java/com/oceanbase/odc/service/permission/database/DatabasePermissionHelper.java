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
package com.oceanbase.odc.service.permission.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.iam.UserDatabasePermissionEntity;
import com.oceanbase.odc.metadb.iam.UserDatabasePermissionRepository;
import com.oceanbase.odc.metadb.iam.UserTablePermissionEntity;
import com.oceanbase.odc.metadb.iam.UserTablePermissionRepository;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.permission.table.TablePermissionService;

/**
 * @author gaoda.xy
 * @date 2024/1/15 10:58
 */
@Component
public class DatabasePermissionHelper {

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
    private TablePermissionService tablePermissionService;

    /**
     * Check whether the current user has the permission to access the database
     * 
     * @param databaseIds database ids
     * @param permissionTypes permission types
     */
    public void checkPermissions(Collection<Long> databaseIds, Collection<DatabasePermissionType> permissionTypes) {
        if (authenticationFacade.currentUser().getOrganizationType() == OrganizationType.INDIVIDUAL) {
            return;
        }
        if (CollectionUtils.isEmpty(databaseIds) || CollectionUtils.isEmpty(permissionTypes)) {
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
        Map<Long, Set<DatabasePermissionType>> id2PermissionTypes = getDatabaseId2PermissionTypes(databaseIds);
        for (Long databaseId : toCheckDatabaseIds) {
            // TODO: may use Permission#implies() to check permission
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
     * Get the DB access permissions of the databases
     * 
     * @param databaseIds database ids
     * @return database id to permission types
     */
    public Map<Long, Set<DatabasePermissionType>> getPermissions(Collection<Long> databaseIds) {
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
        Map<Long, Set<DatabasePermissionType>> id2PermissionTypes = getDatabaseId2PermissionTypes(databaseIds);
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

    private Set<Long> getPermittedProjectIds() {
        // OWNER, DBA or DEVELOPER of a project can access all databases inner the project
        Map<Long, Set<ResourceRoleName>> projectIds2Roles = projectService.getProjectId2ResourceRoleNames();
        return projectIds2Roles.entrySet().stream()
                .filter(e -> e.getValue().contains(ResourceRoleName.OWNER)
                        || e.getValue().contains(ResourceRoleName.DBA)
                        || e.getValue().contains(ResourceRoleName.DEVELOPER))
                .map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    private Map<Long, Set<DatabasePermissionType>> getDatabaseId2PermissionTypes(Collection<Long> databaseIds) {
        Map<Long, Set<DatabasePermissionType>> databaseId2PermissionTypesFromTable =
                getDatabaseId2PermissionTypesFromTable(databaseIds);
        Map<Long, Set<DatabasePermissionType>> databaseId2PermissionTypes = userDatabasePermissionRepository
                .findNotExpiredByUserIdAndDatabaseIdIn(authenticationFacade.currentUserId(), databaseIds).stream()
                .collect(Collectors.toMap(
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
        for (Map.Entry<Long, Set<DatabasePermissionType>> entry : databaseId2PermissionTypesFromTable.entrySet()) {
            if (databaseId2PermissionTypes.containsKey(entry.getKey())) {
                databaseId2PermissionTypes.get(entry.getKey()).addAll(entry.getValue());
            } else {
                databaseId2PermissionTypes.put(entry.getKey(), entry.getValue());
            }
        }
        return databaseId2PermissionTypes;
    }

    private Map<Long, Set<DatabasePermissionType>> getDatabaseId2PermissionTypesFromTable(
            Collection<Long> databaseIds) {
        return userTablePermissionRepository
                .findNotExpiredByUserIdAndDatabaseIdIn(authenticationFacade.currentUserId(), databaseIds).stream()
                .collect(Collectors.toMap(
                        UserTablePermissionEntity::getDatabaseId,
                        e -> {
                            Set<DatabasePermissionType> list = new HashSet<>();
                            list.add(DatabasePermissionType.ACCESS);
                            return list;
                        },
                        (e1, e2) -> {
                            e1.addAll(e2);
                            return e1;
                        }));
    }

}
