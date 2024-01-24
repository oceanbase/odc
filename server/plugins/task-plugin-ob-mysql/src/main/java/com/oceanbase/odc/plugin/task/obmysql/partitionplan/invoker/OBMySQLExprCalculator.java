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
package com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker;

import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.sql.execute.mapper.CellData;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.datatype.OBMySQLJdbcDataTypeFactory;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.mapper.CellDataProcessor;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.mapper.CellDataProcessors;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.NonNull;

/**
 * {@link OBMySQLExprCalculator}
 *
 * @author yh263208
 * @date 2024-01-23 16:19
 * @since ODC_release_4.2.4
 * @see SqlExprCalculator
 */
public class OBMySQLExprCalculator implements SqlExprCalculator {

    private final Connection con;

    public OBMySQLExprCalculator(@NonNull Connection connection) {
        this.con = connection;
    }

    @Override
    public SqlExprResult calculate(@NonNull String expression) {
        String sql = generateExecuteSql(expression);
        Verify.notEmpty(sql, "Query sql can not be empty");
        return new JdbcTemplate(new SingleConnectionDataSource(this.con, false)).queryForObject(sql, (rs, r) -> {
            SqlExprResult result = new SqlExprResult();
            DataType dataType = getDataType(rs.getMetaData(), 0);
            CellDataProcessor processor = getByDataType(dataType);
            result.setDataType(dataType);
            result.setValue(processor.mapCell(new CellData(rs, 0, dataType)));
            return result;
        });
    }

    protected CellDataProcessor getByDataType(@NonNull DataType dataType) {
        return CellDataProcessors.getByDataType(dataType);
    }

    protected DataType getDataType(@NonNull ResultSetMetaData metaData, Integer index) throws SQLException {
        return new OBMySQLJdbcDataTypeFactory(metaData, index).generate();
    }

    protected String generateExecuteSql(String expression) {
        SqlBuilder sqlBuilder = new MySQLSqlBuilder();
        return sqlBuilder.append("select ").append(expression).append(" from dual").toString();
    }

}
