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

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Set_comment_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.createtable.SetComment;

import lombok.NonNull;

/**
 * {@link OracleSetCommentFactory}
 *
 * @author yh263208
 * @date 2023-07-31 11:11
 * @since ODC_release_4.2.0
 */
public class OracleSetCommentFactory extends OBParserBaseVisitor<SetComment> implements StatementFactory<SetComment> {

    private final ParserRuleContext context;

    public OracleSetCommentFactory(@NonNull Set_comment_stmtContext setCommentStmtContext) {
        this.context = setCommentStmtContext;
    }

    @Override
    public SetComment generate() {
        return visit(this.context);
    }

    @Override
    public SetComment visitSet_comment_stmt(Set_comment_stmtContext ctx) {
        String comment = ctx.STRING_VALUE().getText();
        if (ctx.TABLE() != null) {
            return new SetComment(ctx, OracleFromReferenceFactory.getRelationFactor(ctx.normal_relation_factor()),
                    comment);
        }
        return new SetComment(ctx, new OracleColumnRefFactory(ctx.column_definition_ref()).generate(), comment);
    }

}
