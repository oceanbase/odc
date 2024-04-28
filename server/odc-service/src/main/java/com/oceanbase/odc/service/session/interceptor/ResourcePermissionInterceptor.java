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
package com.oceanbase.odc.service.session.interceptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.sql.execute.SqlExecuteStages;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.table.TableService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.permission.table.TablePermissionService;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteReq;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteResp;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.odc.service.session.model.SqlTuplesWithViolation;
import com.oceanbase.odc.service.session.model.UnauthorizedResource;
import com.oceanbase.odc.service.session.util.SchemaExtractor;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * ClassName: TablePermissionInterceptor Package: com.oceanbase.odc.service.session.interceptor
 * Description:
 *
 * @Author: fenghao
 * @Create 2024/3/20 14:44
 * @Version 1.0
 */

// check column,table,database permission
@Slf4j
@Component
public class ResourcePermissionInterceptor extends BaseTimeConsumingInterceptor {
    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private TableService tableService;

    @Autowired
    private TablePermissionService tablePermissionService;

    @Autowired
    private ConnectionService connectionService;


    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public boolean doPreHandle(@NonNull SqlAsyncExecuteReq request, @NonNull SqlAsyncExecuteResp response,
            @NonNull ConnectionSession session, @NonNull Map<String, Object> context) throws Exception {
        if (authenticationFacade.currentUser().getOrganizationType() == OrganizationType.INDIVIDUAL) {
            return true;
        }
        ConnectionConfig connectionConfig = (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(session);
        String currentSchema = ConnectionSessionUtil.getCurrentSchema(session);
        Map<RelationFactor, Set<SqlType>> relationFactorSetMap = SchemaExtractor.listTableName2SqlTypes(
                response.getSqls().stream().map(SqlTuplesWithViolation::getSqlTuple).collect(Collectors.toList()),
                currentSchema, session.getDialectType());
        Map<RelationFactor, Set<DatabasePermissionType>> tableName2PermissionTypes = new HashMap<>();
        for (Entry<RelationFactor, Set<SqlType>> entry : relationFactorSetMap.entrySet()) {
            Set<SqlType> sqlTypes = entry.getValue();
            if (CollectionUtils.isNotEmpty(sqlTypes)) {
                Set<DatabasePermissionType> permissionTypes = sqlTypes.stream().map(DatabasePermissionType::from)
                        .filter(Objects::nonNull).collect(Collectors.toSet());
                if (CollectionUtils.isNotEmpty(permissionTypes)) {
                    tableName2PermissionTypes.put(entry.getKey(), permissionTypes);
                }
            }
        }
        List<UnauthorizedResource> unauthorizedResource =
                tablePermissionService.filterUnauthorizedTables(tableName2PermissionTypes, connectionConfig.getId(),
                        false);
        if (CollectionUtils.isNotEmpty(unauthorizedResource)) {
            response.setUnauthorizedResource(unauthorizedResource);
            return false;
        }
        return true;
    }

    public boolean doPreHandle(@NonNull List<SqlTuple> request, Long databaseId,
            Set<DatabasePermissionType> databasePermissionTypes) throws Exception {
        if (authenticationFacade.currentUser().getOrganizationType() == OrganizationType.INDIVIDUAL) {
            return true;
        }
        Database databaseDetail = databaseService.detail(databaseId);
        Map<RelationFactor, Set<SqlType>> relationFactorSetMap = SchemaExtractor.listTableName2SqlTypes(
                request,
                databaseDetail.getName(), databaseDetail.getDataSource().getDialectType());
        Map<RelationFactor, Set<DatabasePermissionType>> tableName2PermissionTypes = new HashMap<>();
        for (Entry<RelationFactor, Set<SqlType>> entry : relationFactorSetMap.entrySet()) {
            Set<SqlType> sqlTypes = entry.getValue();
            if (CollectionUtils.isNotEmpty(sqlTypes)) {
                Set<DatabasePermissionType> permissionTypes = sqlTypes.stream().map(DatabasePermissionType::from)
                        .filter(Objects::nonNull).collect(Collectors.toSet());
                if (!databasePermissionTypes.isEmpty()) {
                    permissionTypes.addAll(databasePermissionTypes);
                }
                if (CollectionUtils.isNotEmpty(permissionTypes)) {
                    tableName2PermissionTypes.put(entry.getKey(), permissionTypes);
                }
            }
        }
        List<UnauthorizedResource> unauthorizedResource =
                tablePermissionService.filterUnauthorizedTables(tableName2PermissionTypes,
                        databaseDetail.getDataSource().getId(), false);

        if (CollectionUtils.isNotEmpty(unauthorizedResource)) {
            throw new BadRequestException(ErrorCodes.DatabaseAccessDenied,
                    new Object[] {
                            unauthorizedResource.stream().map(UnauthorizedResource::getUnauthorizedPermissionTypes)
                                    .flatMap(Set::stream).map(DatabasePermissionType::getLocalizedMessage)
                                    .collect(Collectors.joining(","))},
                    "Lack permission for the database with id " + databaseId);
        }
        return true;
    }

    @Override
    public void afterCompletion(@NonNull SqlExecuteResult response, @NonNull ConnectionSession session,
            @NonNull Map<String, Object> context) {}

    @Override
    protected String getExecuteStageName() {
        return SqlExecuteStages.DATABASE_PERMISSION_CHECK;
    }
}
