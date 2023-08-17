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

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Sort_column_keyContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.createtable.SortColumn;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.select.SortDirection;

import lombok.NonNull;

/**
 * {@link MySQLSortColumnFactory}
 *
 * @author yh263208
 * @date 2023-05-24 14:07
 * @since ODC_release_4.2.0
 */
public class MySQLSortColumnFactory extends OBParserBaseVisitor<SortColumn> implements StatementFactory<SortColumn> {

    private final Sort_column_keyContext sortColumnKeyContext;

    public MySQLSortColumnFactory(@NonNull Sort_column_keyContext sortColumnKeyContext) {
        this.sortColumnKeyContext = sortColumnKeyContext;
    }

    @Override
    public SortColumn generate() {
        return visit(this.sortColumnKeyContext);
    }

    @Override
    public SortColumn visitSort_column_key(Sort_column_keyContext ctx) {
        ColumnReference r = new ColumnReference(ctx.column_name(), null, null,
                ctx.column_name().getText());
        SortColumn sortColumn = new SortColumn(ctx, r);
        if (ctx.LeftParen() != null && ctx.RightParen() != null) {
            sortColumn.setLength(Integer.valueOf(ctx.INTNUM(0).getText()));
        }
        SortDirection direction = null;
        if (ctx.ASC() != null) {
            direction = SortDirection.ASC;
        } else if (ctx.DESC() != null) {
            direction = SortDirection.DESC;
        }
        sortColumn.setDirection(direction);
        if (ctx.ID() != null) {
            sortColumn.setId(Integer.valueOf(ctx.INTNUM(ctx.INTNUM().size() - 1).getText()));
        }
        return sortColumn;
    }

}
