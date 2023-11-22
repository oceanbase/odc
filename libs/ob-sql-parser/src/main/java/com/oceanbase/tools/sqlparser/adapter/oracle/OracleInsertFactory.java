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
package com.oceanbase.tools.sqlparser.adapter.oracle;

import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Conditional_insert_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Insert_single_table_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Insert_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Insert_table_clause_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Multi_table_insertContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Single_table_insertContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.insert.Insert;
import com.oceanbase.tools.sqlparser.statement.insert.InsertBody;
import com.oceanbase.tools.sqlparser.statement.insert.MultiTableInsert;
import com.oceanbase.tools.sqlparser.statement.insert.SingleTableInsert;

import lombok.NonNull;

/**
 * {@link OracleInsertFactory}
 *
 * @author yh263208
 * @date 2022-12-20 19:23
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class OracleInsertFactory extends OBParserBaseVisitor<Insert> implements StatementFactory<Insert> {

    private final Insert_stmtContext insertStmtContext;

    public OracleInsertFactory(@NonNull Insert_stmtContext insertStmtContext) {
        this.insertStmtContext = insertStmtContext;
    }

    @Override
    public Insert generate() {
        return visit(this.insertStmtContext);
    }

    @Override
    public Insert visitInsert_stmt(Insert_stmtContext ctx) {
        if (ctx.single_table_insert() != null) {
            Single_table_insertContext single = ctx.single_table_insert();
            InsertBody body = new InsertBody(single);
            if (single.column_list() != null) {
                body.setColumns(single.column_list().column_definition_ref().stream().map(c -> {
                    StatementFactory<ColumnReference> factory = new OracleColumnRefFactory(c);
                    return factory.generate();
                }).collect(Collectors.toList()));
            }
            return new SingleTableInsert(ctx, body);
        } else if (ctx.multi_table_insert() != null) {
            Multi_table_insertContext multi = ctx.multi_table_insert();
            List<InsertBody> insertBodies;
            if (multi.insert_table_clause_list() != null) {
                insertBodies = visitInsertTableClauseList(multi.insert_table_clause_list());
            } else {
                Conditional_insert_clauseContext c = multi.conditional_insert_clause();
                insertBodies = c.condition_insert_clause_list().condition_insert_clause().stream()
                        .flatMap(i -> visitInsertTableClauseList(i.insert_table_clause_list()).stream())
                        .collect(Collectors.toList());
                if (c.insert_table_clause_list() != null) {
                    insertBodies.addAll(visitInsertTableClauseList(c.insert_table_clause_list()));
                }
            }
            return new MultiTableInsert(ctx, insertBodies);
        } else {
            throw new IllegalStateException("Missing insert body statement");
        }
    }

    public List<InsertBody> visitInsertTableClauseList(Insert_table_clause_listContext ctx) {
        return ctx.insert_single_table_clause().stream()
                .map(this::visitInsertSingleTableClause).collect(Collectors.toList());
    }

    public InsertBody visitInsertSingleTableClause(Insert_single_table_clauseContext ctx) {
        InsertBody insertBody = new InsertBody(ctx);
        if (ctx.column_list() != null) {
            insertBody.setColumns(ctx.column_list().column_definition_ref().stream().map(c -> {
                StatementFactory<ColumnReference> factory = new OracleColumnRefFactory(c);
                return factory.generate();
            }).collect(Collectors.toList()));
        }
        return insertBody;
    }

}
