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
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Sort_keyContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Sort_key_for_group_byContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.select.SortDirection;
import com.oceanbase.tools.sqlparser.statement.select.SortKey;

import lombok.NonNull;

/**
 * {@link MySQLSortKeyFactory}
 *
 * @author yh263208
 * @date 2022-12-09 16:07
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class MySQLSortKeyFactory extends OBParserBaseVisitor<SortKey> implements StatementFactory<SortKey> {

    private final Sort_keyContext sortKeyContext;
    private final Sort_key_for_group_byContext sortKeyForGroupByContext;

    public MySQLSortKeyFactory(@NonNull Sort_keyContext sortKeyContext) {
        this.sortKeyForGroupByContext = null;
        this.sortKeyContext = sortKeyContext;
    }

    public MySQLSortKeyFactory(@NonNull Sort_key_for_group_byContext sortKeyForGroupByContext) {
        this.sortKeyContext = null;
        this.sortKeyForGroupByContext = sortKeyForGroupByContext;
    }

    @Override
    public SortKey generate() {
        if (this.sortKeyContext != null) {
            return visit(this.sortKeyContext);
        }
        return visit(this.sortKeyForGroupByContext);
    }

    @Override
    public SortKey visitSort_key(Sort_keyContext ctx) {
        StatementFactory<Expression> factory = new MySQLExpressionFactory(ctx.expr());
        SortDirection direction = null;
        if (ctx.ASC() != null) {
            direction = SortDirection.ASC;
        } else if (ctx.DESC() != null) {
            direction = SortDirection.DESC;
        }
        return new SortKey(ctx, factory.generate(), direction);
    }

    @Override
    public SortKey visitSort_key_for_group_by(Sort_key_for_group_byContext ctx) {
        StatementFactory<Expression> factory = new MySQLExpressionFactory(ctx.expr());
        SortDirection direction = null;
        if (ctx.ASC() != null) {
            direction = SortDirection.ASC;
        } else if (ctx.DESC() != null) {
            direction = SortDirection.DESC;
        }
        return new SortKey(ctx, factory.generate(), direction);
    }

}
