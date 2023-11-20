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

import java.util.Arrays;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.authority.SecurityManager;
import com.oceanbase.odc.core.authority.model.DefaultSecurityResource;
import com.oceanbase.odc.core.authority.permission.Permission;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.service.collaboration.environment.EnvironmentService;
import com.oceanbase.odc.service.collaboration.project.ProjectMapper;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.collaboration.project.model.Project;
import com.oceanbase.odc.service.connection.model.ConnectProperties;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;

/**
 * @Author: Lebie
 * @Date: 2023/5/24 19:20
 * @Description: []
 */
@Component
public class ConnectionValidator {
    @Autowired
    private ConnectProperties connectProperties;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    @Lazy
    private ProjectService projectService;

    @Autowired
    private SecurityManager securityManager;

    @Autowired
    private HorizontalDataPermissionValidator permissionValidator;

    void validateForUpsert(ConnectionConfig connection) {
        PreConditions.notNull(connection, "connection");
        PreConditions.notBlank(connection.getHost(), "connection.host");
        PreConditions.notNull(connection.getPort(), "connection.port");
        PreConditions.validNotSqlInjection(connection.getUsername(), "username");
        PreConditions.validNotSqlInjection(connection.getClusterName(), "clusterName");
        PreConditions.validNotSqlInjection(connection.getTenantName(), "tenantName");
        PreConditions.validInHostWhiteList(connection.getHost(), connectProperties.getHostWhiteList());
        PreConditions.validArgumentState(environmentService.exists(connection.getEnvironmentId()),
                ErrorCodes.BadRequest, null, "invalid environment id");
    }

    void validateForUpdate(ConnectionConfig connection, ConnectionConfig saved) {
        PreConditions.validRequestState(
                Objects.isNull(connection.getType())
                        || Objects.equals(connection.getType(), saved.getType()),
                ErrorCodes.FieldUpdateNotSupported, new Object[] {"connection.type"},
                "Cannot change field 'connection.type'");
        PreConditions.validRequestState(
                Objects.isNull(connection.getDialectType())
                        || Objects.equals(connection.getDialectType(), saved.getDialectType()),
                ErrorCodes.FieldUpdateNotSupported, new Object[] {"connection.dialectType"},
                "Cannot change field 'connection.dialectType'");
    }

    void validatePrivateConnectionTempOnly(Boolean temp) {
        if (!connectProperties.isPrivateConnectTempOnly()) {
            return;
        }
        if (Objects.isNull(temp) || !temp) {
            throw new AccessDeniedException(ErrorCodes.ConnectionTempOnly,
                    "Cannot create persistent connection due temp only for private connection");
        }
    }

    /**
     * Validate whether the project is existing and the current user has permission (OWNER or DBA) to
     * operate the project.
     * 
     * @param projectId
     */
    void validateProjectOperable(Long projectId) {
        if (Objects.isNull(projectId)) {
            return;
        }
        Project project = ProjectMapper.INSTANCE.entityToModel(projectService.nullSafeGet(projectId));
        permissionValidator.checkCurrentOrganization(project);
        Permission requiredPermission = securityManager.getPermissionByResourceRoles(
                new DefaultSecurityResource(projectId.toString(), ResourceType.ODC_PROJECT.name()),
                Arrays.asList("OWNER", "DBA"));
        securityManager.checkPermission(requiredPermission);
    }

}
