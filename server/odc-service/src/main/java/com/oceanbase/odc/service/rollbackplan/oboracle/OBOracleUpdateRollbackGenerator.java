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

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.service.rollbackplan.model.RollbackProperties;
import com.oceanbase.odc.service.rollbackplan.model.TableReferenece;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import com.oceanbase.tools.sqlparser.OBOracleSQLParser;
import com.oceanbase.tools.sqlparser.SQLParser;
import com.oceanbase.tools.sqlparser.statement.select.ExpressionReference;
import com.oceanbase.tools.sqlparser.statement.select.FromReference;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;
import com.oceanbase.tools.sqlparser.statement.update.Update;

import lombok.NonNull;

/**
 * {@link OBOracleUpdateRollbackGenerator}
 *
 * @author jingtian
 * @date 2023/5/16
 * @since ODC_release_4.2.0
 */
public class OBOracleUpdateRollbackGenerator extends AbstractOBOracleRollBackGenerator {
    private final SQLParser sqlParser;
    private Update update;

    public OBOracleUpdateRollbackGenerator(@NonNull String sql, @NonNull Update update,
            @NonNull JdbcOperations jdbcOperations,
            @NonNull RollbackProperties rollbackProperties, Long timeOutMilliSeconds) {
        super(sql, jdbcOperations, rollbackProperties, timeOutMilliSeconds);
        this.sqlParser = new OBOracleSQLParser();
        this.update = update;
    }

    @Override
    protected void parseObjectChangedTableNames() {
        FromReference reference = this.update.getTableReferences().get(0);
        if (reference instanceof NameReference) {
            NameReference nameReference = (NameReference) reference;
            this.changedTableNames.add(new TableReferenece(nameReference.getSchema(), nameReference.getRelation(),
                    nameReference.getAlias()));
        } else if (reference instanceof ExpressionReference) {
            parseSubquery(reference);
        }
    }

    @Override
    protected String getFromReference() {
        StringBuilder fromBuilder = new StringBuilder();
        fromBuilder.append(this.update.getTableReferences().get(0).getText());
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
        return "";
    }

    @Override
    protected String addRollbackSqlForUpdateStmt() {
        SqlBuilder sqlBuilder = getSqlBuilder();
        sqlBuilder.append("DELETE FROM ")
                .append(getFromReference())
                .append(getWhereClause())
                .append(";");
        return sqlBuilder.toString();
    }

    @Override
    protected void checkStatementSupported() {
        if (this.update.getAssignList().size() == 0) {
            throw new IllegalStateException("Unsupported update statement, cannot generate rollback sql");
        }
    }
}
