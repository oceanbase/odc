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
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.plugin.schema.api.ExternalResourceExtensionPoint;
import com.oceanbase.odc.service.common.util.WebResponseUtils;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.model.DBExternalResourceProperties;
import com.oceanbase.odc.service.objectstorage.ObjectStorageExecutor;
import com.oceanbase.odc.service.permission.DBResourcePermissionHelper;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.plugin.SchemaPluginUtil;
import com.oceanbase.tools.dbbrowser.model.DBExternalResource;
import com.oceanbase.tools.dbbrowser.model.DBExternalResourceDetailParam;
import com.oceanbase.tools.dbbrowser.model.DBExternalResourceStreamHolder;
import com.oceanbase.tools.dbbrowser.model.DBExternalResourceType;
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
    @Autowired
    private DBExternalResourceProperties properties;
    private ObjectStorageExecutor objectStorageExecutor;

    @PostConstruct
    public void init() {
        objectStorageExecutor = new ObjectStorageExecutor(properties.getConcurrencyLimitNumbers(),
                properties.getWaitLockTimeoutMillSeconds());
    }

    public Boolean upload(ConnectionSession connectionSession, DBExternalResourceUploadParam param,
            MultipartFile file) {
        if (list(connectionSession, param.getSchemaName()).stream()
                .anyMatch(o -> StringUtils.equals(o.getName(), param.getName()))) {
            throw new IllegalArgumentException(String.format("Resource %s already exists", param.getName()));
        }
        Long databaseId = getDatabaseIdByConnectionSession(connectionSession);
        permissionHelper.checkDBPermissions(Collections.singleton(databaseId),
                Collections.singleton(DatabasePermissionType.CHANGE));
        if (file.getSize() > properties.getUploadLimitBytes()) {
            throw new IllegalArgumentException(
                    String.format("Resource is too large, the maximum size of the uploaded file cannot exceed %d bytes",
                            properties.getUploadLimitBytes()));
        }
        return objectStorageExecutor.concurrentSafeExecute(() -> {
            try (InputStream inputStream = file.getInputStream()) {
                param.setInputStream(inputStream);
                return connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                        .execute((ConnectionCallback<Boolean>) con -> {
                            try {
                                return getExternalResourceExtensionPoint(connectionSession).upload(con, param);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public ResponseEntity<InputStreamResource> download(ConnectionSession connectionSession, String schemaName,
            String resourceName)
            throws IOException {
        if (list(connectionSession, schemaName).stream()
                .allMatch(o -> !StringUtils.equals(o.getName(), resourceName))) {
            throw new IllegalArgumentException(String.format("Resource %s does not exist", resourceName));
        }
        DBExternalResourceStreamHolder holder = connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY).execute(
                        (ConnectionCallback<DBExternalResourceStreamHolder>) con -> DBExternalResourceService.this
                                .getExternalResourceExtensionPoint(connectionSession).download(
                                        con, schemaName, resourceName));
        if (holder.getTotalSize() > properties.getDownloadLimitBytes()) {
            holder.close();
            throw new IllegalStateException(String.format(
                    "Resource is too large, the maximum size of the downloaded file cannot exceed %d bytes",
                    properties.getUploadLimitBytes()));
        }
        return WebResponseUtils.getFileAttachmentResponseEntity(new InputStreamResource(holder.getInputStream()),
                generateFileName(schemaName, resourceName, holder.getType()));
    }

    public List<DBObjectIdentity> list(ConnectionSession connectionSession, String dbName) {
        return connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<List<DBObjectIdentity>>) con -> getExternalResourceExtensionPoint(
                        connectionSession).list(con, dbName));
    }

    public DBExternalResource detail(ConnectionSession connectionSession, DBExternalResourceDetailParam param) {
        if (list(connectionSession, param.getSchemaName()).stream()
                .allMatch(o -> !StringUtils.equals(o.getName(), param.getName()))) {
            throw new IllegalArgumentException(String.format("Resource %s does not exist", param.getName()));
        }
        if (param.getSupportViewBytes() > properties.getGetContentLimitBytes()) {
            param.setSupportViewBytes(properties.getGetContentLimitBytes());
        }
        // The character set is based on the front-end input. If not available, follow the default
        // configuration.
        if (param.getCharset() == null) {
            param.setCharset(properties.getCharset());
        }
        return connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<DBExternalResource>) con -> {
                    return getExternalResourceExtensionPoint(connectionSession)
                            .getDetail(con, param);
                });
    }

    public Boolean drop(ConnectionSession connectionSession, String dbName, String resourceName) {
        if (list(connectionSession, dbName).stream().allMatch(o -> !StringUtils.equals(o.getName(), resourceName))) {
            throw new IllegalArgumentException(String.format("Resource %s does not exist", resourceName));
        }
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

    private String generateFileName(String schemaName, String resourceName, DBExternalResourceType type) {
        if (DBExternalResourceType.JAVA_JAR == type) {
            return schemaName + "_" + resourceName + ".jar";
        } else if (DBExternalResourceType.PYTHON_PY == type) {
            return schemaName + "_" + resourceName + ".py";
        } else {
            throw new UnsupportedException(String.format("unsupported resource type %s", type));
        }
    }

}
