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
package com.oceanbase.odc.test.database;

import org.apache.commons.lang3.StringUtils;

/**
 * @author gaoda.xy
 * @date 2023/2/21 00:54
 */
public class TestDBUtil {
    private static final String OB_JDBC_PROTOCOL = "oceanbase";
    private static final String MYSQL_JDBC_PROTOCOL = "mysql";

    public static String buildUrl(String host, Integer port, String database, String type) {
        StringBuilder builder = new StringBuilder();
        if ("MYSQL".equals(type)) {
            builder.append(String.format("jdbc:%s://%s:%d", MYSQL_JDBC_PROTOCOL, host, port));
        } else {
            builder.append(String.format("jdbc:%s://%s:%d", OB_JDBC_PROTOCOL, host, port));
        }
        if (StringUtils.isNotBlank(database)) {
            builder.append(String.format("/%s", database));
        }
        if ("MYSQL".equals(type)) {
            builder.append("?").append("useSSL=false");
        }
        return builder.toString();
    }

    public static String buildUser(String username, String tenant, String cluster) {
        StringBuilder builder = new StringBuilder(username);
        if (StringUtils.isNotBlank(tenant)) {
            builder.append("@").append(tenant);
        }
        if (StringUtils.isNotBlank(cluster)) {
            builder.append("#").append(cluster);
        }
        return builder.toString();
    }
}
