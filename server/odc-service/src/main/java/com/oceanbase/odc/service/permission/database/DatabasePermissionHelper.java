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
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

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

    public void checkPermission(Collection<Long> databaseIds, Collection<String> actions) {
        if (CollectionUtils.isEmpty(databaseIds) || CollectionUtils.isEmpty(actions)) {
            return;
        }
        Map<Long, List<Long>> projectId2DatabaseIds = databaseService.listDatabasesByIds(databaseIds).stream()
                .filter(
                        e -> {
                            if (e.getProject() == null || e.getProject().getId() == null) {
                                throw new AccessDeniedException("Database is not belong to any project");
                            }
                            return true;
                        })
                .collect(Collectors.toMap(
                        e -> e.getProject().getId(),
                        e -> {
                            List<Long> list = new ArrayList<>();
                            list.add(e.getId());
                            return list;
                        },
                        (e1, e2) -> {
                            e1.addAll(e2);
                            return e1;
                        }));

        Map<Long, Set<ResourceRoleName>> projectIds2Roles = projectService.getProjectId2ResourceRoleNames();
        Set<Long> projectIds = projectIds2Roles.entrySet().stream()
                .filter(e -> e.getValue().contains(ResourceRoleName.OWNER)
                        || e.getValue().contains(ResourceRoleName.DBA)
                        || e.getValue().contains(ResourceRoleName.DEVELOPER))
                .map(Map.Entry::getKey).collect(Collectors.toSet());

        List<Long> toCheckDatabaseIds = new ArrayList<>();
        for (Map.Entry<Long, List<Long>> entry : projectId2DatabaseIds.entrySet()) {
            if (!projectIds.contains(entry.getKey())) {
                toCheckDatabaseIds.addAll(entry.getValue());
            }
        }

        Map<Long, List<String>> id2Actions = userDatabasePermissionRepository
                .findByUserIdAndDatabaseIdIn(authenticationFacade.currentUserId(), toCheckDatabaseIds).stream()
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
        for (Long databaseId : toCheckDatabaseIds) {
            List<String> permittedActions = id2Actions.get(databaseId);
            if (CollectionUtils.isEmpty(permittedActions)) {
                throw new AccessDeniedException(String.format("No permission for the database with id %s", databaseId));
            }
            Set<String> unPermittedActions = actions.stream().filter(action -> !permittedActions.contains(action))
                    .collect(Collectors.toSet());
            if (!unPermittedActions.isEmpty()) {
                throw new AccessDeniedException(String.format("No %s permission for the database with id %s",
                        String.join(",", unPermittedActions), databaseId));
            }
        }
    }



}
