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
package com.oceanbase.odc.plugin.connect.oboracle.initializer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.oceanbase.odc.core.datasource.ConnectionInitializer;

import lombok.extern.slf4j.Slf4j;

/**
 * Used to initialize {@code dbms_output}
 *
 * @author yh263208
 * @date 2021-11-22 21:16
 * @see ConnectionInitializer
 * @since ODC_release_3.2.2
 */
@Slf4j
public class OBOracleDBMSOutputInitializer implements ConnectionInitializer {

    private static final int PL_LOG_CACHE_SIZE = 1000000;

    @Override
    public void init(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // enable log output, default cache size 1000000
            statement.execute(String.format("call dbms_output.enable(%s)", PL_LOG_CACHE_SIZE));
        } catch (Exception e) {
            log.warn("Enable dbms_output failed, dbms_output may not exists", e);
        }
    }

}
