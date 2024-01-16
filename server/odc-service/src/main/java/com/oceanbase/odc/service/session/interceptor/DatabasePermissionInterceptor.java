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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.sql.execute.SqlExecuteStages;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.permission.database.DatabasePermissionService;
import com.oceanbase.odc.service.permission.database.model.UnauthorizedDatabase;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteReq;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteResp;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.odc.service.session.model.SqlTuplesWithViolation;
import com.oceanbase.odc.service.session.util.SchemaExtractor;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/8/8 22:23
 * @Description: []
 */
@Slf4j
@Component
public class DatabasePermissionInterceptor extends BaseTimeConsumingInterceptor {

    @Autowired
    private DatabasePermissionService databasePermissionService;
    @Autowired
    private AuthenticationFacade authenticationFacade;

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
        Map<String, Set<SqlType>> schemaName2SqlTypes = SchemaExtractor.listSchemaName2SqlTypes(
                response.getSqls().stream().map(SqlTuplesWithViolation::getSqlTuple).collect(Collectors.toList()),
                session.getDialectType());
        List<UnauthorizedDatabase> unauthorizedDatabases =
                databasePermissionService.filterUnauthorizedDatabases(schemaName2SqlTypes, connectionConfig.getId());
        if (CollectionUtils.isNotEmpty(unauthorizedDatabases)) {
            response.setUnauthorizedDatabases(unauthorizedDatabases);
            return false;
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
