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
import java.sql.Statement;

import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.datasource.ConnectionInitializer;
import com.oceanbase.odc.core.sql.util.OBUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EnableTraceInitializer implements ConnectionInitializer {
    /**
     * <pre>
     * | mode   | version | parameter            |
     * | oracle | 3.x-    | ob_enable_trace_log  |
     * | oracle | 4.x     | ob_enable_show_trace |
     * | mysql  | 3.x-    | ob_enable_trace_log  |
     * | mysql  | 4.x     | ob_enable_show_trace |
     * </pre>
     */
    @Override
    public void init(Connection connection) {
        try {
            String version = OBUtils.getObVersion(connection);
            try (Statement statement = connection.createStatement()) {
                String sql = "set session %s='ON';";
                if (VersionUtils.isGreaterThanOrEqualsTo(version, "4.0.0")) {
                    statement.execute(String.format(sql, "ob_enable_show_trace"));
                    statement.execute("CALL DBMS_MONITOR.OB_SESSION_TRACE_ENABLE(null, 1, 1, 'ALL')");
                } else {
                    statement.execute(String.format(sql, "ob_enable_trace_log"));
                }
            }
        } catch (Exception e) {
            log.warn("Enable show trace failed, errMsg={}", e.getMessage());
        }
    }

}
