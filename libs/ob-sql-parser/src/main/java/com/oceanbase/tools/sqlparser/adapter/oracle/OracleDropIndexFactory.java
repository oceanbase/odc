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

import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Drop_index_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Relation_nameContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.dropindex.DropIndex;

/**
 * @Author: Lebie
 * @Date: 2024/8/21 18:39
 * @Description: []
 */
public class OracleDropIndexFactory extends OBParserBaseVisitor<DropIndex> implements StatementFactory<DropIndex> {
    private final ParserRuleContext parserRuleContext;

    public OracleDropIndexFactory(Drop_index_stmtContext parserRuleContext) {
        this.parserRuleContext = parserRuleContext;
    }

    @Override
    public DropIndex generate() {
        return visit(this.parserRuleContext);
    }

    @Override
    public DropIndex visitDrop_index_stmt(Drop_index_stmtContext ctx) {
        List<Relation_nameContext> relations = ctx.relation_name();
        if (relations.size() == 1) {
            // DROP INDEX ANY_INDEX
            return new DropIndex(ctx, null, relations.get(0).getText());
        } else if (relations.size() == 2) {
            // DROP INDEX ANY_SCHEMA.ANY_INDEX
            return new DropIndex(ctx, relations.get(0).getText(), relations.get(1).getText());
        }
        throw new IllegalStateException("Unexpected drop index statement: " + ctx.getText());
    }
}
