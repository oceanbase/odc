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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.lang3.Validate;
import org.pf4j.Extension;

import com.oceanbase.odc.core.sql.execute.model.SqlExecTime;
import com.oceanbase.odc.plugin.connect.api.TraceExtensionPoint;

import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2023/11/8
 * @since ODC_release_4.2.4
 */
@Slf4j
@Extension
public class OracleTraceExtension implements TraceExtensionPoint {
    @Override
    public SqlExecTime getExecuteDetail(Statement statement, String version) throws SQLException {
        SqlExecTime sqlExecTime = new SqlExecTime();
        try {
            String sql =
                    "SELECT PREV_SQL_ID FROM SYS.V$SESSION WHERE SID = SYS_CONTEXT('USERENV', 'SID') and AUDSID=SYS_CONTEXT('USERENV', 'SESSIONID')";
            String preSqlId = null;
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
                preSqlId = rs.getString("PREV_SQL_ID");
            }
            Validate.notNull(preSqlId, "PREV_SQL_ID can not be null");
            sql = "select SQL_TEXT, ELAPSED_TIME, LAST_ACTIVE_TIME FROM SYS.V$SQL WHERE SQL_ID='" + preSqlId
                    + "' ORDER BY LAST_ACTIVE_TIME DESC";
            ResultSet resultSet = statement.executeQuery(sql);
            if (resultSet.next()) {
                // Get only the first row of data
                Long execTime = resultSet.getBigDecimal("ELAPSED_TIME").longValue();
                log.info("Get execute detail from oracle, sql={}, execTime={}", resultSet.getString("SQL_TEXT"),
                        execTime);
                sqlExecTime.setExecuteMicroseconds(execTime);
            }
        } catch (Exception e) {
            log.warn("Failed to get execute detail from oracle, message={}", e.getMessage());
        }
        return sqlExecTime;
    }
}
