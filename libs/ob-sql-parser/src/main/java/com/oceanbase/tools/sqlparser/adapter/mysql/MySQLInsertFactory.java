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
package com.oceanbase.tools.sqlparser.adapter.mysql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Insert_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Insert_valsContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Insert_vals_listContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Single_table_insertContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Table_subquery_aliasContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Update_asgn_listContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Values_clauseContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.insert.Insert;
import com.oceanbase.tools.sqlparser.statement.insert.InsertTable;
import com.oceanbase.tools.sqlparser.statement.insert.mysql.SetColumn;

import lombok.NonNull;

/**
 * {@link MySQLInsertFactory}
 *
 * @author yh263208
 * @date 2022-12-20 10:15
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class MySQLInsertFactory extends OBParserBaseVisitor<Insert> implements StatementFactory<Insert> {

    private final Insert_stmtContext insertStmtContext;

    public MySQLInsertFactory(@NonNull Insert_stmtContext insertStmtContext) {
        this.insertStmtContext = insertStmtContext;
    }

    @Override
    public Insert generate() {
        return visit(this.insertStmtContext);
    }

    @Override
    public Insert visitInsert_stmt(Insert_stmtContext ctx) {
        Insert insert = new Insert(ctx, visit(ctx.single_table_insert()));
        if (ctx.replace_with_opt_hint() != null) {
            insert.setReplace(true);
        }
        if (ctx.IGNORE() != null) {
            insert.setIgnore(true);
        }
        if (ctx.HIGH_PRIORITY() != null) {
            insert.setHighPriority(true);
        } else if (ctx.LOW_PRIORITY() != null) {
            insert.setLowPriority(true);
        }
        if (ctx.OVERWRITE() != null) {
            insert.setOverwrite(true);
        }
        if (ctx.update_asgn_list() != null) {
            insert.setOnDuplicateKeyUpdateColumns(getSetColumns(ctx.update_asgn_list()));
        }
        return insert;
    }

    @Override
    public Insert visitSingle_table_insert(Single_table_insertContext ctx) {
        InsertTable insertTable = new InsertTable(ctx, MySQLFromReferenceFactory
                .getRelationFactor(ctx.dml_table_name().relation_factor()));
        if (ctx.dml_table_name().use_partition() != null) {
            insertTable.setPartitionUsage(MySQLFromReferenceFactory
                    .visitPartitionUsage(ctx.dml_table_name().use_partition()));
        }
        if (ctx.column_list() != null) {
            insertTable.setColumns(ctx.column_list().column_definition_ref().stream()
                    .map(c -> new MySQLColumnRefFactory(c).generate()).collect(Collectors.toList()));
        }
        if (ctx.values_clause() != null) {
            List<List<Expression>> values = new ArrayList<>();
            Values_clauseContext vCtx = ctx.values_clause();
            if (vCtx.insert_vals_list() != null) {
                fullFillValues(vCtx.insert_vals_list(), values);
            } else {
                values.add(Collections.singletonList(new MySQLSelectFactory(vCtx.select_stmt()).generate()));
            }
            insertTable.setValues(values);
            insertTable.setAlias(getAlias(vCtx.table_subquery_alias()));
            insertTable.setAliasColumns(getAliasColumns(vCtx.table_subquery_alias()));
        }
        if (ctx.update_asgn_list() != null) {
            insertTable.setSetColumns(getSetColumns(ctx.update_asgn_list()));
            insertTable.setAlias(getAlias(ctx.table_subquery_alias()));
            insertTable.setAliasColumns(getAliasColumns(ctx.table_subquery_alias()));
        }
        return new Insert(ctx, Collections.singletonList(insertTable), null);
    }

    private List<String> getAliasColumns(Table_subquery_aliasContext context) {
        if (context == null || context.alias_name_list() == null) {
            return null;
        }
        return context.alias_name_list().column_alias_name().stream()
                .map(c -> c.column_name().getText()).collect(Collectors.toList());
    }

    private String getAlias(Table_subquery_aliasContext context) {
        if (context == null) {
            return null;
        }
        return context.relation_name().getText();
    }

    private List<SetColumn> getSetColumns(Update_asgn_listContext ctx) {
        return ctx.update_asgn_factor().stream().map(c -> {
            Expression val;
            if (c.expr_or_default().DEFAULT() != null) {
                val = new ConstExpression(c.expr_or_default().DEFAULT());
            } else {
                val = new MySQLExpressionFactory(c.expr_or_default().expr()).generate();
            }
            return new SetColumn(c, new MySQLColumnRefFactory(c.column_definition_ref()).generate(), val);
        }).collect(Collectors.toList());
    }

    private void fullFillValues(Insert_vals_listContext ctx, List<List<Expression>> values) {
        if (ctx.insert_vals_list() != null) {
            fullFillValues(ctx.insert_vals_list(), values);
        }
        List<Expression> vals = new ArrayList<>();
        fullFillValues(ctx.insert_vals(), vals);
        values.add(vals);
    }

    private void fullFillValues(Insert_valsContext ctx, List<Expression> values) {
        if (ctx.insert_vals() != null) {
            fullFillValues(ctx.insert_vals(), values);
        }
        if (ctx.empty() != null) {
            return;
        }
        if (ctx.expr_or_default().DEFAULT() != null) {
            values.add(new ConstExpression(ctx.expr_or_default().DEFAULT()));
        } else {
            values.add(new MySQLExpressionFactory(ctx.expr_or_default().expr()).generate());
        }
    }

}
