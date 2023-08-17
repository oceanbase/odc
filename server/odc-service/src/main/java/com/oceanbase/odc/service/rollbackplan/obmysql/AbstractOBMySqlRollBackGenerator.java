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
package com.oceanbase.odc.service.rollbackplan.obmysql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.mapper.JdbcRowMapper;
import com.oceanbase.odc.core.sql.execute.model.JdbcQueryResult;
import com.oceanbase.odc.service.dml.DataConvertUtil;
import com.oceanbase.odc.service.rollbackplan.AbstractRollbackGenerator;
import com.oceanbase.odc.service.rollbackplan.RollBackPlanJdbcRowMapper;
import com.oceanbase.odc.service.rollbackplan.model.RollbackPlan;
import com.oceanbase.odc.service.rollbackplan.model.RollbackProperties;
import com.oceanbase.odc.service.rollbackplan.model.TableReferenece;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

/**
 * {@link AbstractOBMySqlRollBackGenerator}
 *
 * @author jingtian
 * @date 2023/5/16
 * @since ODC_release_4.2.0
 */
public abstract class AbstractOBMySqlRollBackGenerator extends AbstractRollbackGenerator {
    public AbstractOBMySqlRollBackGenerator(String sql, JdbcOperations jdbcOperations,
            RollbackProperties rollbackProperties, Long timeOutMilliSeconds) {
        super(sql, jdbcOperations, rollbackProperties, timeOutMilliSeconds);
    }

    @Override
    protected boolean ifPrimaryOrUniqueKeyExists(Set<TableReferenece> changedTableNames,
            Map<TableReferenece, List<String>> table2PkNameList) {
        for (TableReferenece table : changedTableNames) {
            List<String> pkNameList = getPkOrUqColumnList(table.getSchemaName(), table.getTableName());
            if (pkNameList.size() == 0) {
                return false;
            }
            table2PkNameList.put(table, pkNameList);
        }
        return true;
    }

    @Override
    protected SqlBuilder getSqlBuilder() {
        return new MySQLSqlBuilder();
    }

    private List<String> getPkOrUqColumnList(String schemaName, String tableName) {
        List<String> keyList = getPrimarykeyColumnList(schemaName, tableName);
        if (keyList.size() != 0) {
            return keyList;
        }
        keyList = getUniqueKeyColumnList(schemaName, tableName);
        return keyList;
    }

    private List<String> getPrimarykeyColumnList(String schemaName, String tableName) {
        List<String> pkNameList = new ArrayList<>();
        MySQLSqlBuilder sqlBuilder = new MySQLSqlBuilder();
        sqlBuilder.append("SHOW INDEX FROM ");
        if (schemaName != null) {
            sqlBuilder.append(schemaName).append(".");
        }
        sqlBuilder.append(tableName).append(" WHERE Non_unique = 0 and Key_name='PRIMARY';");
        this.jdbcOperations.query(sqlBuilder.toString(), (rs, num) -> {
            pkNameList.add(rs.getString(5));
            return null;
        });
        return pkNameList;
    }

    private List<String> getUniqueKeyColumnList(String schemaName, String tableName) {
        List<String> upNameList = new ArrayList<>();
        MySQLSqlBuilder sqlBuilder = new MySQLSqlBuilder();
        sqlBuilder.append("SHOW INDEX FROM ");
        if (schemaName != null) {
            sqlBuilder.append(schemaName).append(".");
        }
        sqlBuilder.append(tableName).append(" WHERE Non_unique = 0;");
        AtomicReference keyName = new AtomicReference();
        this.jdbcOperations.query(sqlBuilder.toString(), (rs, num) -> {
            if (num == 0) {
                keyName.set(rs.getString(3));
            }
            if (keyName.get().toString().equals(rs.getString(3))) {
                upNameList.add(rs.getString(5));
            }
            return null;
        });
        return upNameList;
    }

