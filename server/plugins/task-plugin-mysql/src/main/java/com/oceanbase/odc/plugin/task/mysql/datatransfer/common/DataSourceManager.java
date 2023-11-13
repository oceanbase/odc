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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oceanbase.odc.core.datasource.SingleConnectionDataSource;
import com.oceanbase.odc.plugin.connect.mysql.MySQLConnectionExtension;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ConnectionInfo;
import com.oceanbase.odc.plugin.task.obmysql.datatransfer.util.ConnectionUtil;

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
        return ConnectionUtil.getDataSource(connectionInfo, "");
    }

}
