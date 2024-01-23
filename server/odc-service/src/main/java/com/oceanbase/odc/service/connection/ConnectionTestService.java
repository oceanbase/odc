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

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ConnectionAccountType;
import com.oceanbase.odc.core.shared.constant.ErrorCode;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.service.connection.CloudMetadataClient.PermissionAction;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.ConnectionTestResult;
import com.oceanbase.odc.service.connection.model.OBTenant;
import com.oceanbase.odc.service.connection.model.TestConnectionReq;

import lombok.extern.slf4j.Slf4j;

/**
 * Different from {@link ConnectionTesting}, ConnectionTestService will fetch password from metadb
 * if not given.
 * 
 * @author yizhou.xw
 * @version : ConnectionTestService.java, v 0.1 2021-07-26 21:31
 */
@Slf4j
@Service
@Validated
@SkipAuthorize("permission check inside getForConnect")
public class ConnectionTestService {

    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private ConnectionTesting connectionTesting;
    @Autowired
    private CloudMetadataClient cloudMetadataClient;

    public ConnectionTestResult test(@NotNull @Valid TestConnectionReq req) {
        PreConditions.notNull(req, "req");

        Long connectionId = req.getId();
        if (ConnectionAccountType.SYS_READ == req.getAccountType()) {
            req.setTenantName("sys");
        }
        if (Objects.nonNull(connectionId)) {
            ConnectionConfig connection = connectionService.getForConnect(connectionId);
            cloudMetadataClient.checkPermission(OBTenant.of(connection.getClusterName(),
                    connection.getTenantName()), connection.getInstanceType(), false, PermissionAction.READONLY);
            if (Objects.isNull(req.getType())) {
                req.setType(connection.getType());
            }
            if (Objects.isNull(req.getPassword())) {
                log.info("Test connection without password, fill from saved connection, connectionId={}", connectionId);
                req.fillPasswordIfNull(connection);
            }
        }
        // the schema value in req may contains double quote
        // so we need to unquote schema name here
        // why not unquote in connection testing, because connection status periodically check depends on
        // connectionTesing too
        // but connection status periodically check offers unquoted schema name
        req.setDefaultSchema(ConnectionSessionUtil.getUserOrSchemaString(req.getDefaultSchema(), req.getDialectType()));
        ConnectionTestResult result = connectionTesting.test(req);
        ErrorCode errorCode = result.getErrorCode();
        if (errorCode != ErrorCodes.Unknown && errorCode != ErrorCodes.ConnectionUnsupportedConnectType) {
            return result;
        }
        /**
         * 未知错误以及 {@link ErrorCodes.ConnectionUnsupportedConnectType} 前端没有对应处理逻辑，这里直接抛出，让前端走接口异常的处理逻辑
         */
        throw new BadRequestException(result.getErrorCode(), result.getArgs(), result.getErrorMessage());
    }
}
