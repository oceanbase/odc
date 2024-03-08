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

import com.oceanbase.tools.sqlparser.oboracle.PLParser.Create_function_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Create_package_body_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Create_package_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Create_procedure_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Create_trigger_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Create_type_body_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Create_type_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Pl_entry_stmt_listContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParser.Pl_schema_nameContext;
import com.oceanbase.tools.sqlparser.oboracle.PLParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;

/**
 * {@link OBOraclePLCreateStatementVisitor}
 *
 * @author yh263208
 * @date 2024-03-05 16:15
 * @since ODC_release_4.2.4
 */
class OBOraclePLCreateStatementVisitor extends PLParserBaseVisitor<CreateStatement> {

    @Override
    public CreateStatement visitPl_entry_stmt_list(Pl_entry_stmt_listContext ctx) {
        return visit(ctx.pl_entry_stmt());
    }

    @Override
    public CreateStatement visitCreate_package_stmt(Create_package_stmtContext ctx) {
        return new CreateStatement(ctx, getRelationFactor(ctx.package_block().pl_schema_name()));
    }

    @Override
    public CreateStatement visitCreate_package_body_stmt(Create_package_body_stmtContext ctx) {
        return new CreateStatement(ctx, getRelationFactor(ctx.package_body_block().pl_schema_name()));
    }

    @Override
    public CreateStatement visitCreate_procedure_stmt(Create_procedure_stmtContext ctx) {
        return new CreateStatement(ctx, getRelationFactor(ctx.plsql_procedure_source().pl_schema_name()));
    }

    @Override
    public CreateStatement visitCreate_function_stmt(Create_function_stmtContext ctx) {
        return new CreateStatement(ctx, getRelationFactor(ctx.plsql_function_source().pl_schema_name(0)));
    }

    @Override
    public CreateStatement visitCreate_trigger_stmt(Create_trigger_stmtContext ctx) {
        return new CreateStatement(ctx, getRelationFactor(ctx.plsql_trigger_source().pl_schema_name()));
    }

    @Override
    public CreateStatement visitCreate_type_stmt(Create_type_stmtContext ctx) {
        return new CreateStatement(ctx, getRelationFactor(ctx.plsql_type_spec_source().pl_schema_name()));
    }

    @Override
    public CreateStatement visitCreate_type_body_stmt(Create_type_body_stmtContext ctx) {
        return new CreateStatement(ctx, getRelationFactor(ctx.plsql_type_body_source().pl_schema_name()));
    }

    private RelationFactor getRelationFactor(Pl_schema_nameContext ctx) {
        if (ctx.sql_keyword_identifier() != null) {
            return new RelationFactor(ctx.sql_keyword_identifier(), ctx.sql_keyword_identifier().getText());
        } else if (ctx.identifier().size() <= 1) {
            return new RelationFactor(ctx, ctx.identifier(0).getText());
        }
        RelationFactor factor = new RelationFactor(ctx, ctx.identifier(1).getText());
        factor.setSchema(ctx.identifier(0).getText());
        return factor;
    }

}
