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
package com.oceanbase.odc.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;

import com.oceanbase.odc.core.datasource.ConnectionInitializer;
import com.oceanbase.odc.core.datasource.ProxyDataSource;
import com.oceanbase.odc.core.shared.Verify;
import com.zaxxer.hikari.HikariDataSource;

import lombok.NonNull;

/**
 * {@code Flowable} Configuration for web
 *
 * @author yh263208
 * @date 2022-01-27 11:16
 * @since ODC_release_3.3.0
 */
public class DefaultFlowableConfiguration extends BaseFlowableConfiguration {

    @Override
    protected DataSource getFlowableDataSource(DataSource dataSource) {
        ProxyDataSource proxyDataSource = new ProxyDataSource(dataSource);
        proxyDataSource.setInitializer(new NoForeignKeyInitializer());
        return proxyDataSource;
    }


    static class NoForeignKeyInitializer implements ConnectionInitializer {

        @Override
        public void init(Connection connection) throws SQLException {
            String result = getForeignKeyChecks(connection);
            if ("OFF".equalsIgnoreCase(result)) {
                return;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("set session foreign_key_checks='OFF'");
            }
        }

        private String getForeignKeyChecks(@NonNull Connection connection) throws SQLException {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery("show variables like 'foreign_key_checks'")) {
                    Verify.verify(resultSet.next(), "No variable value");
                    return resultSet.getString(2);
                }
            }
        }
    }

}
