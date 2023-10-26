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

package com.oceanbase.odc.plugin.task.obmysql.datatransfer.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.datasource.SingleConnectionDataSource;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ConnectionInfo;

public class ConnectionUtil {

    public static SingleConnectionDataSource getDataSource(ConnectionInfo connectionInfo, String schema) {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        dataSource.setUsername(connectionInfo.getUserNameForConnect());
        dataSource.setPassword(connectionInfo.getPassword());
        dataSource.setUrl(getJdbcUrl(connectionInfo, schema));
        dataSource.setDriverClassName(PluginUtil.getConnectionExtension(connectionInfo).getDriverClassName());
        return dataSource;
    }

    public static String getJdbcUrl(ConnectionInfo connectionInfo, String schema) {
        Map<String, String> jdbcUrlParams = new HashMap<>();
        jdbcUrlParams.put("connectTimeout", "5000");
        if (StringUtils.isNotBlank(connectionInfo.getProxyHost())
                && Objects.nonNull(connectionInfo.getProxyPort())) {
            jdbcUrlParams.put("socksProxyHost", connectionInfo.getProxyHost());
            jdbcUrlParams.put("socksProxyPort", connectionInfo.getProxyPort() + "");
        }
        return PluginUtil.getConnectionExtension(connectionInfo).generateJdbcUrl(connectionInfo.getHost(),
                connectionInfo.getPort(), schema, jdbcUrlParams);
    }

}
