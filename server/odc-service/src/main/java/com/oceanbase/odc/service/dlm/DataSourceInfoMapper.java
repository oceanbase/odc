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

package com.oceanbase.odc.service.dlm;

import java.util.Collections;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.plugin.connect.model.JdbcUrlProperty;
import com.oceanbase.odc.plugin.connect.model.oracle.UserRole;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;
import com.oceanbase.odc.service.session.factory.OBConsoleDataSourceFactory;
import com.oceanbase.tools.migrator.common.configure.DataSourceInfo;
import com.oceanbase.tools.migrator.common.enums.DatasourceType;
import com.oceanbase.tools.migrator.datasource.fs.FileFormat;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/10/17 10:32
 * @Descripition:
 */
@Slf4j
public class DataSourceInfoMapper {

    public static ConnectionConfig toConnectionConfig(DataSourceInfo dataSourceInfo) {
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setDefaultSchema(dataSourceInfo.getDatabaseName());
        connectionConfig.setPassword(dataSourceInfo.getPassword());
        connectionConfig.setHost(dataSourceInfo.getHost());
        connectionConfig.setPort(dataSourceInfo.getPort());
        connectionConfig.setUsername(dataSourceInfo.getUsername());
        connectionConfig.setType(ConnectType.valueOf(dataSourceInfo.getType().name()));
        return connectionConfig;
    }


    public static DataSourceInfo toDataSourceInfo(ConnectionConfig connectionConfig, String schemaName) {
        DataSourceInfo dataSourceInfo = new DataSourceInfo();
        dataSourceInfo.setDatabaseName(connectionConfig.getDefaultSchema());
        dataSourceInfo.setQueryTimeout(connectionConfig.queryTimeoutSeconds());
        if (StringUtils.isNotEmpty(connectionConfig.getPassword())) {
            dataSourceInfo.setPassword(connectionConfig.getPassword());
        }
        dataSourceInfo.setHost(connectionConfig.getHost());
        dataSourceInfo.setPort(connectionConfig.getPort());
        switch (connectionConfig.getDialectType()) {
            case DORIS:
            case MYSQL: {
                dataSourceInfo.setUsername(connectionConfig.getUsername());
                dataSourceInfo.setType(DatasourceType.MYSQL);
                break;
            }
            case OB_MYSQL: {
                dataSourceInfo
                        .setUsername(OBConsoleDataSourceFactory.getUsername(connectionConfig));
                dataSourceInfo.setType(DatasourceType.OB_MYSQL);
                break;
            }
            case OB_ORACLE:
                dataSourceInfo.setUsername(OBConsoleDataSourceFactory.getUsername(connectionConfig));
                dataSourceInfo.setType(DatasourceType.OB_ORACLE);
                break;
            case POSTGRESQL:
                dataSourceInfo.setUsername(connectionConfig.getUsername());
                connectionConfig.setDefaultSchema(schemaName);
                String jdbcUrl = getJdbcUrl(connectionConfig) + "&stringtype=unspecified";
                dataSourceInfo.setUrl(jdbcUrl);
                dataSourceInfo.setType(DatasourceType.POSTGRESQL);
                break;
            case ORACLE:
                dataSourceInfo.setUrl(getJdbcUrl(connectionConfig));
                dataSourceInfo.setType(DatasourceType.ORACLE);
                dataSourceInfo.setUsername(getOracleUsername(connectionConfig));
                break;
            case FILE_SYSTEM:
                dataSourceInfo.setHost(connectionConfig.getHost());
                dataSourceInfo.setType(DatasourceType.valueOf(connectionConfig.getType().name()));
                dataSourceInfo.setUsername(connectionConfig.getUsername());
                dataSourceInfo.setPassword(connectionConfig.getPassword());
                dataSourceInfo.setFileFormat(FileFormat.CSV);
                dataSourceInfo.setRegion(connectionConfig.getRegion());
                dataSourceInfo.setDefaultCharset("UTF-8");
                break;
            default:
                log.warn(String.format("Unsupported datasource type:%s", connectionConfig.getDialectType()));
                throw new UnsupportedException(
                        String.format("Unsupported datasource type:%s", connectionConfig.getDialectType()));
        }
        return dataSourceInfo;
    }

    private static String getJdbcUrl(ConnectionConfig connectionConfig) {
        JdbcUrlProperty jdbcUrlProperty = new JdbcUrlProperty(connectionConfig.getHost(), connectionConfig.getPort(),
                connectionConfig.getDefaultSchema(), Collections.emptyMap(),
                connectionConfig.getSid(),
                connectionConfig.getServiceName(), connectionConfig.getCatalogName());
        return ConnectionPluginUtil.getConnectionExtension(connectionConfig.getDialectType())
                .generateJdbcUrl(jdbcUrlProperty);
    }

    private static String getOracleUsername(ConnectionConfig connectionConfig) {
        return connectionConfig.getUserRole() == UserRole.SYSDBA ? connectionConfig.getUsername() + " as sysdba"
                : connectionConfig.getUsername();
    }
}
