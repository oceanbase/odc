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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Into_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Returning_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.common.oracle.Returning;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.select.Projection;

import lombok.NonNull;

/**
 * {@link OracleReturningFactory}
 *
 * @author yh263208
 * @date 2023-11-08 19:58
 * @since ODC_release_4.2.3
 */
public class OracleReturningFactory extends OBParserBaseVisitor<Returning> implements StatementFactory<Returning> {

    private final ParserRuleContext context;

    public OracleReturningFactory(@NonNull Returning_clauseContext returningClauseContext) {
        this.context = returningClauseContext;
    }

    @Override
    public Returning generate() {
        return visit(this.context);
    }

    @Override
    public Returning visitReturning_clause(Returning_clauseContext ctx) {
        List<Projection> projectionList = ctx.returning_exprs().projection().stream()
                .map(c -> new OracleProjectionFactory(c).generate()).collect(Collectors.toList());
        List<Expression> intoList = new ArrayList<>();
        Into_clauseContext intoCtx = ctx.opt_into_clause().into_clause();
        boolean bulkCollect = false;
        if (intoCtx != null) {
            intoList = intoCtx.into_var_list().into_var().stream().map(c -> {
                if (c.USER_VARIABLE() != null) {
                    return new ConstExpression(c.USER_VARIABLE());
                } else if (c.obj_access_ref_normal() != null) {
                    return new OracleExpressionFactory().visit(c.obj_access_ref_normal());
                }
                if (c.QUESTIONMARK() != null) {
                    return new ConstExpression(c.QUESTIONMARK());
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());
            if (intoCtx.BULK() != null && intoCtx.COLLECT() != null) {
                bulkCollect = true;
            }
        }
        Returning returning = new Returning(ctx, intoList, projectionList);
        returning.setBulkCollect(bulkCollect);
        return returning;
    }

}
