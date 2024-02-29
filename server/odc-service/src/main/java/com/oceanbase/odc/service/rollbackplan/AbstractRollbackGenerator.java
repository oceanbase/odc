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
package com.oceanbase.odc.service.rollbackplan;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.StatementCallback;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.mapper.JdbcRowMapper;
import com.oceanbase.odc.core.sql.execute.model.JdbcColumnMetaData;
import com.oceanbase.odc.core.sql.execute.model.JdbcQueryResult;
import com.oceanbase.odc.service.dml.DataConvertUtil;
import com.oceanbase.odc.service.rollbackplan.model.RollbackPlan;
import com.oceanbase.odc.service.rollbackplan.model.RollbackProperties;
import com.oceanbase.odc.service.rollbackplan.model.TableReferenece;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import com.oceanbase.tools.sqlparser.statement.select.FromReference;
import com.oceanbase.tools.sqlparser.statement.select.JoinReference;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;

/**
 * {@link AbstractRollbackGenerator}
 *
 * @author jingtian
 * @date 2023/5/16
 * @since ODC_release_4.2.0
 */
public abstract class AbstractRollbackGenerator implements GenerateRollbackPlan {
    protected final JdbcOperations jdbcOperations;
    private final JdbcRowMapper jdbcRowMapper;
    private final String sql;
    protected RollbackProperties rollbackProperties;
    protected Set<TableReferenece> changedTableNames = new HashSet<>();
    protected Map<TableReferenece, List<String>> table2PkNameList = new HashMap<>();
    private final List<String> unsupportedColumnTypeNames =
            Arrays.asList("TINYBLOB", "MEDIUMBLOB", "LONGBLOB", "BLOB", "CLOB", "RAW");
    private final Long timeOutMilliSeconds;
    private long startTimeMilliSeconds;

    public AbstractRollbackGenerator(String sql, JdbcOperations jdbcOperations, RollbackProperties rollbackProperties,
            Long timeOutMilliSeconds) {
        this.sql = sql;
        this.jdbcOperations = jdbcOperations;
        this.rollbackProperties = rollbackProperties;
        this.jdbcRowMapper = getJdbcRowMapper();
        this.timeOutMilliSeconds = timeOutMilliSeconds;
    }

    @Override
    public RollbackPlan generate() {
        this.startTimeMilliSeconds = System.currentTimeMillis();
        RollbackPlan rollbackPlan = getRollbackPlan(sql);
        try {
            checkStatementSupported();
            parseObjectChangedTableNames();
            if (!ifPrimaryOrUniqueKeyExists(changedTableNames, table2PkNameList)) {
                rollbackPlan.setErrorMessage(
                        "It is not supported to generate rollback plan for tables without primary key or unique key");
                return rollbackPlan;
            }
        } catch (Exception e) {
            rollbackPlan.setErrorMessage(e.getMessage());
            return rollbackPlan;
        }
        return doGenerate(sql);
    }

    private RollbackPlan doGenerate(String sql) {
        RollbackPlan rollbackPlan = getRollbackPlan(sql);
        List<String> querySqls = new ArrayList<>();
        List<String> batchQuerySqls = new ArrayList<>();
        List<String> rollbackSql = new ArrayList<>();

        try {
            querySqls = getQuerySqls(changedTableNames);
            rollbackPlan.setQuerySqls(querySqls);
            batchQuerySqls.addAll(getBatchQuerySql());
        } catch (Exception e) {
            rollbackPlan.setErrorMessage(e.getMessage());
            return rollbackPlan;
        }

        String addSql = addRollbackSqlForUpdateStmt();
        if (!StringUtils.isBlank(addSql)) {
            rollbackSql.add(addSql);
        }
        int rowCount = 0;
        for (String batchQuerySql : batchQuerySqls) {
            try {
                checkTimeout();
                JdbcQueryResult queryResult = queryData(batchQuerySql);
                rowCount += queryResult.getRows().size();
                rollbackSql.addAll(getRollbackSql(queryResult));
            } catch (Exception e) {
                rollbackPlan.setErrorMessage(
                        "Failed to get rollback sql, error message = " + e);
                return rollbackPlan;
            }
        }
        rollbackPlan.setRollbackSqls(rollbackSql);
        rollbackPlan.setChangeLineCount(rowCount);
        return rollbackPlan;
    }

    private List<String> getQuerySqls(Set<TableReferenece> changedTableNames) {
        List<String> returnVal = new ArrayList<>();

        if (changedTableNames.size() != 0) {
            changedTableNames.forEach(table -> {
                returnVal.add(getQuerySqlForEachChangedTable(table));
            });
        } else {
            returnVal.add(getQuerySqlForEachChangedTable(null));
        }
        return returnVal;
    }

