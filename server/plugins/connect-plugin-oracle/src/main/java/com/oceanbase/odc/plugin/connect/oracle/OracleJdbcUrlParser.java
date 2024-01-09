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

package com.oceanbase.odc.plugin.connect.oracle;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oceanbase.odc.core.shared.jdbc.HostAddress;
import com.oceanbase.odc.core.shared.jdbc.JdbcUrlParser;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2023/11/28
 * @since ODC_release_4.2.4
 */
public class OracleJdbcUrlParser implements JdbcUrlParser {
    private final String ORACLE_JDBC_PREFIX = "jdbc:oracle:thin:@";
    private static final Pattern SERVICE_NAME_PATTERN = Pattern.compile("@//([^:/]*):(\\d+)/.*");
    private static final Pattern SID_PATTERN = Pattern.compile("@([^:/]*):(\\d+):.*");
    private String jdbcUrl;
    private List<HostAddress> addresses;
    private Map<String, Object> parameters;

    public OracleJdbcUrlParser(@NonNull String jdbcUrl) {
        if (!jdbcUrl.startsWith(ORACLE_JDBC_PREFIX)) {
            throw new IllegalArgumentException("Invalid JDBC URL for Oracle: " + jdbcUrl);
        }
        this.jdbcUrl = jdbcUrl;
        this.addresses = parseHostAndPort(jdbcUrl);
        this.parameters = parseParameters(jdbcUrl);
    }

    private List<HostAddress> parseHostAndPort(String jdbcUrl) {
        HostAddress hostAddress = new HostAddress();
        Matcher serviceNameMatcher = SERVICE_NAME_PATTERN.matcher(jdbcUrl);
        Matcher sidMatcher = SID_PATTERN.matcher(jdbcUrl);

        if (serviceNameMatcher.find()) {
            hostAddress.setHost(serviceNameMatcher.group(1));
            hostAddress.setPort(Integer.valueOf(serviceNameMatcher.group(2)));
        } else if (sidMatcher.find()) {
            hostAddress.setHost(sidMatcher.group(1));
            hostAddress.setPort(Integer.valueOf(sidMatcher.group(2)));
        }
        return Collections.singletonList(hostAddress);
    }

    private Map<String, Object> parseParameters(String jdbcUrl) {
        Map<String, Object> paramsMap = new HashMap<>();

        int paramsIndex = jdbcUrl.indexOf('?');
        if (paramsIndex > 0) {
            String paramsString = jdbcUrl.substring(paramsIndex + 1);

            String[] paramsArray = paramsString.split("&");
            for (String param : paramsArray) {
                String[] keyValue = param.split("=", 2);
                String key = keyValue[0];
                String value = keyValue.length > 1 ? keyValue[1] : ""; // 处理没有值的参数
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
        // we cannot get connect schema by parse jdbcUrl
        return null;
    }

    @Override
    public Map<String, Object> getParameters() {
        return this.parameters;
    }
}
