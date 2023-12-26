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
package com.oceanbase.odc.plugin.connect.obmysql;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.pf4j.Extension;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.common.util.ExceptionUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.datasource.ConnectionInitializer;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.core.shared.jdbc.HostAddress;
import com.oceanbase.odc.core.shared.jdbc.JdbcUrlParser;
import com.oceanbase.odc.plugin.connect.api.ConnectionExtensionPoint;
import com.oceanbase.odc.plugin.connect.api.TestResult;
import com.oceanbase.odc.plugin.connect.obmysql.initializer.EnableTraceInitializer;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-04-14
 * @since 4.2.0
 */
@Slf4j
@Extension
public class OBMySQLConnectionExtension implements ConnectionExtensionPoint {

    private static final Integer REACHABLE_TIMEOUT_MILLIS = 10000;

    @Override
    public String generateJdbcUrl(@NonNull String host, @NonNull Integer port, String defaultSchema,
            Map<String, String> jdbcParameters) {
        StringBuilder jdbcUrl = new StringBuilder(String.format(getJdbcUrlPrefix(), host, port));
        if (StringUtils.isNotBlank(defaultSchema)) {
            jdbcUrl.append("/").append(defaultSchema);
        }
        String parameters = getJdbcUrlParameters(jdbcParameters);
        if (StringUtils.isNotBlank(parameters)) {
            jdbcUrl.append("?").append(parameters);
        }
        return jdbcUrl.toString();
    }

    protected String getJdbcUrlPrefix() {
        return "jdbc:oceanbase://%s:%d";
    }

    @Override
    public String getDriverClassName() {
        return OdcConstants.DEFAULT_DRIVER_CLASS_NAME;
    }

    @Override
    public List<ConnectionInitializer> getConnectionInitializers() {
        return Collections.singletonList(new EnableTraceInitializer());
    }

    @Override
    public TestResult test(String jdbcUrl, String username, String password, int queryTimeout) {
        Properties properties = new Properties();
        properties.setProperty("user", username);
        if (password == null) {
            properties.setProperty("password", "");
        } else {
            properties.setProperty("password", password);
        }
        /**
         * 查看 driver 代码可知：driver 建立连接时使用的 socket 超时实际是 connectTimeout 的值，因此要让超时设置生效必须设置 connectTimeout，
         * 为了保险起见 socketTimeout 也一并设置。 且在 driver 的实现中，如果 properties 中设置某个参数，这个参数如果在 url 中再次出现，则会以 properties
         * 中设置的为准。
         */
        properties.setProperty("socketTimeout", REACHABLE_TIMEOUT_MILLIS + "");
        properties.setProperty("connectTimeout", REACHABLE_TIMEOUT_MILLIS + "");
        return test(jdbcUrl, properties, queryTimeout);
    }

    @Override
    public JdbcUrlParser getJdbcUrlParser(@NonNull String jdbcUrl) throws SQLException {
        return new OceanBaseJdbcUrlParser(jdbcUrl);
    }

    protected String getJdbcUrlParameters(Map<String, String> jdbcUrlParams) {
        if (CollectionUtils.isEmpty(jdbcUrlParams)) {
            return null;
        }
        jdbcUrlParams = appendDefaultJdbcUrlParameters(jdbcUrlParams);
        return jdbcUrlParams.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    protected Map<String, String> appendDefaultJdbcUrlParameters(Map<String, String> jdbcUrlParams) {
        // there is a bug of oceanbase-client 2.4.7.1, setting this to true may cause OOM exception
        jdbcUrlParams.put("enableFullLinkTrace", "false");
        return jdbcUrlParams;
    }

    protected TestResult test(String jdbcUrl, Properties properties, int queryTimeout) {
        HostAddress hostAddress;
        try {
            hostAddress = getJdbcUrlParser(jdbcUrl).getHostAddresses().get(0);
        } catch (SQLException e) {
            return TestResult.unknownError(e);
        }
        try (Connection connection = DriverManager.getConnection(jdbcUrl, properties)) {
            try (Statement statement = connection.createStatement()) {
                if (queryTimeout >= 0) {
                    statement.setQueryTimeout(queryTimeout);
                }
                return TestResult.success();
            }
        } catch (Exception e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (rootCause == null) {
                log.warn("Failed to get connection, errMsg={}", e.getLocalizedMessage());
                return TestResult.unknownError(e);
            }
            log.warn("Failed to get connection, rooCauseErrMsg={}", rootCause.getLocalizedMessage());
            String host = hostAddress.getHost();
            Integer port = hostAddress.getPort();
            if (rootCause instanceof ConnectException) {
                /**
                 * Connection refused (Connection refused), 连接被拒绝，通常是远端主机上的对应端口没有程序监听
                 */
                return TestResult.unknownPort(port);
            } else if (rootCause instanceof SocketTimeoutException) {
                /**
                 * socket 连接超时，通常对应于远端机器 ip 不通
                 */
                return TestResult.hostUnreachable(host);
            } else if (rootCause instanceof UnknownHostException) {
                /**
                 * 域名解析失败，未知主机错误
                 */
                return TestResult.unknownHost(host);
            } else if (StringUtils.containsIgnoreCase(rootCause.getMessage(), "Access denied")) {
                return TestResult.accessDenied(rootCause.getLocalizedMessage());
            }
            return TestResult.unknownError(rootCause);
        }
    }

}