    @Override
    protected List<String> getBatchQuerySql() {
        ArrayList<String> returnVal = new ArrayList<>();
        AtomicInteger totalChangeLines = new AtomicInteger();
        this.table2PkNameList.forEach((table, idxColumn) -> {
            MySQLSqlBuilder queryIdxDataSql = new MySQLSqlBuilder();
            queryIdxDataSql.append("SELECT ");
            for (int i = 0; i < idxColumn.size(); i++) {
                queryIdxDataSql.append(getSelectObject(table)).append(".").append(idxColumn.get(i));
                if (i != idxColumn.size() - 1) {
                    queryIdxDataSql.append(", ");
                }
            }
            queryIdxDataSql.append(" FROM ")
                    .append(getFromReference())
                    .append(getWhereClause())
                    .append(getOrderByAndLimit())
                    .append(";");

            JdbcQueryResult idxData = queryData(queryIdxDataSql.toString());
            int singleTableChangedLines = idxData.getRows().size();
            totalChangeLines.addAndGet(singleTableChangedLines);
            int batchSize = this.rollbackProperties.getQueryDataBatchSize();
            int maxChangeLine = this.rollbackProperties.getEachSqlMaxChangeLines();
            if (totalChangeLines.get() > maxChangeLine) {
                throw new IllegalStateException("The number of changed lines exceeds " + maxChangeLine);
            }
            if (singleTableChangedLines <= batchSize) {
                returnVal.add(getQuerySqlForEachChangedTable(table));
            } else {
                int cycle = idxData.getRows().size() / batchSize;
                int remainder = idxData.getRows().size() % batchSize;
                List<List<Object>> rows = idxData.getRows();

                List<String> columnNames = new ArrayList<>();
                Map<String, String> typeNames = new HashMap<>();
                parseMetadata(idxData, columnNames, typeNames);

                for (int i = 0; i < cycle + 1; i++) {
                    checkTimeout();
                    if (i == cycle && remainder == 0) {
                        break;
                    }
                    StringBuilder builder = new StringBuilder();
                    builder.append("SELECT ")
                            .append(getSelectObject(table)).append(".").append("*")
                            .append(" FROM ")
                            .append(getFromReference());
                    if (!getWhereClause().equals("")) {
                        builder.append(getWhereClause()).append(" AND ");
                    } else {
                        builder.append(" WHERE ");
                    }
                    int idxCount = idxColumn.size();
                    for (int m = 0; m < idxCount; m++) {
                        if (m == 0) {
                            builder.append("(");
                        }
                        builder.append(getSelectObject(table)).append(".").append("`" + idxColumn.get(m) + "`");
                        if (m != idxColumn.size() - 1) {
                            builder.append(", ");
                        } else {
                            builder.append(") IN (");
                        }
                    }

                    for (int j = 0; j < batchSize; j++) {
                        if (i == cycle && j > remainder - 1) {
                            break;
                        }
                        for (int k = 0; k < idxCount; k++) {
                            String res = DataConvertUtil.convertToSqlString(DialectType.OB_MYSQL,
                                    typeNames.get(columnNames.get(k)),
                                    rows.get(i * batchSize + j).get(k).toString());
                            if (k == 0) {
                                if (idxCount == 1) {
                                    builder.append(res);
                                } else {
                                    builder.append("(").append(res);
                                }
                            } else if (k == idxColumn.size() - 1) {
                                if (idxCount == 1) {
                                    builder.append(res);
                                } else {
                                    builder.append(res).append(")");
                                }
                            } else {
                                builder.append(res);
                            }
                            builder.append(", ");
                        }
                    }
                    builder.delete(builder.length() - 2, builder.length());
                    builder.append(") ").append(getOrderByAndLimit())
                            .append(";");
                    returnVal.add(builder.toString());
                }
            }
        });
        return returnVal;
    }

    @Override
    protected String addRollbackSqlForUpdateStmt() {
        return "";
    }

    @Override
    protected DialectType getDialectType() {
        return DialectType.OB_MYSQL;
    }

    @Override
    protected void checkStatementSupported() {}

    @Override
    protected JdbcRowMapper getJdbcRowMapper() {
        return new RollBackPlanJdbcRowMapper(DialectType.OB_MYSQL, null);
    }

    @Override
    protected RollbackPlan getRollbackPlan(String sql) {
        return new RollbackPlan(sql, DialectType.OB_MYSQL);
    }
}
