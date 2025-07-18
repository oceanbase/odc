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
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.plugin.connect.api.InformationExtensionPoint;
import com.oceanbase.odc.plugin.schema.api.MViewLogExtensionPoint;
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
 * @date: 2025/7/18 14:22
 * @since: 4.4.0
 */
@Component
@Slf4j
public class DBMViewLogSyncer extends AbstractDBObjectSyncer<MViewLogExtensionPoint> {

    @Autowired
    private VersionDiffConfigService versionDiffConfigService;

    @Override
    Set<String> getLatestObjectNames(@NonNull MViewLogExtensionPoint extensionPoint, @NonNull Connection connection,
            @NonNull Database database) {
        return extensionPoint.list(connection, database.getName()).stream().map(DBObjectIdentity::getName)
                .collect(Collectors.toSet());
    }

    @Override
    Class<MViewLogExtensionPoint> getExtensionPointClass() {
        return MViewLogExtensionPoint.class;
    }

    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.MATERIALIZED_VIEW_LOG;
    }

    @Override
    public boolean supports(@NonNull DialectType dialectType, @NonNull Connection connection) {
        try {
            InformationExtensionPoint point =
                    ConnectionPluginUtil.getInformationExtension(dialectType);
            String databaseProductVersion = point.getDBVersion(connection);
            return versionDiffConfigService.isMViewLogSupported(dialectType, databaseProductVersion)
                    && getExtensionPoint(dialectType) != null;
        } catch (Exception e) {
            log.warn("check materialized view log support failed", e);
            return false;
        }
    }

}
