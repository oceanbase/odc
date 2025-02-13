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
package com.oceanbase.odc.service.db.schema.syncer.object;

import java.sql.Connection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.iam.PermissionRepository;
import com.oceanbase.odc.metadb.iam.UserPermissionRepository;
import com.oceanbase.odc.plugin.connect.api.InformationExtensionPoint;
import com.oceanbase.odc.plugin.schema.api.TableExtensionPoint;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.feature.VersionDiffConfigService;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2024/8/23 14:33
 * @since: 4.3.3
 */
@Component
@Slf4j
public class DBExternalTableSyncer extends AbstractDBObjectSyncer<TableExtensionPoint> {

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private VersionDiffConfigService versionDiffConfigService;

    @Override
    protected void preDelete(@NonNull Set<Long> toBeDeletedIds) {
        List<PermissionEntity> permissions =
                permissionRepository.findByResourceTypeAndResourceIdIn(ResourceType.ODC_TABLE, toBeDeletedIds);
        Set<Long> permissionIds = permissions.stream().map(PermissionEntity::getId).collect(Collectors.toSet());
        permissionRepository.deleteByIds(permissionIds);
        userPermissionRepository.deleteByPermissionIds(permissionIds);
    }

    @Override
    public boolean supports(@NonNull DialectType dialectType, @NonNull Connection connection) {
        try {
            InformationExtensionPoint point =
                    ConnectionPluginUtil.getInformationExtension(dialectType);
            String databaseProductVersion = point.getDBVersion(connection);
            return versionDiffConfigService.isExternalTableSupported(dialectType, databaseProductVersion)
                    && getExtensionPoint(dialectType) != null;
        } catch (Exception e) {
            log.warn("check external table support failed", e);
            return false;
        }
    }

    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.EXTERNAL_TABLE;
    }

    @Override
    Set<String> getLatestObjectNames(@NonNull TableExtensionPoint extensionPoint,
            @NonNull Connection connection, @NonNull Database database) {
        return extensionPoint.list(connection, database.getName(), DBObjectType.EXTERNAL_TABLE).stream()
                .map(DBObjectIdentity::getName).collect(Collectors.toSet());
    }

    @Override
    Class<TableExtensionPoint> getExtensionPointClass() {
        return TableExtensionPoint.class;
    }
}
