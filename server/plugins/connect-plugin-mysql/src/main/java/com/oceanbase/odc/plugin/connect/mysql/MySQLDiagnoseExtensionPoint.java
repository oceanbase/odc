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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.pf4j.Extension;

import com.oceanbase.odc.common.util.tableformat.BorderStyle;
import com.oceanbase.odc.common.util.tableformat.CellStyle;
import com.oceanbase.odc.common.util.tableformat.CellStyle.AbbreviationStyle;
import com.oceanbase.odc.common.util.tableformat.CellStyle.HorizontalAlign;
import com.oceanbase.odc.common.util.tableformat.CellStyle.NullStyle;
import com.oceanbase.odc.common.util.tableformat.Table;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.OBException;
import com.oceanbase.odc.core.shared.model.SqlExecDetail;
import com.oceanbase.odc.core.shared.model.SqlExplain;
import com.oceanbase.odc.core.shared.model.SqlPlanGraph;
import com.oceanbase.odc.plugin.connect.api.SqlDiagnoseExtensionPoint;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2023/6/5
 * @since ODC_release_4.2.0
 */
@Slf4j
@Extension
public class MySQLDiagnoseExtensionPoint implements SqlDiagnoseExtensionPoint {
    @Override
    public SqlExplain getExplain(Statement statement, @NonNull String sql) throws SQLException {
        String explainSql = "explain " + sql;
        SqlExplain sqlExplain = new SqlExplain();
        try {
            ResultSet resultSet = statement.executeQuery(explainSql);
            ResultSetMetaData metaData = resultSet.getMetaData();
            int colCount = metaData.getColumnCount();
            Table table = new Table(colCount, BorderStyle.HORIZONTAL_ONLY);
            CellStyle cs = new CellStyle(HorizontalAlign.CENTER, AbbreviationStyle.DOTS, NullStyle.NULL_TEXT);
            for (int i = 1; i <= colCount; i++) {
                table.setColumnWidth(i - 1, 10, metaData.getColumnDisplaySize(i));
                table.addCell(metaData.getColumnName(i), cs);
            }
            while (resultSet.next()) {
                for (int i = 1; i <= colCount; i++) {
                    table.addCell(resultSet.getString(i), cs);
                }
            }
            sqlExplain.setOriginalText(table.render().toString());
            sqlExplain.setShowFormatInfo(false);
        } catch (Exception e) {
            throw OBException.executeFailed(ErrorCodes.ObGetPlanExplainFailed, e.getMessage());
        }
        return sqlExplain;
    }

    @Override
    public SqlExplain getPhysicalPlanBySqlId(Connection connection, @NonNull String sqlId) throws SQLException {
        throw new UnsupportedOperationException("Not supported for mysql mode");
    }

    @Override
    public SqlExplain getPhysicalPlanBySql(Connection connection, @NonNull String sql) throws SQLException {
        throw new UnsupportedOperationException("Not supported for mysql mode");
    }

    @Override
    public SqlExecDetail getExecutionDetailById(Connection connection, @NonNull String id) throws SQLException {
        throw new UnsupportedOperationException("Not supported for mysql mode");
    }

    @Override
    public SqlExecDetail getExecutionDetailBySql(Connection connection, @NonNull String sql) throws SQLException {
        throw new UnsupportedOperationException("Not supported for mysql mode");
    }

    @Override
    public SqlPlanGraph getSqlPlanGraphByTraceId(Connection connection, @NonNull String traceId) throws SQLException {
        throw new UnsupportedOperationException("Not supported for mysql mode");
    }

}
