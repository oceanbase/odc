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
import com.oceanbase.tools.sqlparser.statement.delete.Delete;
import com.oceanbase.tools.sqlparser.statement.delete.DeleteRelation;
import com.oceanbase.tools.sqlparser.statement.delete.MultiDelete;
import com.oceanbase.tools.sqlparser.statement.select.FromReference;
import com.oceanbase.tools.sqlparser.statement.select.JoinReference;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;

import lombok.NonNull;

/**
 * {@link OBMySqlDeleteRollbackGenerator}
 *
 * @author jingtian
 * @date 2023/5/16
 * @since ODC_release_4.2.0
 */
public class OBMySqlDeleteRollbackGenerator extends AbstractOBMySqlRollBackGenerator {
    private final SQLParser sqlParser;
    private Delete delete;

    public OBMySqlDeleteRollbackGenerator(@NonNull String sql, @NonNull Delete delete,
            @NonNull JdbcOperations jdbcOperations,
            @NonNull RollbackProperties rollbackProperties, Long timeOutMilliSeconds) {
        super(sql, jdbcOperations, rollbackProperties, timeOutMilliSeconds);
        this.sqlParser = new OBMySQLParser();
        this.delete = delete;
    }

    @Override
    protected void parseObjectChangedTableNames() {
        if (this.delete.getSingleDelete() != null) {
            FromReference singleDelete = this.delete.getSingleDelete();
            NameReference reference = (NameReference) singleDelete;
            this.changedTableNames
                    .add(new TableReferenece(reference.getSchema(), reference.getRelation(), reference.getAlias()));
            return;
        }

        MultiDelete multiDelete = this.delete.getMultiDelete();
        List<TableReferenece> tableReferences = new ArrayList<>();
        for (FromReference reference : multiDelete.getTableReferences()) {
            if (reference instanceof NameReference) {
                NameReference nameReference = (NameReference) reference;
                tableReferences.add(new TableReferenece(nameReference.getSchema(), nameReference.getRelation(),
                        nameReference.getAlias()));
            } else if (reference instanceof JoinReference) {
                tableReferences.addAll(parseJoinReference((JoinReference) reference));
            } else {
                throw new IllegalStateException("Unsupported sql statement:" + reference.getText());
            }
        }

        for (DeleteRelation deleteRelation : multiDelete.getDeleteRelations()) {
            for (TableReferenece item : tableReferences) {
                if (item.getAlias() != null && item.getAlias().equals(deleteRelation.getTable())) {
                    this.changedTableNames
                            .add(new TableReferenece(item.getSchemaName(), item.getTableName(), item.getAlias()));
                    break;
                } else if (item.getTableName().equals(deleteRelation.getTable())) {
                    this.changedTableNames
                            .add(new TableReferenece(item.getSchemaName(), item.getTableName(), item.getAlias()));
                    break;
                }
            }
        }
    }

    @Override
    protected String getFromReference() {
        StringBuilder fromBuilder = new StringBuilder();
        if (this.delete.getSingleDelete() != null) {
            fromBuilder.append(this.delete.getSingleDelete().getText());
            return fromBuilder.toString();
        }
        this.delete.getMultiDelete().getTableReferences().forEach(item -> fromBuilder.append(item.getText() + ", "));
        fromBuilder.delete(fromBuilder.length() - 2, fromBuilder.length());
        return fromBuilder.toString();
    }

    @Override
    protected String getWhereClause() {
        SqlBuilder whereClause = getSqlBuilder();
        if (this.delete.getWhere() != null) {
            whereClause.space().append("WHERE ").append(this.delete.getWhere().getText());
        }
        return whereClause.toString();
    }

    @Override
    protected String getOrderByAndLimit() {
        SqlBuilder sqlBuilder = getSqlBuilder();
        if (this.delete.getOrderBy() != null) {
            sqlBuilder.space().append(this.delete.getOrderBy().getText());
        }
        if (this.delete.getLimit() != null) {
            sqlBuilder.space().append(this.delete.getLimit().getText());
        }
        return sqlBuilder.toString();
    }

    @Override
    protected String getRollbackSqlPrefix() {
        return "INSERT INTO ";
    }

}
