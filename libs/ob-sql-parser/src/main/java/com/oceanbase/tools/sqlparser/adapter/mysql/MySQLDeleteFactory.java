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

import java.util.ArrayList;
import java.util.List;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Delete_basic_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Delete_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Multi_delete_tableContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Relation_factor_with_starContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Relation_nameContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.delete.Delete;
import com.oceanbase.tools.sqlparser.statement.delete.DeleteRelation;
import com.oceanbase.tools.sqlparser.statement.delete.MultiDelete;
import com.oceanbase.tools.sqlparser.statement.select.FromReference;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;
import com.oceanbase.tools.sqlparser.statement.select.mysql.Limit;

import lombok.NonNull;

/**
 * {@link MySQLDeleteFactory}
 *
 * @author yh263208
 * @date 2022-12-20 17:19
 * @since ODC_release_4.1.0
 * @see StatementFactory
 */
public class MySQLDeleteFactory extends OBParserBaseVisitor<Delete> implements StatementFactory<Delete> {

    private final Delete_stmtContext deleteStmtContext;

    public MySQLDeleteFactory(@NonNull Delete_stmtContext deleteStmtContext) {
        this.deleteStmtContext = deleteStmtContext;
    }

    @Override
    public Delete generate() {
        return visit(this.deleteStmtContext);
    }

    @Override
    public Delete visitDelete_stmt(Delete_stmtContext deleteCtx) {
        Delete_basic_stmtContext ctx = deleteCtx.delete_basic_stmt();
        Delete delete = null;
        if (ctx.tbl_name() != null) {
            MySQLFromReferenceFactory factory = new MySQLFromReferenceFactory(ctx.tbl_name());
            delete = new Delete(ctx, factory.generate());
        } else if (ctx.multi_delete_table() != null) {
            delete = new Delete(ctx, visitMultiTableContext(ctx.multi_delete_table()));
        }
        if (ctx.expr() != null) {
            StatementFactory<Expression> factory = new MySQLExpressionFactory(ctx.expr());
            delete.setCursor(false);
            delete.setWhere(factory.generate());
        }
        if (ctx.order_by() != null) {
            StatementFactory<OrderBy> factory = new MySQLOrderByFactory(ctx.order_by());
            delete.setOrderBy(factory.generate());
        }
        if (ctx.limit_clause() != null) {
            StatementFactory<Limit> factory = new MySQLLimitFactory(ctx.limit_clause());
            delete.setLimit(factory.generate());
        }
        return delete;
    }

    private MultiDelete visitMultiTableContext(Multi_delete_tableContext ctx) {
        List<DeleteRelation> deleteRelations = new ArrayList<>();
        boolean hasUsing = ctx.USING() != null;
        List<FromReference> tableReferences = MySQLSelectBodyFactory.visitFromList(ctx.table_references());
        for (Relation_factor_with_starContext deleteRelation : ctx.relation_with_star_list()
                .relation_factor_with_star()) {
            List<Relation_nameContext> relationNames = deleteRelation.relation_name();
            int size = relationNames.size();
            String table = (--size) < 0 ? null : relationNames.get(size).getText();
            String schema = (--size) < 0 ? null : relationNames.get(size).getText();
            boolean star = deleteRelation.Star() != null;
            deleteRelations.add(new DeleteRelation(deleteRelation, schema, table, star));
        }
        return new MultiDelete(ctx, deleteRelations, hasUsing, tableReferences);
    }

}
