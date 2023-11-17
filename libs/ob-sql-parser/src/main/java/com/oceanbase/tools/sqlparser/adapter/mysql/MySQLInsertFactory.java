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
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Column_listContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Insert_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.insert.Insert;
import com.oceanbase.tools.sqlparser.statement.insert.InsertBody;
import com.oceanbase.tools.sqlparser.statement.insert.SingleTableInsert;

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
        Column_listContext columns = ctx.single_table_insert().column_list();
        InsertBody insertBody = new InsertBody(ctx.single_table_insert());
        if (columns != null) {
            insertBody.setColumns(columns.column_definition_ref().stream().map(c -> {
                StatementFactory<ColumnReference> factory = new MySQLColumnRefFactory(c);
                return factory.generate();
            }).collect(Collectors.toList()));
        }
        SingleTableInsert insert = new SingleTableInsert(ctx, insertBody);
        if (ctx.replace_with_opt_hint() != null) {
            insert.setReplace(true);
        }
        return insert;
    }

}
