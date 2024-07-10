/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.plugin.connect.postgres;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.oceanbase.odc.core.shared.jdbc.HostAddress;
import com.oceanbase.odc.core.shared.jdbc.JdbcUrlParser;

import lombok.NonNull;

public class PostgresJdbcUrlParser  implements JdbcUrlParser {
    private final String POSTGRES_JDBC_PREFIX = "jdbc:postgresql://";
    private String jdbcUrl;
    private List<HostAddress> addresses;
    private Map<String, Object> parameters;
    private String userName;


    public PostgresJdbcUrlParser(@NonNull String jdbcUrl, String userName) throws SQLException{
        if (jdbcUrl == null || !jdbcUrl.startsWith(POSTGRES_JDBC_PREFIX)) {
            throw new IllegalArgumentException("Invalid PostgreSQL JDBC URL.");
        }
        this.userName = userName;
        this.jdbcUrl = jdbcUrl;
        this.addresses = parseHostAndPort(jdbcUrl);
        this.parameters = parseParameters(jdbcUrl);

    }

    private List<HostAddress> parseHostAndPort(String jdbcUrl) {
        String urlWithoutJdbcPrefix = jdbcUrl.substring(POSTGRES_JDBC_PREFIX.length());

        URI uri = null;
        try {
            uri = new URI("postgresql://" + urlWithoutJdbcPrefix);
        } catch (URISyntaxException e) {
            return null;
        }
        HostAddress hostAddress = new HostAddress();
        hostAddress.setHost(uri.getHost());
        hostAddress.setPort(uri.getPort());
        return Collections.singletonList(hostAddress);
    }

    private Map<String, Object> parseParameters(String jdbcUrl) {
        Map<String, Object> paramsMap = new HashMap<>();
        String urlWithoutJdbcPrefix = jdbcUrl.substring(POSTGRES_JDBC_PREFIX.length());
        URI uri = null;
        try {
            uri = new URI("postgresql://" + urlWithoutJdbcPrefix);
        } catch (URISyntaxException e) {
            return null;
        }

        if (uri.getQuery() != null) {
            String[] params = uri.getQuery().split("&");
            for (String param : params) {
                String[] keyValue = param.split("=", 2);
                String key = keyValue[0];
                String value = keyValue.length > 1 ? keyValue[1] : ""; // Handle parameters without value
                paramsMap.put(key, value);
            }
        }
        return paramsMap;
    }


    @Override
    public List<HostAddress> getHostAddresses() {
        return this.addresses;
    }

    @Override
    public String getSchema() {
        return this.userName;
    }

    @Override
    public Map<String, Object> getParameters() {
        return this.parameters;
    }
}
