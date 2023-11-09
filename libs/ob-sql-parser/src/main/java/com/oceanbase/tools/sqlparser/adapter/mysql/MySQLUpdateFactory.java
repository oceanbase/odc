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
import java.util.Arrays;
import java.util.List;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Expr_or_defaultContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Update_asgn_factorContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Update_asgn_listContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Update_basic_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Update_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;
import com.oceanbase.tools.sqlparser.statement.select.mysql.Limit;
import com.oceanbase.tools.sqlparser.statement.update.Update;
import com.oceanbase.tools.sqlparser.statement.update.UpdateAssign;

import lombok.NonNull;

/**
 * {@link MySQLUpdateFactory}
 *
 * @author yh263208
 * @date 2022-12-20 16:07
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class MySQLUpdateFactory extends OBParserBaseVisitor<Update> implements StatementFactory<Update> {

    private final Update_stmtContext updateStmtContext;

    public MySQLUpdateFactory(@NonNull Update_stmtContext updateStmtContext) {
        this.updateStmtContext = updateStmtContext;
    }

    @Override
    public Update generate() {
        return visit(this.updateStmtContext);
    }

    @Override
    public Update visitUpdate_stmt(Update_stmtContext updateCtx) {
        Update_basic_stmtContext ctx = updateCtx.update_basic_stmt();
        Update update = new Update(ctx, MySQLSelectBodyFactory.visitFromList(ctx.table_references()),
                visitUpdateAsgnList(ctx.update_asgn_list()));
        if (ctx.expr() != null) {
            StatementFactory<Expression> factory = new MySQLExpressionFactory(ctx.expr());
            update.setCursor(false);
            update.setWhere(factory.generate());
        }
        if (ctx.order_by() != null) {
            StatementFactory<OrderBy> factory = new MySQLOrderByFactory(ctx.order_by());
            update.setOrderBy(factory.generate());
        }
        if (ctx.limit_clause() != null) {
            StatementFactory<Limit> factory = new MySQLLimitFactory(ctx.limit_clause());
            update.setLimit(factory.generate());
        }
        return update;
    }

    private List<UpdateAssign> visitUpdateAsgnList(Update_asgn_listContext ctx) {
        List<UpdateAssign> returnVal = new ArrayList<>();
        for (Update_asgn_factorContext updateAsgnFactor : ctx.update_asgn_factor()) {
            MySQLColumnRefFactory columnRefFactory =
                    new MySQLColumnRefFactory(updateAsgnFactor.column_definition_ref());
            List<ColumnReference> columnList = Arrays.asList(columnRefFactory.generate());

            Expr_or_defaultContext exprOrDefault = updateAsgnFactor.expr_or_default();
            Expression expression = null;
            if (exprOrDefault.expr() != null) {
                MySQLExpressionFactory factory = new MySQLExpressionFactory(exprOrDefault.expr());
                expression = factory.generate();
            }
            UpdateAssign updateAssign =
                    new UpdateAssign(updateAsgnFactor, columnList, expression, exprOrDefault.DEFAULT() != null);
            returnVal.add(updateAssign);
        }
        return returnVal;
    }
}
