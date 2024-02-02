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

import com.oceanbase.odc.plugin.connect.model.oracle.UserRole;

/**
 * @author jingtian
 * @date 2024/1/31
 */
public class ConnectionPropertiesBuilder {
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    /**
     * Specifies the administrative user for authentication in oracle JDBC {@link UserRole}
     */
    public static final String USER_ROLE = "internal_logon";
    private final Properties properties = new Properties();

    private ConnectionPropertiesBuilder() {}

    public static ConnectionPropertiesBuilder getBuilder() {
        return new ConnectionPropertiesBuilder();
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
