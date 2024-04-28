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
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Column_group_elementContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Column_nameContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Column_name_listContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Relation_nameContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.common.ColumnGroup;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/4/28
 */
public class MySQLColumnGroupElementFactory extends OBParserBaseVisitor<ColumnGroup>
        implements StatementFactory<ColumnGroup> {
    private final Column_group_elementContext columnGroupElementContext;

    public MySQLColumnGroupElementFactory(Column_group_elementContext columnGroupElementContext) {
        this.columnGroupElementContext = columnGroupElementContext;
    }

    @Override
    public ColumnGroup generate() {
        return visit(columnGroupElementContext);
    }

    @Override
    public ColumnGroup visitColumn_group_element(Column_group_elementContext ctx) {
        if (ctx.ALL() != null) {
            return new ColumnGroup(ctx, true, false);
        } else if (ctx.EACH() != null) {
            return new ColumnGroup(ctx, false, true);
        }
        if (ctx.relation_name() == null || ctx.column_name_list() == null) {
            throw new IllegalStateException("Missing column group");
        }
        Relation_nameContext relationNameContext = ctx.relation_name();
        String relationName = relationNameContext.getText();
        Column_name_listContext columnNameListContext = ctx.column_name_list();
        List<Column_nameContext> columnNameContexts = columnNameListContext.column_name();
        return new ColumnGroup(ctx, relationName,
                columnNameContexts.stream().map(Column_nameContext::getText).collect(Collectors.toList()));
    }

}
