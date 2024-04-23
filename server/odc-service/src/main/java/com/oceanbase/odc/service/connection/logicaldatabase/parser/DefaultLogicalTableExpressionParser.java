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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import com.oceanbase.odc.service.connection.logicaldatabase.LogicalTableExpressionLexer;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalTableExpressionParser;
import com.oceanbase.tools.sqlparser.statement.Statement;

/**
 * @Author: Lebie
 * @Date: 2024/4/23 10:13
 * @Description: []
 */
public class DefaultLogicalTableExpressionParser {
    public Statement parse(String expression) throws SyntaxErrorException {
        LogicalTableExpressionLexer lexer = new LogicalTableExpressionLexer(CharStreams.fromString(expression));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new FastFailErrorListener());
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LogicalTableExpressionParser parser = new LogicalTableExpressionParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new FastFailErrorListener());
        parser.setErrorHandler(new FastFailErrorStrategy());
        LogicalTableExpressionVisitor visitor = new LogicalTableExpressionVisitor();
        return visitor.visit(parser.logicalTableExpressionList());
    }
}
