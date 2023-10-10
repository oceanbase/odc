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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.tools.sqlparser.FastFailErrorListener;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Alter_table_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Constraint_nameContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Create_table_stmtContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Out_of_line_constraintContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Relation_factorContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Relation_nameContext;
import com.oceanbase.tools.sqlparser.obmysql.OBParserBaseListener;

public class OBMysqlTableNameReplacer implements TableNameReplacer {

    private static final String CONSTRAINT_KEYWORD = "CONSTRAINT";
    private static final String FOREIGN_KEYWORD = "FOREIGN";

    public String replaceCreateStmt(String originCreateStmt, String newTableName) {
        return getRewriteSql(originCreateStmt,
                rewriter -> new CreateWalkerOBParserReplaceStatementListener(rewriter, newTableName));
    }

    @Override
    public String replaceAlterStmt(String originAlterStmt, String newTableName) {
        return getRewriteSql(originAlterStmt,
                rewriter -> new WalkerOBParserReplaceStatementListener(rewriter, newTableName));
    }

    private static String getRewriteSql(String originCreateStmt,
            Function<TokenStreamRewriter, OBParserBaseListener> obParserBaseListenerFunc) {
        CharStream charStream = CharStreams.fromString(originCreateStmt);
        OBLexer lexer = new OBLexer(charStream);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBParser parser = new OBParser(tokens);
        parser.getInterpreter().clearDFA();
        parser.removeErrorListeners();
        parser.addErrorListener(new FastFailErrorListener());
        parser.setTrace(false);

        TokenStreamRewriter tokenStreamRewriter = new TokenStreamRewriter(tokens);
        new ParseTreeWalker().walk(obParserBaseListenerFunc.apply(tokenStreamRewriter), parser.sql_stmt());
        return tokenStreamRewriter.getText();
    }

    static class WalkerOBParserReplaceStatementListener extends OBParserBaseListener {
        protected final TokenStreamRewriter tokenStreamRewriter;
        protected final String newTableName;

        public WalkerOBParserReplaceStatementListener(TokenStreamRewriter tokenStreamRewriter, String newTableName) {
            this.tokenStreamRewriter = tokenStreamRewriter;
            this.newTableName = newTableName;
        }

        @Override
        public void enterAlter_table_stmt(Alter_table_stmtContext ctx) {
            relationFactorReplace(ctx.relation_factor());
        }

        protected void relationFactorReplace(Relation_factorContext relation_factorContext) {
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

    static class CreateWalkerOBParserReplaceStatementListener extends WalkerOBParserReplaceStatementListener {
        private final AtomicBoolean IS_CONSTRAINT_FOREIGN_KEY = new AtomicBoolean(false);

        public CreateWalkerOBParserReplaceStatementListener(TokenStreamRewriter tokenStreamRewriter,
                String newTableName) {
            super(tokenStreamRewriter, newTableName);
        }

        @Override
        public void enterCreate_table_stmt(Create_table_stmtContext ctx) {
            relationFactorReplace(ctx.relation_factor());
        }

        @Override
        public void enterOut_of_line_constraint(Out_of_line_constraintContext ctx) {
            if (ctx.getChildCount() == 0) {
                return;
            }
            ParseTree firstChild = ctx.getChild(0);
            if (firstChild instanceof TerminalNodeImpl) {
                String keyword = ((TerminalNodeImpl) firstChild).getSymbol().getText();
                if (CONSTRAINT_KEYWORD.equalsIgnoreCase(keyword) && ctx.getChildCount() >= 3) {
                    if (ctx.getChild(2) instanceof TerminalNodeImpl) {
                        if (FOREIGN_KEYWORD.equalsIgnoreCase(
                                ((TerminalNodeImpl) ctx.getChild(2)).getSymbol().getText())) {
                            IS_CONSTRAINT_FOREIGN_KEY.getAndSet(true);
                        }
                    }
                }
            }
        }

        @Override
        public void exitOut_of_line_constraint(Out_of_line_constraintContext ctx) {
            IS_CONSTRAINT_FOREIGN_KEY.getAndSet(false);
        }

        @Override
        public void enterConstraint_name(Constraint_nameContext ctx) {
            if (ctx.getChildCount() == 0 || !IS_CONSTRAINT_FOREIGN_KEY.get()) {
                return;
            }
            ParseTree parseTree = ctx.getChild(0);
            if (parseTree instanceof Relation_nameContext) {

                Relation_nameContext relation_nameContext = (Relation_nameContext) parseTree;
                ParseTree childNode = relation_nameContext.getChild(0);
                if (childNode instanceof TerminalNode) {
                    TerminalNode terminalNode = (TerminalNode) childNode;
                    tokenStreamRewriter.replace(terminalNode.getSymbol(), "A" + StringUtils.uuidNoHyphen());
                }
            }
        }
    }
}
