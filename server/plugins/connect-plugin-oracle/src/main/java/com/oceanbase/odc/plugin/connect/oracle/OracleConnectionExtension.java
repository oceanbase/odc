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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.apache.commons.lang3.Validate;
import org.pf4j.Extension;

import com.oceanbase.odc.common.util.ExceptionUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.datasource.ConnectionInitializer;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.core.shared.exception.VerifyException;
import com.oceanbase.odc.core.shared.jdbc.JdbcUrlParser;
import com.oceanbase.odc.plugin.connect.api.TestResult;
import com.oceanbase.odc.plugin.connect.model.ConnectionPropertiesBuilder;
import com.oceanbase.odc.plugin.connect.obmysql.OBMySQLConnectionExtension;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2023/11/8
 * @since ODC_release_4.2.4
 */
@Slf4j
@Extension
public class OracleConnectionExtension extends OBMySQLConnectionExtension {
    @Override
    public String generateJdbcUrl(@NonNull Properties properties, Map<String, String> jdbcParameters) {
        String host = properties.getProperty(ConnectionPropertiesBuilder.HOST);
        Object portValue = properties.get(ConnectionPropertiesBuilder.PORT);
        Integer port = (portValue instanceof Integer) ? (Integer) portValue : null;
        Validate.notEmpty(host, "host can not be empty");
        Validate.notNull(port, "port can not be null");

        StringBuilder jdbcUrl = new StringBuilder();
        String sid = properties.getProperty(ConnectionPropertiesBuilder.SID);
        String serviceName = properties.getProperty(ConnectionPropertiesBuilder.SERVICE_NAME);
        if (Objects.nonNull(sid)) {
            jdbcUrl.append("jdbc:oracle:thin:@").append(host).append(":").append(port).append(":").append(sid);
        } else if (Objects.nonNull(serviceName)) {
            jdbcUrl.append("jdbc:oracle:thin:@//").append(host).append(":").append(port).append("/")
                    .append(serviceName);
        } else {
            throw new VerifyException("sid or service name must be set");
        }
        String parameters = getJdbcUrlParameters(jdbcParameters);
        if (StringUtils.isNotBlank(parameters)) {
            jdbcUrl.append("?").append(parameters);
        }
        return jdbcUrl.toString();
    }

    @Override
    protected Map<String, String> appendDefaultJdbcUrlParameters(Map<String, String> jdbcUrlParams) {
        return jdbcUrlParams;
    }

    @Override
    public String getDriverClassName() {
        return OdcConstants.ORACLE_DRIVER_CLASS_NAME;
    }

    @Override
    public List<ConnectionInitializer> getConnectionInitializers() {
        return Collections.emptyList();
    }

    @Override
    public TestResult test(@NonNull String jdbcUrl, @NonNull Properties properties, int queryTimeout) {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, properties)) {
            try (Statement statement = connection.createStatement()) {
                if (queryTimeout >= 0) {
                    statement.setQueryTimeout(queryTimeout);
                }
                return TestResult.success();
            }
        } catch (Exception e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            return TestResult.unknownError(rootCause);
        }
    }

    @Override
    public JdbcUrlParser getConnectionInfo(@NonNull String jdbcUrl, String userName) throws SQLException {
        return new OracleJdbcUrlParser(jdbcUrl, userName);
    }
}
