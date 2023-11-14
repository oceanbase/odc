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

package com.oceanbase.odc.plugin.task.mysql.datatransfer.common;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.datasource.SingleConnectionDataSource;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.plugin.connect.mysql.MySQLConnectionExtension;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ConnectionInfo;

import lombok.NonNull;

public class DataSourceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("DataTransferLogger");

    private volatile static DataSourceManager instance;

    private final Map<ConnectionInfo, DataSource> dsCache = new ConcurrentHashMap<>();

    private DataSourceManager() {}

    public static DataSourceManager getInstance() {
        if (instance == null) {
            synchronized (DataSourceManager.class) {
                instance = new DataSourceManager();
            }
        }
        return instance;
    }

    public DataSource get(@NonNull ConnectionInfo key) {
        return dsCache.computeIfAbsent(key, this::createDs);
    }

    public void revoke(@NonNull ConnectionInfo key) throws Exception {
        DataSource ds = dsCache.getOrDefault(key, null);
        if (ds == null) {
            return;
        }
        if (ds instanceof Closeable) {
            ((Closeable) ds).close();
        }
        dsCache.remove(key);
    }

    private DataSource createDs(ConnectionInfo connectionInfo) {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        dataSource.setUsername(connectionInfo.getUserNameForConnect());
        dataSource.setPassword(connectionInfo.getPassword());
        dataSource.setDriverClassName(OdcConstants.MYSQL_DRIVER_CLASS_NAME);

        Map<String, String> jdbcUrlParams = new HashMap<>();
        jdbcUrlParams.put("connectTimeout", "5000");
        if (StringUtils.isNotBlank(connectionInfo.getProxyHost())
                && Objects.nonNull(connectionInfo.getProxyPort())) {
            jdbcUrlParams.put("socksProxyHost", connectionInfo.getProxyHost());
            jdbcUrlParams.put("socksProxyPort", connectionInfo.getProxyPort() + "");
        }
        String jdbcUrl = new MySQLConnectionExtension().generateJdbcUrl(connectionInfo.getHost(),
                connectionInfo.getPort(), connectionInfo.getSchema(), jdbcUrlParams);

        dataSource.setUrl(jdbcUrl);
        return dataSource;
    }

}
