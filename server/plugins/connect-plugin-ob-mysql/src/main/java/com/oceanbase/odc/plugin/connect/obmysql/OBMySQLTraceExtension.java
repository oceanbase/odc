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
package com.oceanbase.odc.plugin.connect.obmysql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.pf4j.Extension;

import com.alibaba.druid.pool.DruidPooledConnection;
import com.oceanbase.jdbc.OceanBaseConnection;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.sql.execute.model.SqlExecTime;
import com.oceanbase.odc.core.sql.util.OBUtils;
import com.oceanbase.odc.plugin.connect.api.TraceExtensionPoint;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-04-14
 * @since 4.2.0
 */
@Extension
@Slf4j
public class OBMySQLTraceExtension implements TraceExtensionPoint {

    @Override
    public SqlExecTime getExecuteDetail(Statement statement, String version) throws SQLException {
        SqlExecTime executeDetails = new SqlExecTime();
        OceanBaseConnection connection = getOceanBaseConnection(statement);
        long lastPacketResponseTimestamp =
                TimeUnit.MICROSECONDS.convert(connection.getLastPacketResponseTimestamp(),
                        TimeUnit.MILLISECONDS);
        long lastPacketSendTimestamp = TimeUnit.MICROSECONDS.convert(connection.getLastPacketSendTimestamp(),
                TimeUnit.MILLISECONDS);
        // collect jdbc execution time and clear network statistics
        connection.networkStatistics(false);
        if (VersionUtils.isGreaterThanOrEqualsTo(version, "4.0.0")) {
            log.debug("'show trace' does not support OceanBase {}", version);
            String traceId = OBUtils.getLastTraceIdAfter2277(statement);
            executeDetails.setTraceId(traceId);
        } else {
            executeDetails =
                    OBUtils.getLastExecuteDetailsBefore400(statement, version);
        }
        executeDetails.setLastPacketSendTimestamp(lastPacketSendTimestamp);
        executeDetails.setLastPacketResponseTimestamp(lastPacketResponseTimestamp);

        return executeDetails;
    }

    private OceanBaseConnection getOceanBaseConnection(Statement statement) throws SQLException {
        Connection connection = statement.getConnection();
        if (connection instanceof DruidPooledConnection) {
            DruidPooledConnection druidPooledConnection = (DruidPooledConnection) connection;
            connection = druidPooledConnection.getConnection();
        }
        PreConditions.validArgumentState((connection instanceof OceanBaseConnection),
                ErrorCodes.IllegalArgument,
                new Object[] {connection.getClass().getSimpleName()},
                "Connection object can not cast to OceanBaseConnection");
        return (OceanBaseConnection) connection;
    }

}
