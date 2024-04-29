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
package com.oceanbase.tools.sqlparser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import lombok.Getter;
import lombok.NonNull;

/**
 * {@link SyntaxErrorException}
 *
 * @author yh263208
 * @date 2022-12-13 21:01
 * @since ODC_release_4.1.0
 * @see java.lang.Exception
 */
@Getter
public class SyntaxErrorException extends ParseCancellationException {

    private final int start;
    private final int stop;
    private final String text;

    public SyntaxErrorException(@NonNull Recognizer<?, ?> recognizer, @NonNull RecognitionException e) {
        super(buildErrorMessage(recognizer, e), e);
        if (recognizer instanceof Parser) {
            Token token = e.getOffendingToken();
            this.start = token.getStartIndex();
            this.stop = token.getStopIndex();
            TokenStream tokens = (CommonTokenStream) recognizer.getInputStream();
            if (tokens == null) {
                this.text = null;
            } else {
                this.text = tokens.getTokenSource().getInputStream().toString();
            }
        } else {
            CharStream text = (CharStream) recognizer.getInputStream();
            this.start = text.index();
            this.stop = text.index();
            this.text = text.toString();
        }
    }

    private static String buildErrorMessage(Recognizer<?, ?> recognizer, RecognitionException e) {
        if (recognizer instanceof Lexer) {
            // 词法解析器报错
            CharStream charStream = (CharStream) recognizer.getInputStream();
            String token = charStream.getText(Interval.of(charStream.index(), charStream.index()));
            return "Token recognition error '" + escapeWhitespace(token) + "', at pos " + charStream.index();
        }
        // 语法解析器报错
        TokenStream tokens = (CommonTokenStream) recognizer.getInputStream();
        Token token = e.getOffendingToken();
        String input;
        if (tokens != null) {
            int start = Math.max(token.getStartIndex() - 15, 0);
            int end = token.getStopIndex();
            input = token.getTokenSource().getInputStream().getText(Interval.of(start, end));
        } else {
            input = "<unknown input>";
        }
        return "You have an error in your SQL syntax; "
                + "check the manual for the right syntax to use near"
                + " '" + escapeWsAndQuote(input)
                + "...' at line " + token.getLine()
                + ", col " + token.getCharPositionInLine();
    }

    private static String escapeWhitespace(String s) {
        StringBuilder buf = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '\t') {
                buf.append("\\t");
            } else if (c == '\n') {
                buf.append("\\n");
            } else if (c == '\r') {
                buf.append("\\r");
            } else {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    private static String escapeWsAndQuote(String s) {
        s = s.replace("\n", "\\n");
        s = s.replace("\r", "\\r");
        s = s.replace("\t", "\\t");
        return s;
    }

}

