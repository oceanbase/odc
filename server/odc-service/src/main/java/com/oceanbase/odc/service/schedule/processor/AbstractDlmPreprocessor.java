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
package com.oceanbase.odc.service.schedule.processor;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.ConnectionCallback;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.dlm.model.DataArchiveTableConfig;
import com.oceanbase.odc.service.dlm.model.OffsetConfig;
import com.oceanbase.odc.service.dlm.utils.DataArchiveConditionUtil;
import com.oceanbase.odc.service.plugin.SchemaPluginUtil;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeParams;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/7/13 19:47
 * @Descripition:
 */

@Slf4j
public class AbstractDlmPreprocessor implements Preprocessor {

    @Override
    public void process(ScheduleChangeParams req) {}

    public List<DataArchiveTableConfig> getAllTables(ConnectionSession sourceSession, String schemaName) {
        return Objects.requireNonNull(sourceSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<List<DBObjectIdentity>>) con -> SchemaPluginUtil.getTableExtension(
                        sourceSession.getDialectType())
                        .list(con, schemaName, DBObjectType.TABLE)))
                .stream().map(o -> {
                    DataArchiveTableConfig config = new DataArchiveTableConfig();
                    config.setTableName(o.getName());
                    return config;
                }).collect(Collectors.toList());
    }

    public void checkTableAndCondition(ConnectionSession connectionSession, Database sourceDb,
            List<DataArchiveTableConfig> tables,
            List<OffsetConfig> variables) {
        checkShardKey(connectionSession, sourceDb.getName(), tables);
        Map<DataArchiveTableConfig, String> sqlMap = getDataArchiveSqls(sourceDb, tables, variables);
        checkDataArchiveSql(connectionSession, sqlMap);
    }


    private void checkShardKey(ConnectionSession connectionSession, String databaseName,
            List<DataArchiveTableConfig> tables) {
        SyncJdbcExecutor syncJdbcExecutor = connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY);
        SqlBuilder sqlBuilder;
        if (connectionSession.getDialectType().isMysql()) {
            sqlBuilder = new MySQLSqlBuilder();
            sqlBuilder.append("SELECT DISTINCT s.TABLE_NAME FROM ");
            sqlBuilder.append("information_schema.STATISTICS s ");
            sqlBuilder.append("JOIN information_schema.COLUMNS c ");
            sqlBuilder.append("ON s.TABLE_SCHEMA = c.TABLE_SCHEMA ");
            sqlBuilder.append("AND s.TABLE_NAME = c.TABLE_NAME ");
            sqlBuilder.append("AND s.COLUMN_NAME = c.COLUMN_NAME ");
            sqlBuilder.append("WHERE s.NON_UNIQUE = 0 AND s.TABLE_SCHEMA = '");
            sqlBuilder.append(databaseName);
            sqlBuilder.append("' GROUP BY s.TABLE_NAME, s.INDEX_NAME ");
            sqlBuilder.append(" HAVING COUNT(*) = SUM(CASE WHEN c.IS_NULLABLE = 'NO' THEN 1 ELSE 0 END)");
        } else if (connectionSession.getDialectType().isPostgreSql()) {
            sqlBuilder = new MySQLSqlBuilder();
            sqlBuilder.append(String.format("SELECT DISTINCT c.relname AS table_name "
                    + "FROM pg_class c "
                    + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                    + "JOIN pg_index i ON c.oid = i.indrelid "
                    + "WHERE i.indisunique = TRUE "
                    + "  AND (c.relkind = 'r' or c.relkind = 'p') "
                    + "  AND n.nspname = '%s'; ", databaseName));
        } else {
            sqlBuilder = new OracleSqlBuilder();
            sqlBuilder.append(
                    String.format("select table_name from all_constraints where constraint_type = 'P' and owner = '%s'",
                            databaseName));
        }
        HashSet<String> tableNames =
                new HashSet<>(syncJdbcExecutor.query(sqlBuilder.toString(), (rs, num) -> rs.getString(1)));
        tables.forEach(tableConfig -> {
            if (!tableNames.contains(tableConfig.getTableName())) {
                throw new IllegalArgumentException(
                        String.format("The table must contain a non-empty unique index,tableName=%s",
                                tableConfig.getTableName()));
            }
        });
    }

    private void checkDataArchiveSql(ConnectionSession connectionSession, Map<DataArchiveTableConfig, String> sqlMap) {
        SyncJdbcExecutor syncJdbcExecutor = connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY);
        // Ensure the conditions are valid when executing.
        sqlMap.forEach((key, value) -> {
            try {
                if (connectionSession.getDialectType() == DialectType.ORACLE) {
                    syncJdbcExecutor.execute("EXPLAIN PLAN FOR " + value);
                } else {
                    syncJdbcExecutor.execute("explain " + value);
                }
            } catch (Exception e) {
                log.warn("Test condition failed,sql={}", value, e);
                throw new IllegalArgumentException(String.format("Condition is not supported!TableName=%s,Condition=%s",
                        key.getTableName(), key.getConditionExpression()));
            }
        });
    }

    public Map<DataArchiveTableConfig, String> getDataArchiveSqls(Database database,
            List<DataArchiveTableConfig> tables,
            List<OffsetConfig> variables) {
        Map<DataArchiveTableConfig, String> sqlMap = new HashMap<>();
        tables.forEach(table -> sqlMap.put(table, generateTestSql(database, table, variables)));
        return sqlMap;
    }

    private String generateTestSql(Database database, DataArchiveTableConfig table, List<OffsetConfig> variables) {
        try {
            DialectType dbType = database.getDataSource().getDialectType();
            if (!dbType.isOracle() && !dbType.isMysql() && !dbType.isPostgreSql()) {
                throw new UnsupportedException();
            }
            SqlBuilder sqlBuilder = dbType.isMysql() ? new MySQLSqlBuilder() : new OracleSqlBuilder();
            sqlBuilder.append("SELECT 1 FROM ").identifier(database.getName(), table.getTableName());
            if (StringUtils.isNotEmpty(table.getConditionExpression())) {
                sqlBuilder.append(" WHERE ")
                        .append(DataArchiveConditionUtil.parseCondition(table.getConditionExpression(),
                                variables, new Date()));
            }
            return sqlBuilder.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Parse condition error,message=%s", e.getMessage()));
        }
    }

    public void supportDataArchivingLink(ConnectionConfig sourceDs, ConnectionConfig targetDs) {
        if (StringUtils.isNotEmpty(sourceDs.getCloudProvider())
                && !sourceDs.getCloudProvider().equals(targetDs.getCloudProvider())) {
            throw new UnsupportedException(
                    String.format("Unsupported data link from %s to %s.", sourceDs.getCloudProvider(),
                            targetDs.getCloudProvider()));
        }
        if (StringUtils.isNotEmpty(sourceDs.getRegion()) && !sourceDs.getRegion().equals(targetDs.getRegion())) {
            throw new UnsupportedException(
                    String.format("Unsupported data link from %s to %s.", sourceDs.getRegion(),
                            targetDs.getRegion()));
        }
        if (sourceDs.getDialectType().isMysql()) {
            if (!targetDs.getDialectType().isMysql() && targetDs.getDialectType() != DialectType.FILE_SYSTEM) {
                throw new UnsupportedException(
                        String.format("Unsupported data link from %s to %s.", sourceDs.getDialectType(),
                                targetDs.getDialectType()));
            }
        }
        if (sourceDs.getDialectType().isOracle()) {
            if (!targetDs.getDialectType().isOracle()) {
                throw new UnsupportedException(
                        String.format("Unsupported data link from %s to %s.", sourceDs.getDialectType(),
                                targetDs.getDialectType()));
            }
        }
    }


}
