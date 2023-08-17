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
package com.oceanbase.odc.service.connection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.authority.permission.ConnectionPermission;
import com.oceanbase.odc.core.shared.constant.ConnectionVisibleScope;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.auth.AuthorizationFacade;
import com.oceanbase.odc.service.iam.model.User;

import lombok.NonNull;

/**
 * for permission calculation
 *
 * @author yh263208
 * @date 2021-08-31 19:52
 * @since ODC_release_3.2.0
 */
@Component
public class ConnectionPermissionFilter {

    @Autowired
    private ConnectionConfigRepository repository;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private AuthorizationFacade authorizationFacade;

    public Map<Long, Set<String>> permittedConnectionActions(@NonNull Long userId,
            @NonNull Predicate<ConnectionPermission> predicate) {
        Map<SecurityResource, Set<String>> connection2Actions =
                authorizationFacade.getRelatedResourcesAndActions(User.of(userId)).entrySet().stream().filter(entry -> {
                    if (!ResourceType.ODC_CONNECTION.name().equals(entry.getKey().resourceType())) {
                        return false;
                    }
                    Set<String> actions = entry.getValue();
                    ConnectionPermission grantedPermission =
                            new ConnectionPermission(entry.getKey().resourceId(), String.join(",", actions));
                    return predicate.test(grantedPermission);
                }).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        Map<Long, Set<String>> idToActions = new HashMap<>();
        Set<Long> connectionIds = repository.findIdsByVisibleScopeAndOrganizationId(ConnectionVisibleScope.ORGANIZATION,
                authenticationFacade.currentOrganizationId());
        connection2Actions.forEach((securityResource, actions) -> {
            if ("*".equals(securityResource.resourceId())) {
                for (Long id : connectionIds) {
                    Set<String> grantedActions = idToActions.computeIfAbsent(id, connectionId -> new HashSet<>());
                    if (actions.contains("*")) {
                        grantedActions.addAll(ConnectionPermission.getAllActions());
                    } else {
                        grantedActions.addAll(actions);
                    }
                }
            } else {
                Set<String> grantedActions = idToActions.computeIfAbsent(
                        Long.valueOf(securityResource.resourceId()), connectionId -> new HashSet<>());
                if (actions.contains("*")) {
                    grantedActions.addAll(ConnectionPermission.getAllActions());
                } else {
                    grantedActions.addAll(actions);
                }
            }
        });
        return idToActions;
    }

}
