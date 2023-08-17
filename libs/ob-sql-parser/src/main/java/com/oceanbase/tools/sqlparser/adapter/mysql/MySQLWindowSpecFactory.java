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

import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Generalized_window_clauseContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.New_generalized_window_clauseContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.New_generalized_window_clause_with_blanketContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Win_intervalContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Win_windowContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.common.WindowBody;
import com.oceanbase.tools.sqlparser.statement.common.WindowOffset;
import com.oceanbase.tools.sqlparser.statement.common.WindowOffsetType;
import com.oceanbase.tools.sqlparser.statement.common.WindowSpec;
import com.oceanbase.tools.sqlparser.statement.common.WindowType;
import com.oceanbase.tools.sqlparser.statement.expression.IntervalExpression;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;

import lombok.NonNull;

/**
 * {@link MySQLWindowSpecFactory}
 *
 * @author yh263208
 * @date 2022-12-11 17:27
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class MySQLWindowSpecFactory extends OBParserBaseVisitor<WindowSpec> implements StatementFactory<WindowSpec> {

    private final New_generalized_window_clauseContext windowClauseContext;
    private final New_generalized_window_clause_with_blanketContext windowClauseWithBlanketContext;

    public MySQLWindowSpecFactory(@NonNull New_generalized_window_clauseContext windowClauseContext) {
        this.windowClauseWithBlanketContext = null;
        this.windowClauseContext = windowClauseContext;
    }

    public MySQLWindowSpecFactory(
            @NonNull New_generalized_window_clause_with_blanketContext windowClauseWithBlanketContext) {
        this.windowClauseContext = null;
        this.windowClauseWithBlanketContext = windowClauseWithBlanketContext;
    }

    @Override
    public WindowSpec generate() {
        if (this.windowClauseContext != null) {
            return visit(this.windowClauseContext);
        }
        return visit(this.windowClauseWithBlanketContext);
    }

    @Override
    public WindowSpec visitNew_generalized_window_clause(New_generalized_window_clauseContext ctx) {
        if (ctx.new_generalized_window_clause_with_blanket() != null) {
            return visit(ctx.new_generalized_window_clause_with_blanket());
        }
        WindowSpec window = new WindowSpec(ctx);
        window.setName(ctx.NAME_OB().getText());
        return window;
    }

    @Override
    public WindowSpec visitNew_generalized_window_clause_with_blanket(
            New_generalized_window_clause_with_blanketContext ctx) {
        WindowSpec window = new WindowSpec(ctx, visit(ctx.generalized_window_clause()));
        if (ctx.NAME_OB() != null) {
            window.setName(ctx.NAME_OB().getText());
        }
        return window;
    }

    @Override
    public WindowSpec visitGeneralized_window_clause(Generalized_window_clauseContext ctx) {
        WindowSpec window = new WindowSpec(ctx);
        if (ctx.PARTITION() != null) {
            window.setPartitionBy(ctx.expr_list().expr().stream().map(e -> {
                StatementFactory<Expression> factory = new MySQLExpressionFactory(e);
                return factory.generate();
            }).collect(Collectors.toList()));
        }
        if (ctx.order_by() != null) {
            StatementFactory<OrderBy> factory = new MySQLOrderByFactory(ctx.order_by());
            window.setOrderBy(factory.generate());
        }
        if (ctx.win_window() != null) {
            Win_windowContext win = ctx.win_window();
            WindowType windowType = WindowType.RANGE;
            if (win.win_rows_or_range().ROWS() != null) {
                windowType = WindowType.ROWS;
            }
            List<WindowOffset> offsets = win.win_bounding().stream().map(c -> {
                if (c.CURRENT() != null) {
                    return new WindowOffset(c, WindowOffsetType.CURRENT_ROW);
                }
                Win_intervalContext w = c.win_interval();
                StatementFactory<Expression> factory = new MySQLExpressionFactory(w.expr());
                WindowOffsetType type = WindowOffsetType.FOLLOWING;
                if (c.win_preceding_or_following().PRECEDING() != null) {
                    type = WindowOffsetType.PRECEDING;
                }
                WindowOffset offset = new WindowOffset(c, type);
                Expression target = factory.generate();
                if (w.INTERVAL() == null) {
                    offset.setInterval(target);
                    return offset;
                }
                offset.setInterval(new IntervalExpression(w, target, w.date_unit().getText()));
                return offset;
            }).collect(Collectors.toList());
            WindowBody body;
            if (offsets.size() == 1) {
                body = new WindowBody(ctx.win_window(), windowType, offsets.get(0));
            } else if (offsets.size() == 2) {
                body = new WindowBody(ctx.win_window(), windowType, offsets.get(0), offsets.get(1));
            } else {
                throw new IllegalStateException("Window offset's count is illegal");
            }
            window.setBody(body);
        }
        return window;
    }

}
