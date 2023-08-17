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
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Select_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.select.ForUpdate;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;
import com.oceanbase.tools.sqlparser.statement.select.Select;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;
import com.oceanbase.tools.sqlparser.statement.select.oracle.Fetch;

import lombok.NonNull;

/**
 * {@link OracleSelectFactory}
 *
 * @author yh263208
 * @date 2022-12-07 21:32
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class OracleSelectFactory extends OBParserBaseVisitor<Select> implements StatementFactory<Select> {

    private final Select_stmtContext selectStmtContext;

    public OracleSelectFactory(@NonNull Select_stmtContext selectStmtContext) {
        this.selectStmtContext = selectStmtContext;
    }

    @Override
    public Select generate() {
        return visit(this.selectStmtContext);
    }

    @Override
    public Select visitSelect_stmt(Select_stmtContext ctx) {
        StatementFactory<SelectBody> selectBodyFactory = new OracleSelectBodyFactory(ctx.subquery());
        Select select = new Select(ctx, selectBodyFactory.generate());
        if (ctx.fetch_next_clause() != null) {
            StatementFactory<Fetch> factory = new OracleFetchFactory(ctx.fetch_next_clause());
            select.setFetch(factory.generate());
        }
        if (ctx.fetch_next() != null) {
            StatementFactory<Fetch> factory = new OracleFetchFactory(ctx.fetch_next());
            select.setFetch(factory.generate());
        }
        if (ctx.for_update() != null) {
            StatementFactory<ForUpdate> factory = new OracleForUpdateFactory(ctx.for_update());
            select.setForUpdate(factory.generate());
        }
        if (ctx.order_by() != null) {
            StatementFactory<OrderBy> factory = new OracleOrderByFactory(ctx.order_by());
            select.setOrderBy(factory.generate());
        }
        return select;
    }

}
