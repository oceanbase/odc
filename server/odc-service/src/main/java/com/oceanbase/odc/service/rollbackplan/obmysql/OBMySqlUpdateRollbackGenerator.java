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
import java.util.List;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.service.rollbackplan.model.RollbackProperties;
import com.oceanbase.odc.service.rollbackplan.model.TableReferenece;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import com.oceanbase.tools.sqlparser.OBMySQLParser;
import com.oceanbase.tools.sqlparser.SQLParser;
import com.oceanbase.tools.sqlparser.statement.select.FromReference;
import com.oceanbase.tools.sqlparser.statement.select.JoinReference;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;
import com.oceanbase.tools.sqlparser.statement.update.Update;
import com.oceanbase.tools.sqlparser.statement.update.UpdateAssign;

import lombok.NonNull;

/**
 * {@link OBMySqlUpdateRollbackGenerator}
 *
 * @author jingtian
 * @date 2023/5/16
 * @since ODC_release_4.2.0
 */
public class OBMySqlUpdateRollbackGenerator extends AbstractOBMySqlRollBackGenerator {
    private final SQLParser sqlParser;
    private Update update;

    public OBMySqlUpdateRollbackGenerator(@NonNull String sql, @NonNull Update update,
            @NonNull JdbcOperations jdbcOperations,
            @NonNull RollbackProperties rollbackProperties, Long timeOutMilliSeconds) {
        super(sql, jdbcOperations, rollbackProperties, timeOutMilliSeconds);
        this.sqlParser = new OBMySQLParser();
        this.update = update;
    }

    @Override
    protected void parseObjectChangedTableNames() {
        List<TableReferenece> tableReferences = new ArrayList<>();
        for (FromReference tableReference : this.update.getTableReferences()) {
            if (tableReference instanceof NameReference) {
                NameReference reference = (NameReference) tableReference;
                tableReferences
                        .add(new TableReferenece(reference.getSchema(), reference.getRelation(), reference.getAlias()));
            } else if (tableReference instanceof JoinReference) {
                tableReferences.addAll(parseJoinReference((JoinReference) tableReference));
            } else {
                throw new IllegalStateException("Unsupported sql statement:" + tableReference.getText());
            }
        }

        for (UpdateAssign assign : this.update.getAssignList()) {
            assign.getLeftList().forEach(col -> {
                if (col.getRelation() == null) {
                    if (tableReferences.size() > 1) {
                        throw new UnsupportedOperationException(
                                "Unsupported sql statement, missing table relation:" + assign.getText());
                    }
                    this.changedTableNames.add(tableReferences.get(0));
                } else {
                    for (TableReferenece item : tableReferences) {
                        if (item.getAlias() != null && item.getAlias().equals(col.getRelation())) {
                            this.changedTableNames.add(
                                    new TableReferenece(item.getSchemaName(), item.getTableName(), item.getAlias()));
                            break;
                        } else if (item.getTableName().equals(col.getRelation())) {
                            this.changedTableNames.add(
                                    new TableReferenece(item.getSchemaName(), item.getTableName(), item.getAlias()));
                            break;
                        }
                    }
                }
            });
        }
    }

    @Override
    protected String getFromReference() {
        StringBuilder fromBuilder = new StringBuilder();
        this.update.getTableReferences().forEach(item -> fromBuilder.append(item.getText() + ","));
        fromBuilder.deleteCharAt(fromBuilder.length() - 1);
        return fromBuilder.toString();
    }

    @Override
    protected String getWhereClause() {
        SqlBuilder whereClause = getSqlBuilder();
        if (this.update.getWhere() != null) {
            whereClause.space().append("WHERE ").append(this.update.getWhere().getText());
        }
        return whereClause.toString();
    }

    @Override
    protected String getOrderByAndLimit() {
        SqlBuilder sqlBuilder = getSqlBuilder();
        if (this.update.getOrderBy() != null) {
            sqlBuilder.space().append(this.update.getOrderBy().getText());
        }
        if (this.update.getLimit() != null) {
            sqlBuilder.space().append(this.update.getLimit().getText());
        }
        return sqlBuilder.toString();
    }

    @Override
    protected String getRollbackSqlPrefix() {
        return "REPLACE INTO ";
    }

}
