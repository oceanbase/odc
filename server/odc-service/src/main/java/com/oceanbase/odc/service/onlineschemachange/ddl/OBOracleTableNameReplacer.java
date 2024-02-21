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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeSqlType;
import com.oceanbase.tools.sqlparser.FastFailErrorListener;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Alter_table_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_index_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Create_table_stmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Index_nameContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Relation_factorContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Relation_nameContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseListener;

public class OBOracleTableNameReplacer implements TableNameReplacer {

    public ReplaceResult replaceCreateStmt(String originCreateStmt, String newTableName) {

        ReplaceResult result = new ReplaceResult();
        ReplaceElement toReplaceElement = new ReplaceElement();
        toReplaceElement.setNewValue(newTableName);
        toReplaceElement.setReplaceType(ReplaceType.TABLE_NAME);
        String newSql = getRewriteSql(originCreateStmt,
                rewriter -> new CreateOBParserReplaceStatementListener(rewriter,
                        Collections.singletonList(toReplaceElement), result));

        result.setNewSql(newSql);
        result.setOldSql(originCreateStmt);
        return result;
    }

    @Override
    public ReplaceResult replaceAlterStmt(String originAlterStmt, String newTableName) {
        ReplaceResult result = new ReplaceResult();
        ReplaceElement toReplaceElement = new ReplaceElement();
        toReplaceElement.setNewValue(newTableName);
        toReplaceElement.setReplaceType(ReplaceType.TABLE_NAME);
        String newSql = getRewriteSql(originAlterStmt,
                rewriter -> new AlterOBParserReplaceStatementListener(rewriter,
                        Collections.singletonList(toReplaceElement), result));

        result.setNewSql(newSql);
        result.setOldSql(originAlterStmt);
        return result;
    }

    @Override
    public ReplaceResult replaceStmtValue(OnlineSchemaChangeSqlType sqlType,
            String originSql, List<ReplaceElement> replaceElements) {
        ReplaceResult result = new ReplaceResult();
        String newSql;
        if (sqlType == OnlineSchemaChangeSqlType.ALTER) {
            newSql = getRewriteSql(originSql,
                    rewriter -> new AlterOBParserReplaceStatementListener(rewriter, replaceElements,
                            result));
        } else {
            newSql = getRewriteSql(originSql,
                    rewriter -> new CreateOBParserReplaceStatementListener(rewriter, replaceElements,
                            result));
        }
        result.setNewSql(newSql);
        result.setOldSql(originSql);
        return result;
    }

