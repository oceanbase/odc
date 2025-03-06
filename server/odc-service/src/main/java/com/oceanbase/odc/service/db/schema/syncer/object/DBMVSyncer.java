/*
 * Copyright (c) 2025 OceanBase.
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
import com.oceanbase.odc.plugin.schema.api.MVExtensionPoint;
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
 * @date: 2025/3/6 14:26
 * @since: 4.3.4
 */
@Component
@Slf4j
public class DBMVSyncer extends AbstractDBObjectSyncer<MVExtensionPoint>{

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private VersionDiffConfigService versionDiffConfigService;

    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.MATERIALIZED_VIEW;
    }

    @Override
    Set<String> getLatestObjectNames(@NonNull MVExtensionPoint extensionPoint, @NonNull Connection connection,
        @NonNull Database database) {
        return extensionPoint.list(connection, database.getName()).stream().map(DBObjectIdentity::getName)
            .collect(Collectors.toSet());
    }

    @Override
    Class<MVExtensionPoint> getExtensionPointClass() {
        return MVExtensionPoint.class;
    }

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
            return versionDiffConfigService.isMVSupported(dialectType, databaseProductVersion)
                   && getExtensionPoint(dialectType) != null;
        } catch (Exception e) {
            log.warn("check external table support failed", e);
            return false;
        }
    }
}
