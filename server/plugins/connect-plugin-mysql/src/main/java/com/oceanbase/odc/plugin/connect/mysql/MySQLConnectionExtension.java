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
package com.oceanbase.odc.plugin.connect.mysql;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.pf4j.Extension;

import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.datasource.ConnectionInitializer;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.plugin.connect.api.TestResult;
import com.oceanbase.odc.plugin.connect.mysql.initializer.EnableProfileInitializer;
import com.oceanbase.odc.plugin.connect.obmysql.OBMySQLConnectionExtension;
import com.oceanbase.odc.plugin.connect.obmysql.model.HostAddress;

import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2023/5/26
 * @since ODC_release_4.2.0
 */
@Slf4j
@Extension
public class MySQLConnectionExtension extends OBMySQLConnectionExtension {
    private final String MIN_VERSION_SUPPORTED = "5.6.0";

    @Override
    protected String getJdbcUrlPrefix() {
        return "jdbc:mysql://%s:%d";
    }

    @Override
    public String getDriverClassName() {
        return OdcConstants.MYSQL_DRIVER_CLASS_NAME;
    }

    @Override
    public List<ConnectionInitializer> getConnectionInitializers() {
        return Collections.singletonList(new EnableProfileInitializer());
    }

    @Override
    public TestResult test(String jdbcUrl, Properties properties, int queryTimeout) {
        TestResult testResult = super.test(jdbcUrl, properties, queryTimeout);
        if (testResult.getErrorCode() != null) {
            return testResult;
        }
        try (Connection connection = DriverManager.getConnection(jdbcUrl, properties)) {
            MySQLInformationExtension informationExtension = new MySQLInformationExtension();
            String version = informationExtension.getDBVersion(connection);
            if (VersionUtils.isLessThan(version, MIN_VERSION_SUPPORTED)) {
                return TestResult.unsupportedDBVersion(version);
            }
        } catch (Exception e) {
            return TestResult.unknownError(e);
        }
        return testResult;
    }

    @Override
    protected HostAddress parseJdbcUrl(String jdbcUrl) throws SQLException {
        URI url = URI.create(jdbcUrl.substring(5));
        return new HostAddress(url.getHost(), url.getPort());
    }

    @Override
    protected Map<String, String> appendDefaultJdbcUrlParameters(Map<String, String> jdbcUrlParams) {
        if (Objects.nonNull(jdbcUrlParams) && !jdbcUrlParams.containsKey("tinyInt1isBit")) {
            jdbcUrlParams.put("tinyInt1isBit", "false");
        }
        return jdbcUrlParams;
    }

}
