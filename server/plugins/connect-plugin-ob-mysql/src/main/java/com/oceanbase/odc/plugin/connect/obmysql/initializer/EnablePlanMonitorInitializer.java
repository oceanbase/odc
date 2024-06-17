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
package com.oceanbase.odc.plugin.connect.obmysql.initializer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.datasource.ConnectionInitializer;
import com.oceanbase.odc.core.sql.util.OBUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/5/20
 */
@Slf4j
public class EnablePlanMonitorInitializer implements ConnectionInitializer {
    @Override
    public void init(Connection connection) throws SQLException {
        String version = OBUtils.getObVersion(connection);
        if (VersionUtils.isLessThan(version, "4.2.4")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("set enable_sql_plan_monitor='ON'");
        } catch (Exception e) {
            log.warn("Enable enable_sql_plan_monitor failed, errMsg={}", e.getMessage());
        }
    }
}
