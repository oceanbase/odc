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

/**
 * @author jingtian
 * @date 2023/11/8
 * @since ODC_release_4.2.4
 */
public class ConnectionConstants {
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
}
