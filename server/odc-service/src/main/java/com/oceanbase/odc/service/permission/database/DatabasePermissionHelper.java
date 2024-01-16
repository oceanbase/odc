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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.metadb.iam.UserDatabasePermissionEntity;
import com.oceanbase.odc.metadb.iam.UserDatabasePermissionRepository;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;

/**
 * @author gaoda.xy
 * @date 2024/1/15 10:58
 */
@Component
public class DatabasePermissionHelper {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private UserDatabasePermissionRepository userDatabasePermissionRepository;

    public void checkPermissions(Collection<Long> databaseIds, Collection<String> actions) {
        if (CollectionUtils.isEmpty(databaseIds) || CollectionUtils.isEmpty(actions)) {
            return;
        }
        List<Database> databases = databaseService.listDatabasesByIds(databaseIds);
        List<Long> toCheckDatabaseIds = new ArrayList<>();
        Set<Long> projectIds = getAllDatabasePermittedProjectIds();
        for (Database database : databases) {
            if (database.getProject() == null || database.getProject().getId() == null) {
                throw new AccessDeniedException("Database is not belong to any project");
            }
            if (!projectIds.contains(database.getProject().getId())) {
                toCheckDatabaseIds.add(database.getId());
            }
        }
        Map<Long, List<String>> id2Actions = getDatabaseId2Actions(databaseIds);
        for (Long databaseId : toCheckDatabaseIds) {
            List<String> permittedActions = id2Actions.get(databaseId);
            if (CollectionUtils.isEmpty(permittedActions)) {
                throw new AccessDeniedException(String.format("No permission for the database with id %s", databaseId));
            }
            Set<String> unPermittedActions = actions.stream().filter(action -> !permittedActions.contains(action))
                    .collect(Collectors.toSet());
            if (CollectionUtils.isNotEmpty(unPermittedActions)) {
                throw new AccessDeniedException(String.format("No %s permission for the database with id %s",
                        String.join(",", unPermittedActions), databaseId));
            }
        }
    }

    public Map<Long, List<DatabasePermissionType>> getPermissions(Collection<Long> databaseIds) {
        Map<Long, List<DatabasePermissionType>> ret = new HashMap<>();
        if (CollectionUtils.isEmpty(databaseIds)) {
            return ret;
        }
        List<Database> databases = databaseService.listDatabasesByIds(databaseIds);
        Set<Long> projectIds = getAllDatabasePermittedProjectIds();
        Map<Long, List<String>> id2Actions = getDatabaseId2Actions(databaseIds);
        for (Database database : databases) {
            if (database.getProject() == null || database.getProject().getId() == null) {
                ret.put(database.getId(), new ArrayList<>());
            } else if (projectIds.contains(database.getProject().getId())) {
                ret.put(database.getId(), DatabasePermissionType.all());
            } else {
                if (id2Actions.containsKey(database.getId())) {
                    ret.put(database.getId(), id2Actions.get(database.getId()).stream()
                            .map(DatabasePermissionType::from).collect(Collectors.toList()));
                } else {
                    ret.put(database.getId(), new ArrayList<>());
                }
            }
        }
        return ret;
    }

    private Set<Long> getAllDatabasePermittedProjectIds() {
        Map<Long, Set<ResourceRoleName>> projectIds2Roles = projectService.getProjectId2ResourceRoleNames();
        return projectIds2Roles.entrySet().stream()
                .filter(e -> e.getValue().contains(ResourceRoleName.OWNER)
                        || e.getValue().contains(ResourceRoleName.DBA)
                        || e.getValue().contains(ResourceRoleName.DEVELOPER))
                .map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    private Map<Long, List<String>> getDatabaseId2Actions(Collection<Long> databaseIds) {
        return userDatabasePermissionRepository
                .findByExpireTimeAfterAndUserIdAndDatabaseIdIn(new Date(), authenticationFacade.currentUserId(),
                        databaseIds)
                .stream()
                .collect(Collectors.toMap(
                        UserDatabasePermissionEntity::getDatabaseId,
                        e -> {
                            List<String> list = new ArrayList<>();
                            list.add(e.getAction());
                            return list;
                        },
                        (e1, e2) -> {
                            e1.addAll(e2);
                            return e1;
                        }));
    }


}
