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
package com.oceanbase.odc.service.db;

import java.util.List;

import org.apache.commons.lang3.Validate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.plugin.schema.api.MViewLogExtensionPoint;
import com.oceanbase.odc.service.db.model.GenerateUpdateMViewLogDDLReq;
import com.oceanbase.odc.service.plugin.SchemaPluginUtil;
import com.oceanbase.tools.dbbrowser.model.DBMViewLogPurgeParameter;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedViewLog;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/7/15 10:23
 * @since: 4.4.0
 */
@Slf4j
@Service
@SkipAuthorize("inside connect session")
public class DBMaterializedViewLogService {

    public List<DBObjectIdentity> list(@NonNull ConnectionSession connectionSession, @NotEmpty String schemaName) {
        return connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<List<DBObjectIdentity>>) con -> getDBMViewLogExtensionPoint(
                        connectionSession).list(con, schemaName));
    }

    public DBMaterializedViewLog detail(@NonNull ConnectionSession connectionSession, @NotEmpty String schemaName,
            @NotEmpty String mViewLogName) {
        Validate.isTrue(
                connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                        .execute((ConnectionCallback<List<DBObjectIdentity>>) con -> getDBMViewLogExtensionPoint(
                                connectionSession).list(con, schemaName))
                        .stream().anyMatch(dbObjectIdentity -> mViewLogName.equals(dbObjectIdentity.getName())),
                "Materialized view log " + mViewLogName + " not existed in schema " + schemaName);
        return connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<DBMaterializedViewLog>) con -> getDBMViewLogExtensionPoint(
                        connectionSession)
                                .getDetail(con, schemaName, mViewLogName));
    }

    public Boolean purge(@NonNull ConnectionSession connectionSession, @NotEmpty String schemaName,
            @NotEmpty String mViewLogName) {
        Validate.isTrue(
                connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                        .execute((ConnectionCallback<List<DBObjectIdentity>>) con -> getDBMViewLogExtensionPoint(
                                connectionSession).list(con, schemaName))
                        .stream().anyMatch(dbObjectIdentity -> mViewLogName.equals(dbObjectIdentity.getName())),
                "Materialized view log " + mViewLogName + " not existed in schema " + schemaName);
        return connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<Boolean>) con -> getDBMViewLogExtensionPoint(connectionSession)
                        .purge(con, new DBMViewLogPurgeParameter(schemaName, mViewLogName)));
    }

    public String generateCreateTemplate(@NonNull ConnectionSession connectionSession,
            @NonNull DBMaterializedViewLog resource) {
        return connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<String>) con -> getDBMViewLogExtensionPoint(connectionSession)
                        .generateCreateTemplate(resource));
    }

    public String generateUpdateDDL(@NotNull ConnectionSession session,
            @NotNull GenerateUpdateMViewLogDDLReq req) {
        return session.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<String>) con -> getDBMViewLogExtensionPoint(session).generateUpdateDDL(con,
                        req.getPrevious(), req.getCurrent()));
    }

    private MViewLogExtensionPoint getDBMViewLogExtensionPoint(@NonNull ConnectionSession session) {
        return SchemaPluginUtil.getMViewLogExtension(session.getDialectType());
    }

}
