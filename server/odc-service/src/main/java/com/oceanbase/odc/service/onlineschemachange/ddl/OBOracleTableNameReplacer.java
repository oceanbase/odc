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

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.tools.sqlparser.FastFailErrorListener;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Alter_table_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_table_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Index_nameContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Relation_factorContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Relation_nameContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseListener;

public class OBOracleTableNameReplacer implements TableNameReplacer {

    public String replaceCreateStmt(String originCreateStmt, String newTableName) {

        CharStream charStream = CharStreams.fromString(originCreateStmt);
        OBLexer lexer = new OBLexer(charStream);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.getInterpreter().clearDFA();
        parser.removeErrorListeners();
        parser.addErrorListener(new FastFailErrorListener());
        parser.setTrace(false);
        ParseTree parseTree = parser.sql_stmt();

        TokenStreamRewriter tokenStreamRewriter = new TokenStreamRewriter(tokens);

        CreateOBParserReplaceStatementListener eventParser = new CreateOBParserReplaceStatementListener(
                tokenStreamRewriter, newTableName);
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(eventParser, parseTree);
        return tokenStreamRewriter.getText();
    }

    @Override
    public String replaceAlterStmt(String originAlterStmt, String newTableName) {
        CharStream charStream = CharStreams.fromString(originAlterStmt);
        OBLexer lexer = new OBLexer(charStream);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.getInterpreter().clearDFA();
        parser.removeErrorListeners();
        parser.addErrorListener(new FastFailErrorListener());
        parser.setTrace(false);

        TokenStreamRewriter tokenStreamRewriter = new TokenStreamRewriter(tokens);
        AlterOBParserReplaceStatementListener eventParser = new AlterOBParserReplaceStatementListener(
                tokenStreamRewriter, newTableName);

        new ParseTreeWalker().walk(eventParser, parser.sql_stmt());
        return tokenStreamRewriter.getText();
    }

    static class AlterOBParserReplaceStatementListener extends OBParserBaseListener {
        private final TokenStreamRewriter tokenStreamRewriter;
        private final String newTableName;

        public AlterOBParserReplaceStatementListener(TokenStreamRewriter tokenStreamRewriter, String newTableName) {
            this.tokenStreamRewriter = tokenStreamRewriter;
            this.newTableName = newTableName;
        }

        @Override
        public void enterAlter_table_stmt(Alter_table_stmtContext ctx) {
            relationFactorReplace(ctx.relation_factor());
        }

        private void relationFactorReplace(Relation_factorContext relation_factorContext) {
            Relation_nameContext relation_nameContext = relation_factorContext.normal_relation_factor()
                    .relation_name();

            PreConditions.notNull(relation_nameContext, "Table name");

            ParseTree parseTree = relation_nameContext.getChild(relation_nameContext.getChildCount() - 1);
            if (parseTree instanceof TerminalNode) {
                TerminalNode terminalNode = (TerminalNode) parseTree;
                tokenStreamRewriter.replace(terminalNode.getSymbol(), newTableName);
            }
        }

    }

    static class CreateOBParserReplaceStatementListener extends OBParserBaseListener {
        private final TokenStreamRewriter tokenStreamRewriter;
        private final String newTableName;

        public CreateOBParserReplaceStatementListener(TokenStreamRewriter tokenStreamRewriter, String newTableName) {
            this.tokenStreamRewriter = tokenStreamRewriter;
            this.newTableName = newTableName;
        }

        @Override
        public void enterCreate_table_stmt(Create_table_stmtContext ctx) {
            relationFactorReplace(ctx.relation_factor());
        }

        @Override
        public void enterConstraint_name(OBParser.Constraint_nameContext ctx) {
            if (ctx.getChildCount() == 0) {
                return;
            }
            ParseTree parseTree = ctx.getChild(0);
            if (parseTree instanceof Relation_nameContext) {

                Relation_nameContext relation_nameContext = (Relation_nameContext) parseTree;
                ParseTree childNode = relation_nameContext.getChild(0);
                if (childNode instanceof TerminalNode) {
                    TerminalNode terminalNode = (TerminalNode) childNode;
                    tokenStreamRewriter.replace(terminalNode.getSymbol(), DdlUtils.getNewNameWithSuffix(
                            terminalNode.getSymbol().getText(), DdlUtils.getUUIDWithoutUnderline()));
                }
            }
        }

        @Override
        public void enterIndex_name(Index_nameContext ctx) {
            if (ctx.getChildCount() == 0) {
                return;
            }
            ParseTree parseTree = ctx.getChild(0);
            if (parseTree instanceof Relation_nameContext) {

                Relation_nameContext relation_nameContext = (Relation_nameContext) parseTree;
                ParseTree childNode = relation_nameContext.getChild(0);
                if (childNode instanceof TerminalNode) {
                    TerminalNode terminalNode = (TerminalNode) childNode;
                    tokenStreamRewriter.replace(terminalNode.getSymbol(), DdlUtils.getNewNameWithSuffix(
                            terminalNode.getSymbol().getText(), DdlUtils.getUUIDWithoutUnderline()));
                }
            }
        }

        private void relationFactorReplace(Relation_factorContext relation_factorContext) {
            Relation_nameContext relation_nameContext = relation_factorContext.normal_relation_factor()
                    .relation_name();
            PreConditions.notNull(relation_factorContext, "Table name");

            ParseTree parseTree = relation_nameContext.getChild(relation_nameContext.getChildCount() - 1);
            if (parseTree instanceof TerminalNode) {
                TerminalNode terminalNode = (TerminalNode) parseTree;
                tokenStreamRewriter.replace(terminalNode.getSymbol(), newTableName);
            }
        }
    }

}
