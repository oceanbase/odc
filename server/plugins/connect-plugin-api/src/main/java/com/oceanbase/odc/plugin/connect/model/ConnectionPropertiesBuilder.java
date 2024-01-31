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
package com.oceanbase.odc.plugin.connect.model;

import java.util.Properties;

/**
 * @author jingtian
 * @date 2024/1/31
 */
public class ConnectionPropertiesBuilder {
    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String DEFAULT_SCHEMA = "defaultSchema";
    public static final String USER = "user";
    public static final String PASSWORD = "password";

    /**
     * For oracle only
     */
    public static final String SID = "sid";
    public static final String SERVICE_NAME = "serviceName";
    /**
     * Specifies the administrative user for authentication in oracle JDBC, enumeration
     * values：sysdba、sysoper、normal.
     */
    public static final String USER_ROLE = "internal_logon";
    private final Properties properties = new Properties();

    public ConnectionPropertiesBuilder host(String host) {
        if (host != null) {
            properties.put(HOST, host);
        }
        return this;
    }

    public ConnectionPropertiesBuilder port(Integer port) {
        if (port != null) {
            properties.put(PORT, port);
        }
        return this;
    }

    public ConnectionPropertiesBuilder defaultSchema(String schema) {
        if (schema != null) {
            properties.put(DEFAULT_SCHEMA, schema);
        }
        return this;
    }

    public ConnectionPropertiesBuilder sid(String sid) {
        if (sid != null) {
            properties.put(SID, sid);
        }
        return this;
    }

    public ConnectionPropertiesBuilder serviceName(String serviceName) {
        if (serviceName != null) {
            properties.put(SERVICE_NAME, serviceName);
        }
        return this;
    }

    public ConnectionPropertiesBuilder user(String userName) {
        if (userName != null) {
            properties.put(USER, userName);
        }
        return this;
    }

    public ConnectionPropertiesBuilder passWord(String passWord) {
        if (passWord != null) {
            properties.put(PASSWORD, passWord);
        } else {
            properties.put(PASSWORD, "");
        }
        return this;
    }

    public ConnectionPropertiesBuilder userRole(UserRole userRole) {
        if (userRole != null) {
            properties.put(USER_ROLE, userRole.getValue());
        }
        return this;
    }

    public Properties build() {
        return properties;
    }
}
