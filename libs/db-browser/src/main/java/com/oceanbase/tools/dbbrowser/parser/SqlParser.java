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
package com.oceanbase.tools.dbbrowser.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import com.oceanbase.tools.dbbrowser.parser.listener.CustomErrorListener;
import com.oceanbase.tools.dbbrowser.parser.listener.MysqlModeSqlParserListener;
import com.oceanbase.tools.dbbrowser.parser.listener.OracleModeSqlParserListener;
import com.oceanbase.tools.dbbrowser.parser.result.ParseSqlResult;
import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2021/8/25
 */

@Slf4j
public class SqlParser {

    public static ParseSqlResult parseMysql(final String sql) {
        return parseMysql(sql, 0);
    }

    public static ParseSqlResult parseMysql(final String sql, long timeoutMillis) {
        long startTime = System.currentTimeMillis();
        CharStream input = CharStreams.fromString(sql);
        // Lexer-Lexical analysis
        // Create a lexer that feeds off of input CharStream.
        OBLexer lexer = new OBLexer(input);
        // Create a buffer of tokens pulled from the lexer.
        CommonTokenStream tokens;
        if (timeoutMillis <= 0) {
            tokens = new CommonTokenStream(lexer);
        } else {
            tokens = new TimeoutTokenStream(lexer, timeoutMillis);
        }
        // Parser-Syntax analysis
        // Create a parser that feeds off the tokens buffer.
        OBParser parser = new OBParser(tokens);
        parser.addErrorListener(new CustomErrorListener());
        log.info("Time cost for sql parsing is {}ms, sql={}", (System.currentTimeMillis() - startTime), sql);
        return parseMysql(parser.stmt());
    }

    public static ParseSqlResult parseMysql(@NonNull ParseTree parseTree) {
        // listener for parse target
        MysqlModeSqlParserListener listener = new MysqlModeSqlParserListener();

        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, parseTree);
        return new ParseSqlResult(listener);
    }

    public static ParseSqlResult parseOracle(final String sql) {
        return parseOracle(sql, 0);
    }

    public static ParseSqlResult parseOracle(final String sql, long timeoutMillis) {
        long startTime = System.currentTimeMillis();
        CharStream input = CharStreams.fromString(sql);
        // Lexer-Lexical analysis
        // Create a lexer that feeds off of input CharStream.
        com.oceanbase.tools.sqlparser.oboracle.OBLexer lexer =
                new com.oceanbase.tools.sqlparser.oboracle.OBLexer(input);
        // Create a buffer of tokens pulled from the lexer.
        CommonTokenStream tokens;
        if (timeoutMillis <= 0) {
            tokens = new CommonTokenStream(lexer);
        } else {
            tokens = new TimeoutTokenStream(lexer, timeoutMillis);
        }
        // Parser-Syntax analysis
        // Create a parser that feeds off the tokens buffer.
        com.oceanbase.tools.sqlparser.oboracle.OBParser parser =
                new com.oceanbase.tools.sqlparser.oboracle.OBParser(tokens);
        parser.addErrorListener(new CustomErrorListener());
        log.info("Time cost for sql parsing is {}ms, sql={}", (System.currentTimeMillis() - startTime), sql);
        return parseOracle(parser.stmt());
    }

    public static ParseSqlResult parseOracle(@NonNull ParseTree parseTree) {
        OracleModeSqlParserListener listener = new OracleModeSqlParserListener();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, parseTree);
        return new ParseSqlResult(listener);
    }

}
