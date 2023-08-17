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

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_asc_descContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Sort_column_keyContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.createtable.SortColumn;

import lombok.NonNull;

/**
 * {@link OracleSortColumnFactory}
 *
 * @author yh263208
 * @date 2023-05-24 14:07
 * @since ODC_release_4.2.0
 */
public class OracleSortColumnFactory extends OBParserBaseVisitor<SortColumn> implements StatementFactory<SortColumn> {

    private final Sort_column_keyContext sortColumnKeyContext;

    public OracleSortColumnFactory(@NonNull Sort_column_keyContext sortColumnKeyContext) {
        this.sortColumnKeyContext = sortColumnKeyContext;
    }

    @Override
    public SortColumn generate() {
        return visit(this.sortColumnKeyContext);
    }

    @Override
    public SortColumn visitSort_column_key(Sort_column_keyContext ctx) {
        SortColumn column = new SortColumn(ctx, new OracleExpressionFactory(ctx.index_expr().bit_expr()).generate());
        Opt_asc_descContext optAscDescContext = ctx.opt_asc_desc();
        column.setDirection(OracleSortKeyFactory.getSortDirection(optAscDescContext));
        column.setNullPosition(OracleSortKeyFactory.getSortNullPosition(optAscDescContext));
        if (ctx.INTNUM() != null) {
            column.setId(Integer.valueOf(ctx.INTNUM().getText()));
        }
        return column;
    }

}
