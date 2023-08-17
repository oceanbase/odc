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
import com.oceanbase.tools.sqlparser.obmysql.OBParser.ProjectionContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.select.Projection;

import lombok.NonNull;

/**
 * {@link MySQLProjectionFactory}
 *
 * @author yh263208
 * @date 2022-12-12 17:16
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class MySQLProjectionFactory extends OBParserBaseVisitor<Projection> implements StatementFactory<Projection> {

    private final ProjectionContext projectionContext;

    public MySQLProjectionFactory(@NonNull ProjectionContext projectionContext) {
        this.projectionContext = projectionContext;
    }

    @Override
    public Projection generate() {
        return visit(this.projectionContext);
    }

    @Override
    public Projection visitProjection(ProjectionContext ctx) {
        if (ctx.Star() != null) {
            return new Projection(ctx.Star());
        }
        StatementFactory<Expression> factory = new MySQLExpressionFactory(ctx.expr());
        String columnLabel = null;
        if (ctx.column_label() != null) {
            columnLabel = ctx.column_label().getText();
        } else if (ctx.STRING_VALUE() != null) {
            columnLabel = ctx.STRING_VALUE().getText();
        }
        return new Projection(ctx, factory.generate(), columnLabel);
    }

}
