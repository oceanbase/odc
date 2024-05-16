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

import com.alibaba.druid.filter.FilterEventAdapter;
import com.alibaba.druid.proxy.jdbc.ConnectionProxy;
import com.oceanbase.odc.core.shared.Verify;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2024-04-19
 * @since 4.2.4
 */
@Slf4j
public class ForeignKeyChecksFilter extends FilterEventAdapter {

    @Override
    public void connection_connectAfter(ConnectionProxy connection) {
        disableForeignKey(connection);
    }

    public void disableForeignKey(Connection connection) {
        try {
            String result = getForeignKeyChecks(connection);
            if ("OFF".equalsIgnoreCase(result)) {
                return;
            }
        } catch (SQLException e) {
            log.warn("Get foreign key check failed.", e);
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("set session foreign_key_checks='OFF'");
        } catch (SQLException e) {
            log.warn("Set session foreign_key_checks='OFF' failed.", e);
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

