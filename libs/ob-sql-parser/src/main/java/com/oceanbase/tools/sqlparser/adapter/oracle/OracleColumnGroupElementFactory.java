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
import java.util.stream.Collectors;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Column_group_elementContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Column_nameContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.common.ColumnGroupElement;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/4/28
 */
public class OracleColumnGroupElementFactory extends OBParserBaseVisitor<ColumnGroupElement>
        implements StatementFactory<ColumnGroupElement> {

    private final Column_group_elementContext columnGroupElementContext;

    public OracleColumnGroupElementFactory(Column_group_elementContext columnGroupElementContext) {
        this.columnGroupElementContext = columnGroupElementContext;
    }

    @Override
    public ColumnGroupElement generate() {
        return visit(columnGroupElementContext);
    }

    @Override
    public ColumnGroupElement visitColumn_group_element(Column_group_elementContext ctx) {
        if (ctx.ALL() != null) {
            return new ColumnGroupElement(ctx, true, false);
        } else if (ctx.EACH() != null) {
            return new ColumnGroupElement(ctx, false, true);
        }
        if (ctx.relation_name() == null || ctx.column_name_list() == null) {
            throw new IllegalStateException("Missing column group");
        }
        String relationName = ctx.relation_name().getText();
        List<Column_nameContext> columnNameContexts = ctx.column_name_list().column_name();
        return new ColumnGroupElement(ctx, relationName,
                columnNameContexts.stream().map(Column_nameContext::getText).collect(Collectors.toList()));
    }

}
