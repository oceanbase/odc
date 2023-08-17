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
package com.oceanbase.odc.service.db.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import com.oceanbase.odc.service.db.parser.listener.OBOracleCallPLByAnonymousBlockListener;
import com.oceanbase.odc.service.db.parser.result.ParserCallPLByAnonymousBlockResult;
import com.oceanbase.tools.dbbrowser.parser.TimeoutTokenStream;
import com.oceanbase.tools.sqlparser.FastFailErrorListener;
import com.oceanbase.tools.sqlparser.FastFailErrorStrategy;
import com.oceanbase.tools.sqlparser.oracle.PlSqlLexer;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser;
import com.oceanbase.tools.sqlparser.util.CaseChangingCharStream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AnonymousBlockParser {

    public static ParserCallPLByAnonymousBlockResult parserCallPLAnonymousBlockResult(final String pl,
            long timeoutMillis) {
        CharStream input = CharStreams.fromString(pl);
        CaseChangingCharStream caseChangingCharStream = new CaseChangingCharStream(input, true);
        PlSqlLexer lexer = new PlSqlLexer(caseChangingCharStream);
        lexer.removeErrorListeners();
        lexer.addErrorListener(new FastFailErrorListener());
        CommonTokenStream tokens;
        if (timeoutMillis <= 0) {
            tokens = new CommonTokenStream(lexer);
        } else {
            tokens = new TimeoutTokenStream(lexer, timeoutMillis);
        }
        PlSqlParser parser = new PlSqlParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new FastFailErrorListener());
        parser.setErrorHandler(new FastFailErrorStrategy());
        ParseTree tree = parser.sql_script();
        OBOracleCallPLByAnonymousBlockListener listener =
                new OBOracleCallPLByAnonymousBlockListener();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, tree);
        return new ParserCallPLByAnonymousBlockResult(listener);
    }

}
