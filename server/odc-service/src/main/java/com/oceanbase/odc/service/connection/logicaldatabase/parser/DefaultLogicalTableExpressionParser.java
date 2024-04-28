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
package com.oceanbase.odc.service.connection.logicaldatabase.parser;

import java.io.IOException;
import java.io.Reader;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import com.oceanbase.odc.service.connection.logicaldatabase.LogicalTableExpressionLexer;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalTableExpressionParser;
import com.oceanbase.tools.sqlparser.BaseSQLParser;
import com.oceanbase.tools.sqlparser.statement.Statement;

/**
 * @Author: Lebie
 * @Date: 2024/4/23 10:13
 * @Description: []
 */
public class DefaultLogicalTableExpressionParser
        extends BaseSQLParser<LogicalTableExpressionLexer, LogicalTableExpressionParser> {

    @Override
    protected ParseTree doParse(LogicalTableExpressionParser parser) {
        return parser.logicalTableExpressionList();
    }

    @Override
    protected LogicalTableExpressionLexer getLexer(Reader statementReader) throws IOException {
        return new LogicalTableExpressionLexer(CharStreams.fromReader(statementReader));
    }

    @Override
    protected LogicalTableExpressionParser getParser(TokenStream tokens) {
        return new LogicalTableExpressionParser(tokens);
    }

    @Override
    protected String getStatementFactoryBasePackage() {
        return null;
    }

    @Override
    public Statement buildStatement(ParseTree root) {
        return new LogicalTableExpressionVisitor().visit(root);
    }
}
