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

import java.util.stream.Collectors;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Select_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.With_clauseContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.select.Select;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;
import com.oceanbase.tools.sqlparser.statement.select.WithTable;

import lombok.NonNull;

/**
 * {@link MySQLSelectFactory}
 *
 * @author yh263208
 * @date 2022-12-08 12:07
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class MySQLSelectFactory extends OBParserBaseVisitor<Select> implements StatementFactory<Select> {

    private final Select_stmtContext selectStmtContext;

    public MySQLSelectFactory(@NonNull Select_stmtContext selectStmtContext) {
        this.selectStmtContext = selectStmtContext;
    }

    @Override
    public Select generate() {
        return visit(this.selectStmtContext);
    }

    @Override
    public Select visitSelect_stmt(Select_stmtContext ctx) {
        SelectBody selectBody;
        if (ctx.select_no_parens() != null) {
            selectBody = new MySQLSelectBodyFactory(ctx.select_no_parens()).generate();
        } else {
            selectBody = new MySQLSelectBodyFactory(ctx.select_with_parens()).generate();
        }
        if (ctx.with_clause() != null) {
            With_clauseContext w = ctx.with_clause();
            if (w.RECURSIVE() != null) {
                selectBody.setRecursive(true);
            }
            selectBody.getWith().addAll(w.with_list().common_table_expr().stream().map(c -> {
                StatementFactory<WithTable> factory = new MySQLWithTableFactory(c);
                return factory.generate();
            }).collect(Collectors.toList()));
        }
        return new Select(ctx, selectBody);
    }

}
