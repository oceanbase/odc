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
package com.oceanbase.odc.core.sql.parser;

import com.oceanbase.tools.sqlparser.obmysql.PLParser.Create_function_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.PLParser.Create_procedure_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.PLParser.Sp_nameContext;
import com.oceanbase.tools.sqlparser.obmysql.PLParser.Stmt_blockContext;
import com.oceanbase.tools.sqlparser.obmysql.PLParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;

/**
 * {@link OBMySQLPLCreateStatementVisitor}
 *
 * @author yh263208
 * @date 2024-03-05 15:29
 * @since ODC_release_4.2.4
 */
class OBMySQLPLCreateStatementVisitor extends PLParserBaseVisitor<CreateStatement> {

    @Override
    public CreateStatement visitStmt_block(Stmt_blockContext ctx) {
        return visit(ctx.stmt_list().stmt(0));
    }

    @Override
    public CreateStatement visitCreate_procedure_stmt(Create_procedure_stmtContext ctx) {
        return new CreateStatement(ctx, getRelationFactor(ctx.sp_name()));
    }

    @Override
    public CreateStatement visitCreate_function_stmt(Create_function_stmtContext ctx) {
        return new CreateStatement(ctx, getRelationFactor(ctx.sp_name()));
    }

    private RelationFactor getRelationFactor(Sp_nameContext ctx) {
        if (ctx.ident().size() <= 1) {
            return new RelationFactor(ctx, ctx.ident(0).getText());
        }
        RelationFactor factor = new RelationFactor(ctx, ctx.ident(1).getText());
        factor.setSchema(ctx.ident(0).getText());
        return factor;
    }

}
