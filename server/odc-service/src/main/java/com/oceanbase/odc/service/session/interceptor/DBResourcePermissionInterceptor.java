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
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.sql.execute.SqlExecuteStages;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.DBResource;
import com.oceanbase.odc.service.connection.database.model.UnauthorizedDBResource;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.permission.DBResourcePermissionHelper;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.session.model.AsyncExecuteContext;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteReq;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteResp;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.odc.service.session.model.SqlTuplesWithViolation;
import com.oceanbase.odc.service.session.util.DBSchemaExtractor;
import com.oceanbase.odc.service.session.util.DBSchemaExtractor.DBSchemaIdentity;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @Author: fenghao
 * @Create 2024/3/20 14:44
 * @Version 1.0
 */
@Slf4j
@Component
public class DBResourcePermissionInterceptor extends BaseTimeConsumingInterceptor {
    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private DBResourcePermissionHelper dbResourcePermissionHelper;

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public boolean doPreHandle(@NonNull SqlAsyncExecuteReq request, @NonNull SqlAsyncExecuteResp response,
            @NonNull ConnectionSession session, @NonNull AsyncExecuteContext context) throws Exception {
        if (authenticationFacade.currentUser().getOrganizationType() == OrganizationType.INDIVIDUAL) {
            return true;
        }
        ConnectionConfig connectionConfig = (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(session);
        String currentSchema = ConnectionSessionUtil.getCurrentSchema(session);
        Map<DBSchemaIdentity, Set<SqlType>> identity2Types = DBSchemaExtractor.listDBSchemasWithSqlTypes(
                response.getSqls().stream().map(SqlTuplesWithViolation::getSqlTuple).collect(Collectors.toList()),
                session.getDialectType(), currentSchema);
        Map<DBResource, Set<DatabasePermissionType>> resource2PermissionTypes = new HashMap<>();
        for (Entry<DBSchemaIdentity, Set<SqlType>> entry : identity2Types.entrySet()) {
            DBSchemaIdentity identity = entry.getKey();
            Set<SqlType> sqlTypes = entry.getValue();
            if (CollectionUtils.isNotEmpty(sqlTypes)) {
                Set<DatabasePermissionType> permissionTypes = sqlTypes.stream().map(DatabasePermissionType::from)
                        .filter(Objects::nonNull).collect(Collectors.toSet());
                if (CollectionUtils.isNotEmpty(permissionTypes)) {
                    resource2PermissionTypes.put(
                            DBResource.from(connectionConfig, identity.getSchema(), identity.getTable()),
                            permissionTypes);
                }
            }
        }
        List<UnauthorizedDBResource> unauthorizedDBResource = dbResourcePermissionHelper
                .filterUnauthorizedDBResources(resource2PermissionTypes, false);
        if (CollectionUtils.isNotEmpty(unauthorizedDBResource)) {
            response.setUnauthorizedDBResources(unauthorizedDBResource);
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(@NonNull SqlExecuteResult response, @NonNull ConnectionSession session,
            @NonNull AsyncExecuteContext context) {}

    @Override
    protected String getExecuteStageName() {
        return SqlExecuteStages.DATABASE_PERMISSION_CHECK;
    }

}
