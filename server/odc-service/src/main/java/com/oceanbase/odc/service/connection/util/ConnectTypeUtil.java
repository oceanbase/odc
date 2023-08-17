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
package com.oceanbase.odc.service.connection.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.connection.model.HostAddress;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link ConnectTypeUtil}
 *
 * @author yh263208
 * @date 2022-09-29 15:21
 * @since ODC_release_3.5.0
 */
@Slf4j
public class ConnectTypeUtil {

    /**
     * 和公有云同学确认过，公有云的私网/公网地址均以 {@code oceanbase.aliyuncs.com} 结尾，以此为标志判断是否处于云环境 和多云同学确认过，多云场景下所有的公网地址均已
     * {@code oceanbase.cloud} 结尾，以此为标志判断是否处于多云环境
     */
    public static final String[] CLOUD_SUFFIX = new String[] {"oceanbase.aliyuncs.com", "oceanbase.cloud"};
    private static final Integer REACHABLE_TIMEOUT_MILLIS = 10000;

    public static ConnectType getConnectType(@NonNull String jdbcUrl, @NonNull String username,
            String password, int queryTimeout) throws SQLException {
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

        try (Connection connection = DriverManager.getConnection(jdbcUrl, properties);
                Statement statement = connection.createStatement()) {
            if (queryTimeout >= 0) {
                statement.setQueryTimeout(queryTimeout);
            }
            return getConnectType(statement, jdbcUrl);
        }
    }

    private static ConnectType getConnectType(Statement statement, String jdbcUrl) throws SQLException {
        DialectType dialectType = getDialectType(statement);
        if (dialectType == null || !isCloud(new DefaultJdbcUrlParser(jdbcUrl))) {
            /**
             * 通常来说，用户最容易填错 type 的场景就是搞混了公有云和非公有云模式，因此这里也就仅对这种场景做检测。之所以去掉其他 数据库类型的检测一个是因为将来 odc
             * 要支持的类型太多复杂度过高，二来是有的数据库类型难以区分，比如 sofaodp 等。
             */
            return null;
        }
        // 公有云模式下
        switch (dialectType) {
            case OB_ORACLE:
                return ConnectType.CLOUD_OB_ORACLE;
            case OB_MYSQL:
                return ConnectType.CLOUD_OB_MYSQL;
            default:
                throw new UnsupportedOperationException("Unsupported dialect type, " + dialectType);
        }
    }

    private static DialectType getDialectType(Statement statement) throws SQLException {
        String dialectQuerySql = "SHOW VARIABLES LIKE 'ob_compatibility_mode'";
        try (ResultSet resultSet = statement.executeQuery(dialectQuerySql)) {
            if (!resultSet.next()) {
                return null;
            }
            String str = resultSet.getString("VALUE").toUpperCase();
            return DialectType.valueOf(StringUtils.startsWithIgnoreCase(str, "ob") ? str : "OB_" + str);
        }
    }

    public static boolean isCloud(JdbcUrlParser parser) {
        List<HostAddress> hostAddresses = parser.getHostAddresses();
        Verify.verify(CollectionUtils.isNotEmpty(hostAddresses), "HostAddress is empty");
        boolean returnVal = true;
        for (HostAddress address : hostAddresses) {
            returnVal &= StringUtils.endsWithAny(address.getHost(), CLOUD_SUFFIX);
        }
        return returnVal;
    }

}
