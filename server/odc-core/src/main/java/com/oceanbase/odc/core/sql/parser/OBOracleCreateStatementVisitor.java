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

import com.oceanbase.tools.sqlparser.adapter.oracle.OracleFromReferenceFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_dblink_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_sequence_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_synonym_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_view_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;

/**
 * {@link OBOracleCreateStatementVisitor}
 *
 * @author yh263208
 * @date 2024-03-05 14:50
 * @since ODC_release_4.2.4
 */
class OBOracleCreateStatementVisitor extends OBParserBaseVisitor<CreateStatement> {

    @Override
    public CreateStatement visitCreate_view_stmt(Create_view_stmtContext ctx) {
        RelationFactor relationFactor = OracleFromReferenceFactory.getRelationFactor(ctx.view_name().relation_factor());
        return new CreateStatement(ctx, relationFactor);
    }

    @Override
    public CreateStatement visitCreate_synonym_stmt(Create_synonym_stmtContext ctx) {
        return new CreateStatement(ctx, new RelationFactor(ctx.synonym_name(), ctx.synonym_name().getText()));
    }

    @Override
    public CreateStatement visitCreate_sequence_stmt(Create_sequence_stmtContext ctx) {
        RelationFactor relationFactor = OracleFromReferenceFactory.getRelationFactor(ctx.relation_factor());
        return new CreateStatement(ctx, relationFactor);
    }

    @Override
    public CreateStatement visitCreate_dblink_stmt(Create_dblink_stmtContext ctx) {
        return new CreateStatement(ctx, new RelationFactor(ctx.dblink(), ctx.dblink().getText()));
    }

}
