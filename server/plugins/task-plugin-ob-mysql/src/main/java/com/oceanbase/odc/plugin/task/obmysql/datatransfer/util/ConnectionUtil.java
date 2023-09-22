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

import com.oceanbase.odc.core.datasource.SingleConnectionDataSource;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;

public class ConnectionUtil {

    public static SingleConnectionDataSource getDataSource(DataTransferConfig config) {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        dataSource.setUrl(config.getConnectionInfo().getJdbcUrl());
        dataSource.setUsername(config.getConnectionInfo().getUserNameForConnect());
        dataSource.setPassword(config.getConnectionInfo().getPassword());
        dataSource.setDriverClassName(PluginUtil.getConnectionExtension(config).getDriverClassName());
        return dataSource;
    }

}
