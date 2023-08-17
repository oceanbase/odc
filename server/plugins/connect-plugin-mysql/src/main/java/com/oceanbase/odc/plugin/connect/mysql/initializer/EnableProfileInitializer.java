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
package com.oceanbase.odc.plugin.connect.mysql.initializer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.oceanbase.odc.core.datasource.ConnectionInitializer;

import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2023/5/30
 * @since ODC_release_4.2.0
 */
@Slf4j
public class EnableProfileInitializer implements ConnectionInitializer {
    @Override
    public void init(Connection connection) throws SQLException {
        try {
            Statement statement = connection.createStatement();
            statement.execute("set session profiling=1;");
        } catch (Exception e) {
            log.warn("Enable show profile failed, errMsg={}", e.getMessage());
        }
    }
}
