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
package com.oceanbase.odc.service.automation;

import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.metadb.collaboration.ProjectEntity;
import com.oceanbase.odc.metadb.iam.RoleEntity;
import com.oceanbase.odc.service.automation.model.AutomationConstants;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.iam.ResourceRoleService;
import com.oceanbase.odc.service.iam.RoleService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@SkipAuthorize("odc internal usage")
public class AutomationActionChecker {

    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private RoleService roleService;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private ResourceRoleService resourceRoleService;
    @Autowired
    private ProjectService projectService;

    public void check(String action, Map<String, Object> arguments) {
        if (AutomationConstants.BIND_ROLE.equals(action)) {
            checkBindRole(arguments);
        } else if (AutomationConstants.BIND_PERMISSION.equals(action)) {
            checkBindPermission(arguments);
        } else if (AutomationConstants.BIND_PROJECT_ROLE.equals(action)) {
            checkBindProjectRole(arguments);
        } else {
            throw new IllegalArgumentException("Illegal action : " + action);
        }
    }

    private void checkBindRole(Map<String, Object> arguments) {
        PreConditions.notNull(arguments.get("roleId"), "role id");
        Long roleId = ((Integer) arguments.get("roleId")).longValue();
        RoleEntity role = roleService.nullSafeGet(roleId);
        if (!Objects.equals(authenticationFacade.currentOrganizationId(), role.getOrganizationId())) {
            throw new AccessDeniedException("Could not bind role from different organization!");
        }
    }

    private void checkBindPermission(Map<String, Object> arguments) {
        if (!ResourceType.ODC_CONNECTION.code().equals(arguments.get("resourceType"))) {
            throw new IllegalArgumentException("Unknown resourceType : " + arguments.get("resourceType"));
        }
        Object resourceId = arguments.get("resourceId");
        if (resourceId == null) {
            throw new IllegalArgumentException("resourceId could not be null");
        } else if (resourceId instanceof String) {
            if ("ALL".equals(resourceId)) {
                return;
            }
            throw new IllegalArgumentException("Illegal resourceId : " + resourceId);
        }
        resourceId = ((Integer) resourceId).longValue();
        ConnectionConfig connectionConfig = connectionService.getWithoutPermissionCheck((Long) resourceId);
        if (!Objects.equals(authenticationFacade.currentOrganizationId(), connectionConfig.getOrganizationId())) {
            throw new AccessDeniedException("Could not bind resource from different organization!");
        }
    }

    private void checkBindProjectRole(Map<String, Object> arguments) {
        PreConditions.notNull(arguments.get("projectId"), "project id");
        Long projectId = ((Integer) arguments.get("projectId")).longValue();
        ProjectEntity projectEntity = projectService.nullSafeGet(projectId);
        if (!Objects.equals(projectEntity.getOrganizationId(), authenticationFacade.currentOrganizationId())) {
            throw new AccessDeniedException("Could not bind project from different organizations!");
        }
    }
}
