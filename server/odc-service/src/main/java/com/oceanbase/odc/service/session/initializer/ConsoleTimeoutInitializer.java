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
package com.oceanbase.odc.service.session.initializer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import com.oceanbase.odc.core.datasource.ConnectionInitializer;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsoleTimeoutInitializer implements ConnectionInitializer {

    private final Long timeoutUs;
    private final static Set<String> TIMEOUT_VARIABLES_SET = new HashSet<>();

    static {
        TIMEOUT_VARIABLES_SET.add("ob_query_timeout");
        TIMEOUT_VARIABLES_SET.add("ob_trx_timeout");
        TIMEOUT_VARIABLES_SET.add("ob_pl_block_timeout");
    }

    public ConsoleTimeoutInitializer(@NonNull Long timeouUs) {
        this.timeoutUs = timeouUs;
    }

    @Override
    public void init(Connection connection) throws SQLException {
        Set<String> variableToBeModified = new HashSet<>();
        try (Statement stmt = connection.createStatement();
                ResultSet resultSet = stmt.executeQuery("show session variables like '%timeout'")) {
            while (resultSet.next()) {
                String variableName = resultSet.getString(1);
                if (!TIMEOUT_VARIABLES_SET.contains(variableName)) {
                    continue;
                }
                long timeout = resultSet.getLong(2);
                if (timeout < timeoutUs) {
                    variableToBeModified.add(variableName);
                }
            }
        }
        for (String variable : variableToBeModified) {
            try (PreparedStatement stmt = connection.prepareStatement(String.format("set session %s=?", variable))) {
                stmt.setObject(1, timeoutUs);
                int affectRow = stmt.executeUpdate();
                log.info("Set the variable value, {}={}, affectRow={}", variable, timeoutUs, affectRow);
            }
        }
    }

}

