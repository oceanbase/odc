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
import com.oceanbase.odc.plugin.schema.api.TypeExtensionPoint;
import com.oceanbase.odc.service.common.model.ResourceSql;
import com.oceanbase.odc.service.plugin.SchemaPluginUtil;
import com.oceanbase.odc.service.session.ConnectConsoleService;
import com.oceanbase.tools.dbbrowser.model.DBPLObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBType;

import lombok.NonNull;

/**
 * @author wenniu.ly
 * @date 2020/12/23
 */
@Service
@SkipAuthorize("inside connect session")
public class DBTypeService {
    @Autowired
    private ConnectConsoleService consoleService;

    public List<DBType> list(ConnectionSession connectionSession, String dbName) {
        return connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<List<DBPLObjectIdentity>>) con -> getTypeExtensionPoint(connectionSession)
                        .list(con, dbName))
                .stream()
                .map(item -> {
                    DBType type = new DBType();
                    type.setTypeName(item.getName());
                    type.setStatus(item.getStatus());
                    type.setErrorMessage(item.getErrorMessage());
                    return type;
                }).collect(Collectors.toList());
    }

    public DBType detail(ConnectionSession connectionSession, String schemaName, String typeName) {
        return connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<DBType>) con -> getTypeExtensionPoint(connectionSession).getDetail(con,
                        schemaName, typeName));
    }

    public ResourceSql generateCreateSql(@NonNull ConnectionSession session,
            @NonNull DBType unit) {
        return ResourceSql.ofSql(session.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<String>) con -> getTypeExtensionPoint(session)
                        .generateCreateTemplate(unit)));
    }

    private TypeExtensionPoint getTypeExtensionPoint(@NonNull ConnectionSession session) {
        return SchemaPluginUtil.getTypeExtension(session.getDialectType());
    }

}
