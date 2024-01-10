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

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.session.factory.OBConsoleDataSourceFactory;
import com.oceanbase.tools.migrator.common.configure.DataSourceInfo;
import com.oceanbase.tools.migrator.common.enums.DataBaseType;
import com.oceanbase.tools.migrator.common.util.EncryptUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/10/17 10:32
 * @Descripition:
 */
@Slf4j
public class DataSourceInfoBuilder {

    public static DataSourceInfo build(ConnectionConfig connectionConfig) {
        DataSourceInfo dataSourceInfo = new DataSourceInfo();
        dataSourceInfo.setDatabaseName(connectionConfig.getDefaultSchema());
        if (StringUtils.isNotEmpty(connectionConfig.getPassword())) {
            dataSourceInfo.setPassword(connectionConfig.getPassword());
        }
        switch (connectionConfig.getDialectType()) {
            case MYSQL: {
                dataSourceInfo.setIp(connectionConfig.getHost());
                dataSourceInfo.setPort(connectionConfig.getPort());
                dataSourceInfo.setFullUserName(connectionConfig.getUsername());
                dataSourceInfo.setDatabaseType(DataBaseType.MYSQL);
                break;
            }
            case OB_MYSQL: {
                dataSourceInfo
                        .setObProxy(String.format("%s:%s", connectionConfig.getHost(), connectionConfig.getPort()));
                dataSourceInfo
                        .setFullUserName(OBConsoleDataSourceFactory.getUsername(connectionConfig));
                dataSourceInfo.setDatabaseType(DataBaseType.OCEANBASEV10);
                dataSourceInfo.setSysUser(connectionConfig.getSysTenantUsername());
                dataSourceInfo.setClusterName(connectionConfig.getClusterName());
                if (StringUtils.isNotEmpty(connectionConfig.getSysTenantPassword())) {
                    try {
                        dataSourceInfo.setSysPassword(EncryptUtils.encode(connectionConfig.getSysTenantPassword()));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                dataSourceInfo.setSysDatabaseName("oceanbase");
                break;
            }
            default:
                log.warn(String.format("Unsupported datasource type:%s", connectionConfig.getDialectType()));
                throw new UnsupportedException(
                        String.format("Unsupported datasource type:%s", connectionConfig.getDialectType()));
        }
        return dataSourceInfo;
    }
}
