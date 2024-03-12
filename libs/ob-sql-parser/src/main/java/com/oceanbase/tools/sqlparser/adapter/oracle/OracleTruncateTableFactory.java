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

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Truncate_table_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.truncate.TruncateTable;

import lombok.NonNull;

/**
 * {@link OracleTruncateTableFactory}
 *
 * @author yh263208
 * @date 2024-03-05 22:31
 * @since ODC_release_4.2.4
 */
public class OracleTruncateTableFactory extends OBParserBaseVisitor<TruncateTable>
        implements StatementFactory<TruncateTable> {

    private final ParserRuleContext context;

    public OracleTruncateTableFactory(@NonNull Truncate_table_stmtContext truncateTableStmtContext) {
        this.context = truncateTableStmtContext;
    }

    @Override
    public TruncateTable generate() {
        return visit(this.context);
    }

    @Override
    public TruncateTable visitTruncate_table_stmt(Truncate_table_stmtContext ctx) {
        return new TruncateTable(ctx, OracleFromReferenceFactory.getRelationFactor(ctx.relation_factor()));
    }

}
