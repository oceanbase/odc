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
package com.oceanbase.odc.service.onlineschemachange.ddl;

import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.tools.sqlparser.FastFailErrorListener;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Alter_table_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Create_table_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Relation_factorContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Relation_nameContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseListener;

public class OBMysqlTableNameReplacer implements TableNameReplacer {

    public String replaceCreateStmt(String originCreateStmt, String newTableName) {
        CharStream charStream = CharStreams.fromString(originCreateStmt);
        OBLexer lexer = new OBLexer(charStream);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.getInterpreter().clearDFA();
        parser.removeErrorListeners();
        parser.addErrorListener(new FastFailErrorListener());
        parser.setTrace(false);

        TokenStreamRewriter tokenStreamRewriter = new TokenStreamRewriter(tokens);
        WalkerOBParserReplaceStatementListener eventParser = new WalkerOBParserReplaceStatementListener(
                tokenStreamRewriter, newTableName);

        new ParseTreeWalker().walk(eventParser, parser.sql_stmt());
        return tokenStreamRewriter.getText();
    }

    @Override
    public String replaceAlterStmt(String originAlterStmt, String newTableName) {
        return replaceCreateStmt(originAlterStmt, newTableName);
    }

    static class WalkerOBParserReplaceStatementListener extends OBParserBaseListener {
        private final TokenStreamRewriter tokenStreamRewriter;
        private final String newTableName;

        public WalkerOBParserReplaceStatementListener(TokenStreamRewriter tokenStreamRewriter, String newTableName) {
            this.tokenStreamRewriter = tokenStreamRewriter;
            this.newTableName = newTableName;
        }

        @Override
        public void enterAlter_table_stmt(Alter_table_stmtContext ctx) {
            relationFactorReplace(ctx.relation_factor());
        }

        @Override
        public void enterCreate_table_stmt(Create_table_stmtContext ctx) {
            relationFactorReplace(ctx.relation_factor());
        }

        private void relationFactorReplace(Relation_factorContext relation_factorContext) {
            List<Relation_nameContext> relation_nameContexts = relation_factorContext.normal_relation_factor()
                    .relation_name();

            PreConditions.notEmpty(relation_nameContexts, "Table name");
            Relation_nameContext relation_nameContext = relation_nameContexts.get(relation_nameContexts.size() - 1);

            ParseTree parseTree = relation_nameContext.getChild(relation_nameContext.getChildCount() - 1);
            if (parseTree instanceof TerminalNode) {
                TerminalNode terminalNode = (TerminalNode) parseTree;
                tokenStreamRewriter.replace(terminalNode.getSymbol(), newTableName);
            }
        }

    }
}
