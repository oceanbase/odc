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

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Limit_clauseContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.select.mysql.Limit;

import lombok.NonNull;

/**
 * {@link MySQLLimitFactory}
 *
 * @author yh263208
 * @date 2022-12-12 16:15
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class MySQLLimitFactory extends OBParserBaseVisitor<Limit> implements StatementFactory<Limit> {

    private final Limit_clauseContext limitClauseContext;

    public MySQLLimitFactory(@NonNull Limit_clauseContext limitClauseContext) {
        this.limitClauseContext = limitClauseContext;
    }

    @Override
    public Limit generate() {
        return visit(this.limitClauseContext);
    }

    @Override
    public Limit visitLimit_clause(Limit_clauseContext ctx) {
        List<Expression> exprs = ctx.limit_expr().stream().map(c -> {
            if (c.INTNUM() != null) {
                return new ConstExpression(c.INTNUM());
            } else if (c.QUESTIONMARK() != null) {
                return new ConstExpression(c.QUESTIONMARK());
            }
            StatementFactory<ColumnReference> factory = new MySQLColumnRefFactory(c.column_ref());
            return factory.generate();
        }).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(exprs)) {
            throw new IllegalStateException("Missing limit row count");
        }
        Limit limit = new Limit(ctx, exprs.get(0));
        if (exprs.size() > 1) {
            limit.setOffset(exprs.get(1));
        }
        return limit;
    }

}
