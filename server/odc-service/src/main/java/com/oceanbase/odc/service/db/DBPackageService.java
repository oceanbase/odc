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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.plugin.schema.api.PackageExtensionPoint;
import com.oceanbase.odc.service.common.model.ResourceSql;
import com.oceanbase.odc.service.plugin.SchemaPluginUtil;
import com.oceanbase.odc.service.session.ConnectConsoleService;
import com.oceanbase.tools.dbbrowser.model.DBPLObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBPackage;

import lombok.NonNull;

@Service
@SkipAuthorize("inside connect session")
public class DBPackageService {
    @Autowired
    private ConnectConsoleService consoleService;

    public List<DBPackage> list(ConnectionSession connectionSession, String dbName) {
        return connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<List<DBPLObjectIdentity>>) con -> getPackageExtensionPoint(
                        connectionSession).list(con, dbName))
                .stream()
                .filter(i -> !StringUtils.equalsIgnoreCase(i.getName(), OdcConstants.PL_DEBUG_PACKAGE))
                .map(item -> {
                    DBPackage dbPackage = new DBPackage();
                    dbPackage.setPackageName(item.getName());
                    dbPackage.setErrorMessage(item.getErrorMessage());
                    dbPackage.setStatus(item.getStatus());
                    return dbPackage;
                }).collect(Collectors.toList());
    }

    public DBPackage detail(ConnectionSession connectionSession, String schemaName, String packageName) {
        return connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<DBPackage>) con -> getPackageExtensionPoint(connectionSession)
                        .getDetail(con, schemaName, packageName));
    }

    public ResourceSql getCreateSql(@NonNull ConnectionSession session,
            @NonNull DBPackage resource) {
        return ResourceSql.ofSql(session.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<String>) con -> getPackageExtensionPoint(session)
                        .generateCreateTemplate(con, resource)));
    }

    private PackageExtensionPoint getPackageExtensionPoint(@NonNull ConnectionSession session) {
        return SchemaPluginUtil.getPackageExtension(session.getDialectType());
    }

}
