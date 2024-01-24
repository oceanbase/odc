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

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.tools.dbbrowser.model.datatype.JdbcDataTypeFactory;
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
        String sql = generateExecuteSql(expression, getSqlBuilder());
        Verify.notEmpty(sql, "Query sql can not be empty");
        return new JdbcTemplate(new SingleConnectionDataSource(this.con, false)).queryForObject(sql, (rs, r) -> {
            SqlExprResult result = new SqlExprResult();
            result.setValue(rs.getObject(1));
            result.setDataType(new JdbcDataTypeFactory(rs.getMetaData(), 0).generate());
            return result;
        });
    }

    protected SqlBuilder getSqlBuilder() {
        return new MySQLSqlBuilder();
    }

    protected String generateExecuteSql(String expression, SqlBuilder sqlBuilder) {
        return sqlBuilder.append("select ").append(expression).append(" from dual").toString();
    }

}