    @Override
    public String replaceCreateIndexStmt(String originCreateIndexStmt, String newTableName) {
        return getRewriteSql(originCreateIndexStmt,
                rewriter -> new CreateIndexOBParserReplaceStatementListener(rewriter, newTableName));
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

    static class CreateIndexOBParserReplaceStatementListener extends OBParserBaseListener {
        private final TokenStreamRewriter tokenStreamRewriter;
        private final String newTableName;

        public CreateIndexOBParserReplaceStatementListener(TokenStreamRewriter tokenStreamRewriter,
                String newTableName) {
            this.tokenStreamRewriter = tokenStreamRewriter;
            this.newTableName = newTableName;
        }

        @Override
        public void enterCreate_index_stmt(Create_index_stmtContext ctx) {
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

    static class AlterOBParserReplaceStatementListener extends OBParserBaseListener {
        private final TokenStreamRewriter tokenStreamRewriter;
        private final List<ReplaceElement> toReplaceElement;
        private final ReplaceResult replaceResult;

        public AlterOBParserReplaceStatementListener(TokenStreamRewriter tokenStreamRewriter,
                List<ReplaceElement> toReplaceElement,
                ReplaceResult replaceResult) {
            this.tokenStreamRewriter = tokenStreamRewriter;
            this.toReplaceElement = toReplaceElement;
            this.replaceResult = replaceResult;
        }

        @Override
        public void enterAlter_table_stmt(Alter_table_stmtContext ctx) {
            relationFactorReplace(ctx.relation_factor());
        }

        private void relationFactorReplace(Relation_factorContext relation_factorContext) {
            Relation_nameContext relation_nameContext = relation_factorContext.normal_relation_factor()
                    .relation_name();

            PreConditions.notNull(relation_nameContext, "Table name");

            List<ReplaceElement> replaceElements = getReplaceElements(toReplaceElement, ReplaceType.TABLE_NAME);
            if (replaceElements.isEmpty()) {
                return;
            }
            ParseTree parseTree = relation_nameContext.getChild(relation_nameContext.getChildCount() - 1);
            if (parseTree instanceof TerminalNode) {
                TerminalNode terminalNode = (TerminalNode) parseTree;
                tokenStreamRewriter.replace(terminalNode.getSymbol(), replaceElements.get(0).getNewValue());
                ReplaceElement replaceElement = new ReplaceElement();
                replaceElement.setNewValue(replaceElements.get(0).getNewValue());
                replaceElement.setOldValue(terminalNode.toString());
                replaceElement.setReplaceType(ReplaceType.TABLE_NAME);
                replaceResult.getReplaceElements().add(replaceElement);
            }
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
                    List<ReplaceElement> replaceElements =
                            getReplaceElements(toReplaceElement, ReplaceType.CONSTRAINT_NAME);
                    Map<String, ReplaceElement> oldValueMap = replaceElements.stream().collect(
                            Collectors.toMap(ReplaceElement::getOldValue, v -> v));
                    String newValue = oldValueMap.containsKey(terminalNode.toString())
                            ? oldValueMap.get(terminalNode.toString()).getNewValue()
                            : "A" + StringUtils.uuidNoHyphen();
                    tokenStreamRewriter.replace(terminalNode.getSymbol(), newValue);
                    ReplaceElement replaceElement = new ReplaceElement();
                    replaceElement.setNewValue(newValue);
                    replaceElement.setOldValue(terminalNode.toString());
                    replaceElement.setReplaceType(ReplaceType.CONSTRAINT_NAME);
                    replaceResult.getReplaceElements().add(replaceElement);
                }
            }
        }

    }

    static class CreateOBParserReplaceStatementListener extends OBParserBaseListener {
        private final TokenStreamRewriter tokenStreamRewriter;
        private final List<ReplaceElement> toReplaceElement;
        private final ReplaceResult replaceResult;

        public CreateOBParserReplaceStatementListener(TokenStreamRewriter tokenStreamRewriter,
                List<ReplaceElement> toReplaceElement,
                ReplaceResult replaceResult) {
            this.tokenStreamRewriter = tokenStreamRewriter;
            this.toReplaceElement = toReplaceElement;
            this.replaceResult = replaceResult;
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
                    String newValue = "A" + StringUtils.uuidNoHyphen();
                    tokenStreamRewriter.replace(terminalNode.getSymbol(), newValue);
                    ReplaceElement replaceElement = new ReplaceElement();
                    replaceElement.setNewValue(newValue);
                    replaceElement.setOldValue(terminalNode.toString());
                    replaceElement.setReplaceType(ReplaceType.CONSTRAINT_NAME);
                    replaceResult.getReplaceElements().add(replaceElement);
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

                    String newValue = "A" + StringUtils.uuidNoHyphen();
                    tokenStreamRewriter.replace(terminalNode.getSymbol(), newValue);
                    ReplaceElement replaceElement = new ReplaceElement();
                    replaceElement.setNewValue(newValue);
                    replaceElement.setOldValue(terminalNode.toString());
                    replaceElement.setReplaceType(ReplaceType.INDEX_NAME);
                    replaceResult.getReplaceElements().add(replaceElement);

                }
            }
        }

        private void relationFactorReplace(Relation_factorContext relation_factorContext) {
            Relation_nameContext relation_nameContext = relation_factorContext.normal_relation_factor()
                    .relation_name();
            PreConditions.notNull(relation_factorContext, "Table name");

            ParseTree parseTree = relation_nameContext.getChild(relation_nameContext.getChildCount() - 1);
            if (parseTree instanceof TerminalNode) {
                List<ReplaceElement> replaceElements = getReplaceElements(toReplaceElement, ReplaceType.TABLE_NAME);
                if (replaceElements.isEmpty()) {
                    return;
                }
                TerminalNode terminalNode = (TerminalNode) parseTree;
                tokenStreamRewriter.replace(terminalNode.getSymbol(), replaceElements.get(0).getNewValue());
                ReplaceElement replaceElement = new ReplaceElement();
                replaceElement.setNewValue(replaceElements.get(0).getNewValue());
                replaceElement.setOldValue(terminalNode.toString());
                replaceElement.setReplaceType(ReplaceType.TABLE_NAME);
                replaceResult.getReplaceElements().add(replaceElement);
            }
        }
    }

    private static List<ReplaceElement> getReplaceElements(List<ReplaceElement> replaceElements, ReplaceType type) {
        if (CollectionUtils.isNotEmpty(replaceElements)) {
            return replaceElements.stream().filter(a -> a.getReplaceType() == type).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

}
