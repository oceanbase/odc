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
package com.oceanbase.odc.common.jdbc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oceanbase.odc.common.util.StringUtils;

import lombok.Data;
import lombok.NonNull;

public class JdbcUrlParser {
    private static final String REGEX =
            "jdbc:(mysql|oracle|oceanbase|postgresql|sqlserver)://([^/:]+)(?::([0-9]+))?/([^?;]*)";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    /**
     * Parses a JDBC URL and returns a ConnectionInfo object.
     *
     * @param jdbcUrl the JDBC URL to parse
     * @return a ConnectionInfo object containing the parsed information
     * @throws IllegalArgumentException if the JDBC URL is invalid or the database type is not supported
     */
    public static ConnectionInfo parse(@NonNull String jdbcUrl) {
        Matcher matcher = PATTERN.matcher(jdbcUrl);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid jdbc url: " + jdbcUrl + ". Expected pattern: " + REGEX);
        }
        ConnectionInfo connectionInfo = new ConnectionInfo();
        connectionInfo.type = matcher.group(1);
        connectionInfo.host = matcher.group(2);
        String portString = matcher.group(3);
        connectionInfo.port =
                StringUtils.isBlank(portString) ? getDefaultPort(connectionInfo.type) : parsePort(portString);
        connectionInfo.database = matcher.group(4);
        return connectionInfo;
    }

    private static int parsePort(String portString) {
        try {
            return Integer.parseInt(portString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port number: " + portString, e);
        }
    }

    private static int getDefaultPort(String dbType) {
        switch (dbType) {
            case "mysql":
                return 3306;
            case "oracle":
                return 1521;
            case "postgresql":
                return 5432;
            case "sqlserver":
                return 1433;
            case "oceanbase":
                return 2883;
            default:
                throw new IllegalArgumentException("Unsupported database type");
        }
    }

    @Data
    public static class ConnectionInfo {
        private String type;
        private String host;
        private Integer port;
        private String database;
    }
}
