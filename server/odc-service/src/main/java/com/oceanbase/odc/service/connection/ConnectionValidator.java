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

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.service.collaboration.environment.EnvironmentService;
import com.oceanbase.odc.service.connection.model.ConnectProperties;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;

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
        if (connection.getType().isDefaultSchemaRequired()) {
            PreConditions.notBlank(connection.getDefaultSchema(), "connection.defaultSchema");
        }
    }

    void validateForUpdate(ConnectionConfig connection, ConnectionConfig saved) {
        PreConditions.validRequestState(
                Objects.isNull(connection.getType())
                        || Objects.equals(connection.getType(), saved.getType()),
                ErrorCodes.FieldUpdateNotSupported, new Object[] {"connection.type"},
                "Cannot change field 'connection.type'");
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

}
