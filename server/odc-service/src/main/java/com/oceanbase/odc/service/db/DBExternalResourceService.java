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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.plugin.schema.api.ExternalResourceExtensionPoint;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.permission.DBResourcePermissionHelper;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.plugin.SchemaPluginUtil;
import com.oceanbase.tools.dbbrowser.model.DBExternalResource;
import com.oceanbase.tools.dbbrowser.model.DBExternalResourceUploadParam;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;

import lombok.NonNull;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/8/22 15:57
 * @since: 4.4.1
 */
@Service
@SkipAuthorize("inside connect session")
public class DBExternalResourceService {

    @Autowired
    private DatabaseRepository databaseRepository;
    @Autowired
    private DBResourcePermissionHelper permissionHelper;

    public Boolean upload(ConnectionSession connectionSession, DBExternalResourceUploadParam param, MultipartFile file)
            throws IOException {
        IOException[] ioEx = new IOException[1];
        try (InputStream inputStream = file.getInputStream()) {
            param.setInputStream(inputStream);
            Boolean execute = connectionSession.getSyncJdbcExecutor(
                    ConnectionSessionConstants.BACKEND_DS_KEY)
                    .execute((ConnectionCallback<Boolean>) con -> {
                        Boolean status = null;
                        try {
                            status = getExternalResourceExtensionPoint(connectionSession).upload(
                                    con, param);
                        } catch (IOException e) {
                            ioEx[0] = e;
                        }
                        return status;
                    });
            if (ioEx[0] != null) {
                throw ioEx[0];
            }
            return execute;
        }
    }

    public InputStreamResource download(ConnectionSession connectionSession, String schemaName, String resourceName)
            throws IOException {
        IOException[] ioEx = new IOException[1];
        InputStream resourceStream = connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY).execute((ConnectionCallback<InputStream>) con -> {
                    InputStream stream = null;
                    try {
                        stream = getExternalResourceExtensionPoint(connectionSession).download(con, schemaName,
                                resourceName);
                    } catch (IOException e) {
                        ioEx[0] = e;
                    }
                    return stream;
                });
        if (ioEx[0] != null) {
            if (resourceStream != null) {
                resourceStream.close();
            }
            throw ioEx[0];
        }
        return new InputStreamResource(resourceStream);
    }

    public List<DBObjectIdentity> list(ConnectionSession connectionSession, String dbName) {
        return connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<List<DBObjectIdentity>>) con -> getExternalResourceExtensionPoint(
                        connectionSession).list(con, dbName));
    }

    public DBExternalResource detail(ConnectionSession connectionSession, String dbName, String resourceName)
            throws IOException {
        IOException[] ioEx = new IOException[1];
        DBExternalResource detail = connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<DBExternalResource>) con -> {
                    DBExternalResource resource = null;
                    try {
                        resource = getExternalResourceExtensionPoint(connectionSession)
                                .getDetail(con,
                                        dbName, resourceName,
                                        StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        ioEx[0] = e;
                    }
                    return resource;
                });
        if (ioEx[0] != null) {
            throw ioEx[0];
        }
        return detail;
    }

    public Boolean drop(ConnectionSession connectionSession, String dbName, String resourceName) {
        Long databaseId = getDatabaseIdByConnectionSession(connectionSession);
        permissionHelper.checkDBPermissions(Collections.singleton(databaseId),
                Collections.singleton(DatabasePermissionType.CHANGE));
        return connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<Boolean>) con -> getExternalResourceExtensionPoint(connectionSession)
                        .drop(con, dbName, resourceName));
    }

    private ExternalResourceExtensionPoint getExternalResourceExtensionPoint(@NonNull ConnectionSession session) {
        return SchemaPluginUtil.getExternalResourceExtensionPoint(session.getDialectType());
    }

    private Long getDatabaseIdByConnectionSession(@NonNull ConnectionSession session) {
        ConnectionConfig connConfig = (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(session);
        String schemaName = ConnectionSessionUtil.getCurrentSchema(session);
        DatabaseEntity databaseEntity =
                databaseRepository.findByConnectionIdAndNameAndExisted(connConfig.getId(), schemaName, true)
                        .orElseThrow(() -> new NotFoundException(ResourceType.ODC_DATABASE, "name", schemaName));
        return databaseEntity.getId();
    }

}
