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
package com.oceanbase.odc.plugin.connect.api;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.pf4j.ExtensionPoint;

import com.oceanbase.odc.core.datasource.ConnectionInitializer;
import com.oceanbase.odc.core.shared.jdbc.JdbcUrlParser;

import lombok.NonNull;

/**
 * @author yaobin
 * @date 2023-04-14
 * @since 4.2.0
 */
public interface ConnectionExtensionPoint extends ExtensionPoint {

    String generateJdbcUrl(String host, Integer port, String defaultSchema, Map<String, String> jdbcParameters);

    String getDriverClassName();

    /**
     * If we need do some initialize on connection, eg: set a schema related current connection. these
     * ConnectionInitializers will be invoked follow the order in list, after the connection obtained
     * from datasource. otherwise return null.
     *
     * @return ConnectionInitializer list or null
     */
    List<ConnectionInitializer> getConnectionInitializers();

    TestResult test(String jdbcUrl, String username, String password, int queryTimeout);

    JdbcUrlParser getJdbcUrlParser(@NonNull String jdbcUrl) throws SQLException;

}
