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
import java.util.Properties;

import org.pf4j.ExtensionPoint;

import com.oceanbase.odc.core.datasource.ConnectionInitializer;
import com.oceanbase.odc.core.shared.jdbc.JdbcUrlParser;
import com.oceanbase.odc.plugin.connect.model.ConnectionPropertiesBuilder;

import lombok.NonNull;

/**
 * @author yaobin
 * @date 2023-04-14
 * @since 4.2.0
 */
public interface ConnectionExtensionPoint extends ExtensionPoint {

    /**
     * @param properties Properties required by jdbcURL, such as HOST, PORT and DEFAULT_SCHEMA, see
     *        {@link ConnectionPropertiesBuilder}
     * @param jdbcParameters jdbc parameters.
     *
     * @return jdbcURL
     */
    String generateJdbcUrl(Properties properties, Map<String, String> jdbcParameters);

    String getDriverClassName();

    /**
     * If we need do some initialize on connection, eg: set a schema related current connection. these
     * ConnectionInitializers will be invoked follow the order in list, after the connection obtained
     * from datasource. otherwise return null.
     *
     * @return ConnectionInitializer list or null
     */
    List<ConnectionInitializer> getConnectionInitializers();

    /**
     * Get connection information based on jdbcUrl and the username of the current connection
     *
     * @param jdbcUrl jdbc url.
     * @param userName jdbc url.
     * @return {@link JdbcUrlParser}
     */
    JdbcUrlParser getConnectionInfo(@NonNull String jdbcUrl, String userName) throws SQLException;

    /**
     * @param properties Properties required by test connection, such as USER, PASSWORD, see
     *        {@link ConnectionPropertiesBuilder}
     * @param queryTimeout query timeout.
     *
     * @return test connection result
     */
    TestResult test(String jdbcUrl, Properties properties, int queryTimeout);
}
