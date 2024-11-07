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
package com.oceanbase.odc.service.session.factory;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.datasource.DataSourceFactory;
import com.oceanbase.odc.core.datasource.SingleConnectionDataSource;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.plugin.connect.api.ConnectionExtensionPoint;
import com.oceanbase.odc.plugin.connect.model.JdbcUrlProperty;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;

import lombok.NonNull;

/**
 * Used for the construction of the database connection pool corresponding to the user under the sys
 * tenant
 *
 * @author yh263208
 * @date 2021-11-17 11:53
 * @since ODC_release_3.2.2
 * @see DataSourceFactory
 */
public class OBSysUserDataSourceFactory implements DataSourceFactory {

    private static final Map<String, String> JDBC_URL_PARAMS = new HashMap<>();
    private final ConnectionExtensionPoint connectionExtensionPoint;

    static {
        JDBC_URL_PARAMS.put("socketTimeout", "8000");
        JDBC_URL_PARAMS.put("connectTimeout", "8000");
    }

    private final ConnectionConfig connectionConfig;

    public OBSysUserDataSourceFactory(@NonNull ConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
        this.connectionExtensionPoint = ConnectionPluginUtil.getConnectionExtension(DialectType.OB_MYSQL);
    }

    @Override
    public DataSource getDataSource() {
        String username = connectionConfig.getDialectType().isOceanbase() ? getUsernameWithTenantAndCluster()
                : connectionConfig.getUsername();
        if (username == null) {
            throw new NullPointerException("Sys username can not be null");
        }
        String jdbcUrl = getJdbcUrl();
        String password = getPassword();
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setAutoCommit(true);
        dataSource.setDriverClassName(connectionExtensionPoint.getDriverClassName());
        return dataSource;
    }

    @Override
    public DialectType getDialectType() {
        return DialectType.OB_MYSQL;
    }

    protected String getUsernameWithTenantAndCluster() {
        String username = connectionConfig.getSysTenantUsername();
        if (StringUtils.isEmpty(username)) {
            return null;
        }
        username = username + "@sys";
        if (StringUtils.isNotBlank(connectionConfig.getClusterName())) {
            username = username + "#" + connectionConfig.getClusterName();
        }
        return username;
    }

    protected String getPassword() {
        return connectionConfig.getSysTenantPassword();
    }

    protected String getJdbcUrl() {
        return connectionExtensionPoint
                .generateJdbcUrl(new JdbcUrlProperty(this.connectionConfig.getHost(), this.connectionConfig.getPort(),
                        null, JDBC_URL_PARAMS));
    }

}
