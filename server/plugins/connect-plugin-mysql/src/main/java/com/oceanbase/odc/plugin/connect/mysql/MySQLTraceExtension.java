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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.pf4j.Extension;

import com.oceanbase.odc.core.sql.execute.model.SqlExecTime;
import com.oceanbase.odc.plugin.connect.api.TraceExtensionPoint;

import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2023/5/26
 * @since ODC_release_4.2.0
 */
@Slf4j
@Extension
public class MySQLTraceExtension implements TraceExtensionPoint {

    @Override
    public SqlExecTime getExecuteDetail(Statement statement, String version) throws SQLException {
        SqlExecTime sqlExecTime = new SqlExecTime();
        ResultSet resultSet = statement.executeQuery("show profile;");
        Long execTime = 0L;
        while (resultSet.next()) {
            execTime += resultSet.getBigDecimal(2).multiply(new BigDecimal("1000000")).longValue();
        }

        ResultSet profiling = statement.executeQuery("show session variables like 'profiling';");
        boolean isProfileSetON = true;
        while (profiling.next()) {
            if ("OFF".equals(profiling.getString(2))) {
                isProfileSetON = false;
            }
        }
        if (isProfileSetON) {
            sqlExecTime.setExecuteMicroseconds(execTime);
        }
        return sqlExecTime;
    }
}
