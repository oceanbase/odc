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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.tree.TerminalNode;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Condition_insert_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Conditional_insert_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Dml_table_nameContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Insert_single_table_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Insert_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Insert_table_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Insert_table_clause_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Insert_vals_listContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Multi_table_insertContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Order_by_fetch_with_check_optionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Returning_log_error_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Single_table_insertContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Values_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.insert.ConditionalInsert;
import com.oceanbase.tools.sqlparser.statement.insert.Insert;
import com.oceanbase.tools.sqlparser.statement.insert.InsertCondition;
import com.oceanbase.tools.sqlparser.statement.insert.InsertTable;
import com.oceanbase.tools.sqlparser.statement.select.Select;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;

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
            return new Insert(ctx, visit(ctx.single_table_insert()));
        }
        return new Insert(ctx, visit(ctx.multi_table_insert()));
    }

    @Override
    public Insert visitMulti_table_insert(Multi_table_insertContext ctx) {
        Insert insert;
        if (ctx.insert_table_clause_list() != null) {
            insert = new Insert(ctx, visit(ctx.insert_table_clause_list()));
            insert.setAll(true);
        } else {
            insert = new Insert(ctx, visit(ctx.conditional_insert_clause()));
        }
        Select select = new Select(ctx.subquery(), new OracleSelectBodyFactory(ctx.subquery()).generate());
        if (ctx.order_by() != null) {
            select.setOrderBy(new OracleOrderByFactory(ctx.order_by()).generate());
        }
        if (ctx.fetch_next_clause() != null) {
            select.setFetch(new OracleFetchFactory(ctx.fetch_next_clause()).generate());
        }
        insert.setSelect(select);
        return insert;
    }

    @Override
    public Insert visitInsert_table_clause_list(Insert_table_clause_listContext ctx) {
        List<InsertTable> insertTables = ctx.insert_single_table_clause().stream()
                .map(this::getInsertTable).collect(Collectors.toList());
        return new Insert(ctx, insertTables, null);
    }

    @Override
    public Insert visitConditional_insert_clause(Conditional_insert_clauseContext ctx) {
        ConditionalInsert conditionalInsert;
        List<InsertCondition> insertConditions = ctx.condition_insert_clause_list()
                .condition_insert_clause()
                .stream().map(this::getInsertCondition).collect(Collectors.toList());
        if (ctx.insert_table_clause_list() != null) {
            conditionalInsert = new ConditionalInsert(ctx.condition_insert_clause_list(),
                    ctx.insert_table_clause_list(), insertConditions);
            conditionalInsert.setElseClause(ctx.insert_table_clause_list()
                    .insert_single_table_clause().stream()
                    .map(this::getInsertTable).collect(Collectors.toList()));
        } else {
            conditionalInsert = new ConditionalInsert(ctx.condition_insert_clause_list(), insertConditions);
        }
        Insert insert = new Insert(ctx, Collections.emptyList(), conditionalInsert);
        if (ctx.ALL() != null) {
            insert.setAll(true);
        } else if (ctx.FIRST() != null) {
            insert.setFirst(true);
        }
        return insert;
    }

    @Override
    public Insert visitSingle_table_insert(Single_table_insertContext ctx) {
        InsertTable insertTable;
        Insert_table_clauseContext iCtx = ctx.insert_table_clause();
        TerminalNode beginNode = ctx.INTO() == null ? ctx.OVERWRITE() : ctx.INTO();
        if (iCtx.dml_table_name() != null) {
            Dml_table_nameContext dCtx = iCtx.dml_table_name();
            insertTable = new InsertTable(beginNode, ctx.values_clause(), OracleFromReferenceFactory
                    .getRelationFactor(dCtx.relation_factor()));
            if (dCtx.use_partition() != null) {
                insertTable.setPartitionUsage(new OraclePartitionUsageFactory(dCtx.use_partition()).generate());
            }
        } else if (iCtx.select_with_parens() != null) {
            insertTable = new InsertTable(beginNode, ctx.values_clause(),
                    new OracleSelectBodyFactory(iCtx.select_with_parens()).generate());
        } else {
            OracleSelectBodyFactory factory = new OracleSelectBodyFactory(iCtx.subquery());
            SelectBody select = factory.generate();
            Order_by_fetch_with_check_optionContext oCtx = iCtx.order_by_fetch_with_check_option();
            if (oCtx.order_by() != null) {
                select.getLastSelectBody().setOrderBy(new OracleOrderByFactory(oCtx.order_by()).generate());
            }
            if (oCtx.fetch_next_clause() != null) {
                select.getLastSelectBody().setFetch(new OracleFetchFactory(oCtx.fetch_next_clause()).generate());
            }
            if (oCtx.with_check_option() != null) {
                select.getLastSelectBody().setWithCheckOption(true);
            }
            insertTable = new InsertTable(beginNode, ctx.values_clause(), select);
        }
        if (iCtx.relation_name() != null) {
            insertTable.setAlias(iCtx.relation_name().getText());
        }
        if (ctx.NOLOGGING() != null) {
            insertTable.setNologging(true);
        }
        if (ctx.column_list() != null) {
            insertTable.setColumns(ctx.column_list().column_definition_ref().stream()
                    .map(c -> new OracleColumnRefFactory(c).generate()).collect(Collectors.toList()));
        }
        Values_clauseContext vCtx = ctx.values_clause();
        List<List<Expression>> values = new ArrayList<>();
        if (vCtx.insert_vals_list() != null) {
            fullFillValues(vCtx.insert_vals_list(), values);
        } else if (vCtx.obj_access_ref_normal() != null) {
            values.add(Collections.singletonList(new OracleExpressionFactory().visit(vCtx.obj_access_ref_normal())));
        } else {
            Select select = new Select(vCtx.subquery(), new OracleSelectBodyFactory(vCtx.subquery()).generate());
            if (vCtx.order_by() != null) {
                select.setOrderBy(new OracleOrderByFactory(vCtx.order_by()).generate());
            }
            if (vCtx.fetch_next_clause() != null) {
                select.setFetch(new OracleFetchFactory(vCtx.fetch_next_clause()).generate());
            }
            values.add(Collections.singletonList(select));
        }
        insertTable.setValues(values);
        Insert insert = new Insert(ctx, Collections.singletonList(insertTable), null);
        if (ctx.returning_log_error_clause() != null) {
            Returning_log_error_clauseContext rCtx = ctx.returning_log_error_clause();
            if (rCtx.returning_clause() != null) {
                insert.setReturning(new OracleReturningFactory(rCtx.returning_clause()).generate());
            }
            if (rCtx.log_error_clause() != null) {
                insert.setLogErrors(new OracleLogErrorsFactory(rCtx.log_error_clause()).generate());
            }
        }
        if (ctx.OVERWRITE() != null) {
            insert.setOverwrite(true);
        }
        return insert;
    }

    private void fullFillValues(Insert_vals_listContext ctx, List<List<Expression>> values) {
        if (ctx.insert_vals_list() != null) {
            fullFillValues(ctx.insert_vals_list(), values);
        }
        values.add(ctx.insert_vals().expr_or_default().stream().map(c -> {
            if (c.DEFAULT() != null) {
                return new ConstExpression(c.DEFAULT());
            }
            return new OracleExpressionFactory(c.bit_expr()).generate();
        }).collect(Collectors.toList()));
    }

    private InsertTable getInsertTable(Insert_single_table_clauseContext ctx) {
        Dml_table_nameContext dCtx = ctx.dml_table_name();
        InsertTable insertTable = new InsertTable(ctx, OracleFromReferenceFactory
                .getRelationFactor(dCtx.relation_factor()));
        if (dCtx.use_partition() != null) {
            insertTable.setPartitionUsage(new OraclePartitionUsageFactory(dCtx.use_partition()).generate());
        }
        if (ctx.column_list() != null) {
            insertTable.setColumns(ctx.column_list().column_definition_ref().stream()
                    .map(c -> new OracleColumnRefFactory(c).generate()).collect(Collectors.toList()));
        }
        if (ctx.insert_vals() != null) {
            List<List<Expression>> values = new ArrayList<>();
            values.add(ctx.insert_vals().expr_or_default().stream().map(c -> {
                if (c.DEFAULT() != null) {
                    return new ConstExpression(c.DEFAULT());
                }
                return new OracleExpressionFactory(c.bit_expr()).generate();
            }).collect(Collectors.toList()));
            insertTable.setValues(values);
        }
        return insertTable;
    }

    private InsertCondition getInsertCondition(Condition_insert_clauseContext ctx) {
        List<InsertTable> insertTables = ctx.insert_table_clause_list()
                .insert_single_table_clause()
                .stream().map(this::getInsertTable).collect(Collectors.toList());
        return new InsertCondition(ctx, new OracleExpressionFactory(ctx.expr()).generate(), insertTables);
    }

}
