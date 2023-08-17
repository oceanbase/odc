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
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Alias_name_listContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Common_table_exprContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;
import com.oceanbase.tools.sqlparser.statement.select.WithTable;

import lombok.NonNull;

/**
 * {@link MySQLWithTableFactory}
 *
 * @author yh263208
 * @date 2022-12-12 17:31
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class MySQLWithTableFactory extends OBParserBaseVisitor<WithTable> implements StatementFactory<WithTable> {

    private final Common_table_exprContext commonTableExprContext;

    public MySQLWithTableFactory(@NonNull Common_table_exprContext commonTableExprContext) {
        this.commonTableExprContext = commonTableExprContext;
    }

    @Override
    public WithTable generate() {
        return visit(this.commonTableExprContext);
    }

    @Override
    public WithTable visitCommon_table_expr(Common_table_exprContext ctx) {
        String relationName = ctx.relation_name().getText();
        SelectBody select = null;
        if (ctx.select_no_parens() != null) {
            StatementFactory<SelectBody> factory = new MySQLSelectBodyFactory(ctx.select_no_parens());
            select = factory.generate();
        } else if (ctx.select_with_parens() != null) {
            StatementFactory<SelectBody> factory = new MySQLSelectBodyFactory(ctx.select_with_parens());
            select = factory.generate();
        }
        if (select == null) {
            throw new IllegalStateException("Missing select statement");
        }
        if (ctx.with_clause() != null) {
            if (ctx.with_clause().RECURSIVE() != null) {
                select.setRecursive(true);
            }
            for (Common_table_exprContext c : ctx.with_clause().with_list().common_table_expr()) {
                select.getWith().add(visit(c));
            }
        }
        WithTable withTable = new WithTable(ctx, relationName, select);
        if (ctx.alias_name_list() != null) {
            withTable.setAliasList(visitAliasNames(ctx.alias_name_list()));
        }
        return withTable;
    }

    private List<String> visitAliasNames(Alias_name_listContext ctx) {
        return ctx.column_alias_name().stream().map(c -> c.column_name().getText()).collect(Collectors.toList());
    }

}
