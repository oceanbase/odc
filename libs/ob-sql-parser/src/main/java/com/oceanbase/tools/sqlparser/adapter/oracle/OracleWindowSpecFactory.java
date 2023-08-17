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
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Generalized_window_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Win_intervalContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Win_windowContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.common.WindowBody;
import com.oceanbase.tools.sqlparser.statement.common.WindowOffset;
import com.oceanbase.tools.sqlparser.statement.common.WindowOffsetType;
import com.oceanbase.tools.sqlparser.statement.common.WindowSpec;
import com.oceanbase.tools.sqlparser.statement.common.WindowType;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;

import lombok.NonNull;

/**
 * {@link OracleWindowSpecFactory}
 *
 * @author yh263208
 * @date 2022-12-11 19:34
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class OracleWindowSpecFactory extends OBParserBaseVisitor<WindowSpec> implements StatementFactory<WindowSpec> {

    private final Generalized_window_clauseContext windowClause;

    public OracleWindowSpecFactory(@NonNull Generalized_window_clauseContext windowClause) {
        this.windowClause = windowClause;
    }

    @Override
    public WindowSpec generate() {
        return visit(this.windowClause);
    }

    @Override
    public WindowSpec visitGeneralized_window_clause(Generalized_window_clauseContext ctx) {
        WindowSpec window = new WindowSpec(ctx);
        if (ctx.PARTITION() != null) {
            window.setPartitionBy(ctx.expr_list().bit_expr().stream().map(e -> {
                StatementFactory<Expression> factory = new OracleExpressionFactory(e);
                return factory.generate();
            }).collect(Collectors.toList()));
        }
        if (ctx.order_by() != null) {
            StatementFactory<OrderBy> factory = new OracleOrderByFactory(ctx.order_by());
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
                WindowOffsetType type = WindowOffsetType.FOLLOWING;
                if (c.win_preceding_or_following().PRECEDING() != null) {
                    type = WindowOffsetType.PRECEDING;
                }
                WindowOffset offset = new WindowOffset(c, type);
                StatementFactory<Expression> factory = new OracleExpressionFactory(w.bit_expr());
                offset.setInterval(factory.generate());
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
