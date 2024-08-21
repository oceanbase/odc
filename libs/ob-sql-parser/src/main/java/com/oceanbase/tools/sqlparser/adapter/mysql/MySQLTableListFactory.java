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
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Table_listContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.droptable.TableList;

/**
 * @Author: Lebie
 * @Date: 2024/8/21 20:47
 * @Description: []
 */
public class MySQLTableListFactory extends OBParserBaseVisitor<TableList> implements StatementFactory<TableList> {
    private final ParserRuleContext parserRuleContext;

    public MySQLTableListFactory(Table_listContext parserRuleContext) {
        this.parserRuleContext = parserRuleContext;
    }

    @Override
    public TableList generate() {
        return visit(this.parserRuleContext);
    }

    @Override
    public TableList visitTable_list(Table_listContext ctx) {
        return new TableList(ctx, MySQLFromReferenceFactory.getRelationFactors(ctx.relation_factor()));
    }
}
