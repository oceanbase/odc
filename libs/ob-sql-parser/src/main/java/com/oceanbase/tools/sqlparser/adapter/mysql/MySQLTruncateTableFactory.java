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

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Truncate_table_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.truncate.TruncateTable;

import lombok.NonNull;

/**
 * {@link MySQLTruncateTableFactory}
 *
 * @author yh263208
 * @date 2024-03-05 22:26
 * @since ODC_release_4.2.4
 */
public class MySQLTruncateTableFactory extends OBParserBaseVisitor<TruncateTable>
        implements StatementFactory<TruncateTable> {

    private final ParserRuleContext context;

    public MySQLTruncateTableFactory(@NonNull Truncate_table_stmtContext truncateTableContext) {
        this.context = truncateTableContext;
    }

    @Override
    public TruncateTable generate() {
        return visit(this.context);
    }

    @Override
    public TruncateTable visitTruncate_table_stmt(Truncate_table_stmtContext ctx) {
        return new TruncateTable(ctx, MySQLFromReferenceFactory.getRelationFactor(ctx.relation_factor()));
    }

}
