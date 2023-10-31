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

package com.oceanbase.odc.service.dlm.checker;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.dlm.model.DataArchiveTableConfig;
import com.oceanbase.odc.service.dlm.model.OffsetConfig;
import com.oceanbase.odc.service.dlm.utils.DataArchiveConditionUtil;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/10/30 16:18
 * @Descripition:
 */
@Slf4j
public class MySqlDLMChecker extends AbstractDLMChecker {
    public MySqlDLMChecker(Database database) {
        super(database);
    }

    @Override
    public void checkTargetDbType(DialectType dbType) {
        if (dbType != DialectType.MYSQL && dbType != DialectType.OB_MYSQL) {
            log.warn("Unsupported data archiving link:{} to {}", database.getDataSource().getDialectType(), dbType);
            throw new UnsupportedException(String.format("Unsupported data archiving link:%s to %s",
                    database.getDataSource().getDialectType(), dbType));
        }
    }

    @Override
    public void checkTablesPrimaryKey(List<DataArchiveTableConfig> tables) {
        SyncJdbcExecutor syncJdbcExecutor = getConnectionSession().getSyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY);
        SqlBuilder sqlBuilder = new MySQLSqlBuilder();
        sqlBuilder.append("select table_name from information_schema.COLUMNS where ");
        sqlBuilder.append(String.format("table_schema='%s' ", database.getName()));
        sqlBuilder.append("and column_key = 'PRI' group by table_name;");
        HashSet<String> tableNames =
                new HashSet<>(syncJdbcExecutor.query(sqlBuilder.toString(), (rs, num) -> rs.getString(1)));
        tables.forEach(tableConfig -> {
            if (!tableNames.contains(tableConfig.getTableName())) {
                throw new IllegalArgumentException(
                        String.format("The table need to contain a primary key!tableName=%s",
                                tableConfig.getTableName()));
            }
        });
    }

    @Override
    public void checkDLMTableCondition(List<DataArchiveTableConfig> tables, List<OffsetConfig> variables) {
        SyncJdbcExecutor syncJdbcExecutor = getConnectionSession().getSyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY);
        // Ensure the conditions are valid when executing.
        generateTestSqls(tables, variables).forEach((key, value) -> {
            try {
                syncJdbcExecutor.execute("explain " + value);
            } catch (Exception e) {
                log.warn("Test condition failed,sql={}", value, e);
                throw new IllegalArgumentException(String.format("Condition is not supported!TableName=%s,Condition=%s",
                        key.getTableName(), key.getConditionExpression()));
            }
        });
    }

    private Map<DataArchiveTableConfig, String> generateTestSqls(List<DataArchiveTableConfig> tables,
            List<OffsetConfig> variables) {
        Map<DataArchiveTableConfig, String> sqlMap = new HashMap<>();
        tables.forEach(table -> sqlMap.put(table, generateTestSql(table, variables)));
        return sqlMap;
    }

    private String generateTestSql(DataArchiveTableConfig table, List<OffsetConfig> variables) {
        try {
            SqlBuilder sqlBuilder = new MySQLSqlBuilder();
            sqlBuilder.append("SELECT 1 FROM ").identifier(database.getName(), table.getTableName());
            if (StringUtils.isNotEmpty(table.getConditionExpression())) {
                sqlBuilder.append(" WHERE ")
                        .append(DataArchiveConditionUtil.parseCondition(table.getConditionExpression(),
                                variables, new Date()));
            }
            sqlBuilder.append(" LIMIT 1;");
            return sqlBuilder.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Parse condition error,message=%s", e.getMessage()));
        }
    }
}
