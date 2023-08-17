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
package com.oceanbase.odc.service.rollbackplan.oboracle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.core.sql.execute.mapper.JdbcRowMapper;
import com.oceanbase.odc.core.sql.execute.model.JdbcQueryResult;
import com.oceanbase.odc.service.dml.DataConvertUtil;
import com.oceanbase.odc.service.rollbackplan.AbstractRollbackGenerator;
import com.oceanbase.odc.service.rollbackplan.RollBackPlanJdbcRowMapper;
import com.oceanbase.odc.service.rollbackplan.model.RollbackPlan;
import com.oceanbase.odc.service.rollbackplan.model.RollbackProperties;
import com.oceanbase.odc.service.rollbackplan.model.TableReferenece;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import com.oceanbase.tools.sqlparser.statement.select.ExpressionReference;
import com.oceanbase.tools.sqlparser.statement.select.FromReference;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;

/**
 * {@lnk OBOracleRollBackGenerator}
 *
 * @author jingtian
 * @date 2023/5/16
 * @since ODC_release_4.2.0
 */
public abstract class AbstractOBOracleRollBackGenerator extends AbstractRollbackGenerator {

    public AbstractOBOracleRollBackGenerator(String sql, JdbcOperations jdbcOperations,
            RollbackProperties rollbackProperties, Long timeOutMilliSeconds) {
        super(sql, jdbcOperations, rollbackProperties, timeOutMilliSeconds);
    }

    @Override
    protected boolean ifPrimaryOrUniqueKeyExists(Set<TableReferenece> changedTableNames,
            Map<TableReferenece, List<String>> table2PkNameList) {
        for (TableReferenece table : changedTableNames) {
            table2PkNameList.put(table, Arrays.asList("ROWID"));
        }
        return true;
    }

    @Override
    protected SqlBuilder getSqlBuilder() {
        return new OracleSqlBuilder();
    }

    protected void parseSubquery(FromReference reference) {
        ExpressionReference expressionReference = (ExpressionReference) reference;
        if (expressionReference.getTarget() instanceof SelectBody) {
            SelectBody selectBody = (SelectBody) expressionReference.getTarget();
            if (selectBody.getFroms().size() > 1) {
                throw new IllegalStateException(
                        "Does not support generating rollback plan for subquery SQL statements involving multi-table queries");
            } else {
                // 若子查询取了别名，则取别名作为为 changedTableNames，否则 changedTableNames 的 size 为 0
                if (expressionReference.getAlias() != null) {
                    this.changedTableNames.add(new TableReferenece(null, null, expressionReference.getAlias()));
                }
            }
        } else {
            throw new IllegalStateException("Unsupported sql statement:" + expressionReference.getText());
        }
    }

    @Override
    protected List<String> getBatchQuerySql() {
        ArrayList<String> returnVal = new ArrayList<>();

        SqlBuilder queryIdxDataSql = getSqlBuilder();
        queryIdxDataSql.append("SELECT ROWID ").append(" FROM ").append(getFromReference()).append(getWhereClause())
                .append(";");

        JdbcQueryResult idxData = queryData(queryIdxDataSql.toString());
        int batchSize = this.rollbackProperties.getQueryDataBatchSize();
        int changedLines = idxData.getRows().size();
        int maxChangeLines = this.rollbackProperties.getEachSqlMaxChangeLines();
        if (changedLines <= batchSize) {
            if (this.changedTableNames.size() == 0) {
                returnVal.add(getQuerySqlForEachChangedTable(null));
            } else {
                this.changedTableNames.forEach(item -> {
                    returnVal.add(getQuerySqlForEachChangedTable(item));
                });
            }
        } else if (changedLines < maxChangeLines) {
            int cycle = idxData.getRows().size() / batchSize;
            int remainder = idxData.getRows().size() % batchSize;
            List<List<Object>> rows = idxData.getRows();

            for (int i = 0; i < cycle + 1; i++) {
                checkTimeout();
                if (i == cycle && remainder == 0) {
                    break;
                }
                StringBuilder builder = new StringBuilder();
                builder.append("SELECT ");
                if (this.changedTableNames.size() != 0) {
                    this.changedTableNames.forEach(item -> {
                        builder.append(getSelectObject(item)).append(".").append("*");
                    });
                } else {
                    builder.append("*");
                }
                builder.append(" FROM ").append(getFromReference());
                if (!getWhereClause().equals("")) {
                    builder.append(" " + getWhereClause()).append(" AND ");
                } else {
                    builder.append(" WHERE ");
                }
                builder.append("ROWID IN (");

                for (int j = 0; j < batchSize; j++) {
                    if (i == cycle && j > remainder - 1) {
                        break;
                    }
                    String res = DataConvertUtil.convertToSqlString(DialectType.OB_MYSQL, OdcConstants.ROWID,
                            rows.get(i * batchSize + j).get(0).toString());
                    builder.append("'" + res + "'").append(",");
                }
                builder.deleteCharAt(builder.length() - 1).append(")");
                returnVal.add(builder.toString());
            }
        } else {
            throw new IllegalStateException("The number of changed lines exceeds " + maxChangeLines);
        }
        return returnVal;
    }

    @Override
    protected String getRollbackSqlPrefix() {
        return "INSERT INTO ";
    }

    @Override
    protected DialectType getDialectType() {
        return DialectType.OB_ORACLE;
    }

    @Override
    protected JdbcRowMapper getJdbcRowMapper() {
        return new RollBackPlanJdbcRowMapper(DialectType.OB_ORACLE, this.rollbackProperties.getDefaultTimeZone());
    }

    @Override
    protected RollbackPlan getRollbackPlan(String sql) {
        return new RollbackPlan(sql, DialectType.OB_ORACLE);
    }
}
