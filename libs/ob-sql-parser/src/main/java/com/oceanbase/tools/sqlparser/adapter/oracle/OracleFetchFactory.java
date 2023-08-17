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

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Fetch_nextContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Fetch_next_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Fetch_next_countContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Fetch_next_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Fetch_next_percentContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Fetch_next_percent_exprContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.select.oracle.Fetch;
import com.oceanbase.tools.sqlparser.statement.select.oracle.FetchAddition;
import com.oceanbase.tools.sqlparser.statement.select.oracle.FetchDirection;
import com.oceanbase.tools.sqlparser.statement.select.oracle.FetchType;

import lombok.NonNull;

/**
 * {@link OracleFetchFactory}
 *
 * @author yh263208
 * @date 2022-12-06 11:12
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class OracleFetchFactory extends OBParserBaseVisitor<Fetch> implements StatementFactory<Fetch> {

    private final Fetch_nextContext fetchNextContext;
    private final Fetch_next_clauseContext fetchNextClauseContext;

    public OracleFetchFactory(@NonNull Fetch_nextContext fetchNextContext) {
        this.fetchNextClauseContext = null;
        this.fetchNextContext = fetchNextContext;
    }

    public OracleFetchFactory(@NonNull Fetch_next_clauseContext fetchNextClauseContext) {
        this.fetchNextContext = null;
        this.fetchNextClauseContext = fetchNextClauseContext;
    }

    @Override
    public Fetch generate() {
        if (this.fetchNextClauseContext != null) {
            return visit(this.fetchNextClauseContext);
        }
        if (this.fetchNextContext == null) {
            throw new IllegalStateException("No fetch next context available");
        }
        return visitFetchNext(this.fetchNextContext, null);
    }

    @Override
    public Fetch visitFetch_next_clause(Fetch_next_clauseContext ctx) {
        if (ctx.OFFSET() == null) {
            return visitFetchNext(ctx.fetch_next(), ctx);
        }
        if (ctx.fetch_next() != null) {
            return visitFetchNext(ctx.fetch_next(), ctx);
        }
        OracleExpressionFactory factory = new OracleExpressionFactory(ctx.bit_expr());
        return new Fetch(ctx, null, null, null, null, factory.generate());
    }

    private Fetch visitFetchNext(Fetch_nextContext ctx, Fetch_next_clauseContext parent) {
        if (ctx.fetch_next_count() != null) {
            return visitFetchNextCount(ctx.fetch_next_count(), parent);
        }
        return visitFetchNextPercent(ctx.fetch_next_percent(), parent);
    }

    private Fetch visitFetchNextCount(Fetch_next_countContext ctx, Fetch_next_clauseContext parent) {
        FetchAddition only = ctx.ONLY() == null ? FetchAddition.WITH_TIES : FetchAddition.ONLY;
        Fetch_next_exprContext fetchNextExprContext = ctx.fetch_next_expr();
        FetchDirection direction = fetchNextExprContext.FIRST() == null ? FetchDirection.NEXT : FetchDirection.FIRST;
        Expression fetch = null;
        if (fetchNextExprContext.bit_expr() != null) {
            OracleExpressionFactory factory = new OracleExpressionFactory(fetchNextExprContext.bit_expr());
            fetch = factory.generate();
        }
        if (parent == null || parent.bit_expr() == null) {
            return new Fetch(ctx, fetch, direction, FetchType.COUNT, only, null);
        }
        OracleExpressionFactory factory = new OracleExpressionFactory(parent.bit_expr());
        return new Fetch(parent, fetch, direction, FetchType.COUNT, only, factory.generate());
    }

    private Fetch visitFetchNextPercent(Fetch_next_percentContext ctx, Fetch_next_clauseContext parent) {
        FetchAddition only = ctx.ONLY() == null ? FetchAddition.WITH_TIES : FetchAddition.ONLY;
        Fetch_next_percent_exprContext fetchNextPercentExpr = ctx.fetch_next_percent_expr();
        FetchDirection direction = fetchNextPercentExpr.FIRST() == null ? FetchDirection.NEXT : FetchDirection.FIRST;
        Expression fetch = null;
        if (fetchNextPercentExpr.bit_expr() != null) {
            OracleExpressionFactory factory = new OracleExpressionFactory(fetchNextPercentExpr.bit_expr());
            fetch = factory.generate();
        }
        if (parent == null || parent.bit_expr() == null) {
            return new Fetch(ctx, fetch, direction, FetchType.PERCENT, only, null);
        }
        OracleExpressionFactory factory = new OracleExpressionFactory(parent.bit_expr());
        return new Fetch(parent, fetch, direction, FetchType.PERCENT, only, factory.generate());
    }

}
