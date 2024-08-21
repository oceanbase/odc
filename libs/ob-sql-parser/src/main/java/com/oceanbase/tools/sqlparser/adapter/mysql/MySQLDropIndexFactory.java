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
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Drop_index_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.dropindex.DropIndex;

/**
 * @Author: Lebie
 * @Date: 2024/8/21 18:28
 * @Description: []
 */
public class MySQLDropIndexFactory extends OBParserBaseVisitor<DropIndex> implements StatementFactory<DropIndex> {
    private final ParserRuleContext parserRuleContext;

    public MySQLDropIndexFactory(ParserRuleContext parserRuleContext) {
        this.parserRuleContext = parserRuleContext;
    }

    @Override
    public DropIndex generate() {
        return visit(this.parserRuleContext);
    }

    @Override
    public DropIndex visitDrop_index_stmt(Drop_index_stmtContext ctx) {
        RelationFactor relation = MySQLFromReferenceFactory.getRelationFactor(ctx.relation_factor());
        String indexName = ctx.relation_name().getText();
        return new DropIndex(ctx, indexName, relation);
    }
}
