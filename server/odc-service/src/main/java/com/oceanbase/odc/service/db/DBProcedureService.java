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
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.plugin.schema.api.ProcedureExtensionPoint;
import com.oceanbase.odc.service.common.model.ResourceSql;
import com.oceanbase.odc.service.plugin.SchemaPluginUtil;
import com.oceanbase.odc.service.session.ConnectConsoleService;
import com.oceanbase.tools.dbbrowser.model.DBPLObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;

import lombok.NonNull;

@Service
@SkipAuthorize("inside connect session")
public class DBProcedureService {
    @Autowired
    private ConnectConsoleService consoleService;

    public List<DBProcedure> list(ConnectionSession connectionSession, String dbName) {
        return connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<List<DBPLObjectIdentity>>) con -> getProcedureExtensionPoint(
                        connectionSession).list(con, dbName))
                .stream().map(
                        item -> {
                            DBProcedure procedure = new DBProcedure();
                            procedure.setProName(item.getName());
                            procedure.setErrorMessage(item.getErrorMessage());
                            procedure.setStatus(item.getStatus());
                            return procedure;
                        })
                .collect(Collectors.toList());
    }

    public DBProcedure detail(ConnectionSession connectionSession, String dbName, String proName) {
        return connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<DBProcedure>) con -> getProcedureExtensionPoint(connectionSession)
                        .getDetail(con, dbName, proName));
    }

    public ResourceSql getCreateSql(@NonNull ConnectionSession session,
            @NonNull DBProcedure resource) {
        return ResourceSql.ofSql(session.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<String>) con -> getProcedureExtensionPoint(session)
                        .generateCreateTemplate(resource)));
    }

    private ProcedureExtensionPoint getProcedureExtensionPoint(@NonNull ConnectionSession session) {
        return SchemaPluginUtil.getProcedureExtension(session.getDialectType());
    }

}
