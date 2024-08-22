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
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_table_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.droptable.DropTable;

/**
 * @Author: Lebie
 * @Date: 2024/8/21 19:11
 * @Description: []
 */
public class OracleDropTableFactory extends OBParserBaseVisitor<DropTable> implements StatementFactory<DropTable> {
    private final ParserRuleContext parserRuleContext;

    public OracleDropTableFactory(Drop_table_stmtContext parserRuleContext) {
        this.parserRuleContext = parserRuleContext;
    }

    @Override
    public DropTable generate() {
        return visit(this.parserRuleContext);
    }


    @Override
    public DropTable visitDrop_table_stmt(Drop_table_stmtContext ctx) {
        return new DropTable(ctx, OracleFromReferenceFactory.getRelationFactor(ctx.relation_factor()),
                ctx.CASCADE() != null && ctx.CONSTRAINTS() != null,
                ctx.PURGE() != null);
    }
}
