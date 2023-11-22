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

import com.oceanbase.tools.dbbrowser.parser.listener.LogErrorListener;
import com.oceanbase.tools.dbbrowser.parser.listener.MysqlModePLParserListener;
import com.oceanbase.tools.dbbrowser.parser.listener.OracleModePLParserListener;
import com.oceanbase.tools.dbbrowser.parser.listener.OracleModeParserListener;
import com.oceanbase.tools.dbbrowser.parser.result.ParseMysqlPLResult;
import com.oceanbase.tools.dbbrowser.parser.result.ParseOraclePLResult;
import com.oceanbase.tools.sqlparser.FastFailErrorListener;
import com.oceanbase.tools.sqlparser.FastFailErrorStrategy;
import com.oceanbase.tools.sqlparser.obmysql.PLLexer;
import com.oceanbase.tools.sqlparser.oracle.PlSqlLexer;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser;
import com.oceanbase.tools.sqlparser.util.CaseChangingCharStream;
import com.oceanbase.tools.sqlparser.util.TimeoutTokenStream;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2021/8/25
 */

@Slf4j
public class PLParser {

    public static ParseMysqlPLResult parseObMysql(final String pl) {
        return parseObMysql(pl, 0);
    }

    public static ParseMysqlPLResult parseObMysql(final String pl, long timeoutMillis) {
        long startTime = System.currentTimeMillis();
        CharStream input = CharStreams.fromString(pl);
        // Lexer-Lexical analysis
        // Create a lexer that feeds off of input CharStream
        PLLexer lexer = new PLLexer(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(new FastFailErrorListener());
        // Create a buffer of tokens pulled from the lexer
        CommonTokenStream tokens;
        if (timeoutMillis <= 0) {
            tokens = new CommonTokenStream(lexer);
        } else {
            tokens = new TimeoutTokenStream(lexer, timeoutMillis);
        }
        // Parser-Syntax analysis
        // Create a parser that feeds off the tokens buffer
        com.oceanbase.tools.sqlparser.obmysql.PLParser parser =
                new com.oceanbase.tools.sqlparser.obmysql.PLParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new FastFailErrorListener());
        parser.setErrorHandler(new FastFailErrorStrategy());
        log.info("Time cost for pl parsing is {}ms, pl={}", (System.currentTimeMillis() - startTime), pl);
        return parseObMysql(parser.stmt_block());
    }

    public static ParseMysqlPLResult parseObMysql(@NonNull ParseTree parseTree) {
        // listener for parse target
        MysqlModePLParserListener listener = new MysqlModePLParserListener();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, parseTree);
        return new ParseMysqlPLResult(listener);
    }

    public static ParseOraclePLResult parseObOracle(final String pl) {
        return parseObOracle(pl, 0);
    }

    public static ParseOraclePLResult parseObOracle(final String pl, long timeoutMillis) {
        long startTime = System.currentTimeMillis();
        ParseOraclePLResult result =
                parseByRule(pl, com.oceanbase.tools.sqlparser.oboracle.PLParser.RULE_pl_entry_stmt_list, timeoutMillis);
        if (result.isEmpty()) {
            log.warn("Pl parse failed by using pl_entry_stmt_list rule, try pl_ddl_stmt rule, sql={}", pl);
            result = parseByRule(pl, com.oceanbase.tools.sqlparser.oboracle.PLParser.RULE_pl_ddl_stmt, timeoutMillis);
        }
        log.info("Time cost for pl parsing is {}ms, pl={}", (System.currentTimeMillis() - startTime), pl);
        return result;
    }

    private static ParseOraclePLResult parseByRule(String pl, int paserRule, long timeoutMillis) {
        CharStream input = CharStreams.fromString(pl);
        com.oceanbase.tools.sqlparser.oboracle.PLLexer lexer =
                new com.oceanbase.tools.sqlparser.oboracle.PLLexer(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(new FastFailErrorListener());
        CommonTokenStream tokens;
        if (timeoutMillis <= 0) {
            tokens = new CommonTokenStream(lexer);
        } else {
            tokens = new TimeoutTokenStream(lexer, timeoutMillis);
        }
        com.oceanbase.tools.sqlparser.oboracle.PLParser parser =
                new com.oceanbase.tools.sqlparser.oboracle.PLParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new FastFailErrorListener());
        parser.setErrorHandler(new FastFailErrorStrategy());
        if (paserRule == com.oceanbase.tools.sqlparser.oboracle.PLParser.RULE_pl_ddl_stmt) {
            return parseObOracle(parser.pl_ddl_stmt());
        }
        // Begin parsing at "pl_entry_stmt_list" rule,
        // if package type, use "pl_ddl_stmt"
        return parseObOracle(parser.pl_entry_stmt_list());
    }

    public static ParseOraclePLResult parseObOracle(@NonNull ParseTree parseTree) {
        OracleModePLParserListener listener = new OracleModePLParserListener();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, parseTree);
        return new ParseOraclePLResult(listener);
    }

    public static ParseOraclePLResult parseOracle(final String pl) {
        return parseOracle(pl, 0);
    }

    public static ParseOraclePLResult parseOracle(final String pl, long timeoutMillis) {
        CharStream input = CharStreams.fromString(pl);
        CaseChangingCharStream caseChangingCharStream = new CaseChangingCharStream(input, true);
        PlSqlLexer lexer = new PlSqlLexer(caseChangingCharStream);
        CommonTokenStream tokens;
        if (timeoutMillis <= 0) {
            tokens = new CommonTokenStream(lexer);
        } else {
            tokens = new TimeoutTokenStream(lexer, timeoutMillis);
        }
        PlSqlParser parser = new PlSqlParser(tokens);
        parser.addErrorListener(new LogErrorListener());
        return parseOracle(parser.sql_script());
    }

    public static ParseOraclePLResult parseOracle(@NonNull ParseTree parseTree) {
        OracleModeParserListener listener = new OracleModeParserListener();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, parseTree);
        return new ParseOraclePLResult(listener);
    }

}
