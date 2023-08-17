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
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Delete_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Opt_where_extensionContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.delete.Delete;

import lombok.NonNull;

/**
 * {@link OracleDeleteFactory}
 *
 * @author yh263208
 * @date 2022-12-20 17:08
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class OracleDeleteFactory extends OBParserBaseVisitor<Delete> implements StatementFactory<Delete> {

    private final Delete_stmtContext deleteStmtContext;

    public OracleDeleteFactory(@NonNull Delete_stmtContext deleteStmtContext) {
        this.deleteStmtContext = deleteStmtContext;
    }

    @Override
    public Delete generate() {
        return visit(this.deleteStmtContext);
    }

    @Override
    public Delete visitDelete_stmt(Delete_stmtContext ctx) {
        OracleFromReferenceFactory fromFactory = new OracleFromReferenceFactory(ctx.table_factor());
        Delete delete = new Delete(ctx, fromFactory.generate());

        Opt_where_extensionContext whereExtension = ctx.opt_where_extension();
        if (whereExtension != null) {
            if (whereExtension.opt_where() == null) {
                StatementFactory<Expression> factory = new OracleExpressionFactory(whereExtension.obj_access_ref());
                delete.setCursor(true);
                delete.setWhere(factory.generate());
            } else if (whereExtension.opt_where().expr() != null) {
                StatementFactory<Expression> factory = new OracleExpressionFactory(whereExtension.opt_where().expr());
                delete.setCursor(false);
                delete.setWhere(factory.generate());
            }
        } else if (ctx.expr() != null) {
            StatementFactory<Expression> factory = new OracleExpressionFactory(ctx.expr());
            delete.setCursor(false);
            delete.setWhere(factory.generate());
        }
        return delete;
    }

}