    protected JdbcQueryResult queryData(String sql) {
        JdbcQueryResult jdbcQueryResult = this.jdbcOperations.execute(new StatementCallback<JdbcQueryResult>() {
            @Override
            public JdbcQueryResult doInStatement(Statement stmt) throws SQLException, DataAccessException {
                String querySql = (needRemoveDelimiter() && StringUtils.isNotBlank(sql) && sql.trim().endsWith(";"))
                        ? sql.trim().substring(0, sql.length() - 1)
                        : sql;
                ResultSet resultSet = stmt.executeQuery(querySql);
                JdbcQueryResult jdbcQueryResult = new JdbcQueryResult(resultSet.getMetaData(), jdbcRowMapper);
                while (resultSet.next()) {
                    try {
                        jdbcQueryResult.addLine(resultSet);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                return jdbcQueryResult;
            }
        });
        if (jdbcQueryResult.getRows().size() == 0) {
            throw new IllegalStateException("The number of data change rows is 0");
        }
        return jdbcQueryResult;
    }

    protected boolean needRemoveDelimiter() {
        return false;
    }

    protected void parseMetadata(JdbcQueryResult result, List<String> columnNames, Map<String, String> typeNames) {
        List<JdbcColumnMetaData> columnMetaDataList = result.getMetaData().getColumns();
        for (JdbcColumnMetaData columnMetaData : columnMetaDataList) {
            String columnTypeName = columnMetaData.getColumnTypeName();
            if (unsupportedColumnTypeNames.contains(columnTypeName)) {
                throw new UnsupportedOperationException(
                        "Unsupported column type for generating rollback sql, column type : " + columnTypeName);
            }
            columnNames.add(columnMetaData.getColumnName());
            typeNames.put(columnMetaData.getColumnName(), columnTypeName);
        }
    }

    protected String getQuerySqlForEachChangedTable(TableReferenece table) {
        SqlBuilder sqlBuilder = getSqlBuilder();
        sqlBuilder.append("SELECT ");
        if (table != null) {
            sqlBuilder.append(getSelectObject(table)).append(".").append("*");
        } else {
            sqlBuilder.append("*");
        }
        sqlBuilder.append(" FROM ")
                .append(getFromReference())
                .append(getWhereClause())
                .append(getOrderByAndLimit())
                .append(";");
        return sqlBuilder.toString();
    }

    protected String getSelectObject(TableReferenece referenece) {
        SqlBuilder builder = getSqlBuilder();
        if (referenece.getAlias() == null) {
            if (referenece.getSchemaName() != null) {
                builder.append(referenece.getSchemaName()).append(".");
            }
            builder.append(referenece.getTableName());
        } else {
            builder.append(referenece.getAlias());
        }
        return builder.toString();
    }

    protected List<TableReferenece> parseJoinReference(JoinReference joinReference) {
        List<TableReferenece> returnVal = new ArrayList<>();
        FromReference left = joinReference.getLeft();
        FromReference right = joinReference.getRight();
        if (left instanceof JoinReference) {
            returnVal.addAll(parseJoinReference((JoinReference) left));
        } else if (left instanceof NameReference) {
            NameReference reference = (NameReference) left;
            returnVal.add(new TableReferenece(reference.getSchema(), reference.getRelation(), reference.getAlias()));
        }
        if (right instanceof JoinReference) {
            returnVal.addAll(parseJoinReference((JoinReference) right));
        } else if (right instanceof NameReference) {
            NameReference reference = (NameReference) right;
            returnVal.add(new TableReferenece(reference.getSchema(), reference.getRelation(), reference.getAlias()));
        }
        return returnVal;
    }

    private List<String> getRollbackSql(JdbcQueryResult queryResult) {
        List<String> returnVal = new ArrayList<>();

        List<JdbcColumnMetaData> columnMetaDataList = queryResult.getMetaData().getColumns();
        List<String> columnNames = new ArrayList<>();
        Map<String, String> typeNames = new HashMap<>();
        parseMetadata(queryResult, columnNames, typeNames);

        String schema = columnMetaDataList.get(0).getCatalogName();
        String table = columnMetaDataList.get(0).getTableName();
        for (List<Object> row : queryResult.getRows()) {
            SqlBuilder sqlBuilder = getSqlBuilder();
            sqlBuilder.append(getRollbackSqlPrefix());
            if ("".equals(schema)) {
                sqlBuilder.append(getFromReference()).space().append("VALUES (");
            } else {
                sqlBuilder.identifier(schema).append(".").identifier(table).space().append("VALUES (");
            }
            for (int i = 0; i < row.size(); i++) {
                String res = DataConvertUtil.convertToSqlString(getDialectType(), typeNames.get(columnNames.get(i)),
                        Objects.isNull(row.get(i)) ? null : row.get(i).toString());
                if (i == row.size() - 1) {
                    sqlBuilder.append(res + ");");
                } else {
                    sqlBuilder.append(res + ",");
                }
            }
            returnVal.add(sqlBuilder.toString());
        }
        return returnVal;
    }

    protected void checkTimeout() {
        if (Objects.nonNull(this.timeOutMilliSeconds)
                && System.currentTimeMillis() - this.startTimeMilliSeconds > this.timeOutMilliSeconds) {
            throw new RuntimeException(
                    "Timeout for generating rollback plan, timeout millisecond=" + this.timeOutMilliSeconds);
        }
    }

    protected abstract boolean ifPrimaryOrUniqueKeyExists(Set<TableReferenece> changedTableNames,
            Map<TableReferenece, List<String>> table2PkNameList);

    protected abstract void parseObjectChangedTableNames();

    protected abstract SqlBuilder getSqlBuilder();

    protected abstract String getFromReference();

    protected abstract String getWhereClause();

    protected abstract String getOrderByAndLimit();

    protected abstract List<String> getBatchQuerySql();

    protected abstract String addRollbackSqlForUpdateStmt();

    protected abstract String getRollbackSqlPrefix();

    protected abstract DialectType getDialectType();

    protected abstract void checkStatementSupported();

    protected abstract JdbcRowMapper getJdbcRowMapper();

    protected abstract RollbackPlan getRollbackPlan(String sql);

}
