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

package com.oceanbase.odc.core.sql.parser;

import java.io.StringReader;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;

import com.oceanbase.tools.sqlparser.BaseSQLParser;
import com.oceanbase.tools.sqlparser.SyntaxErrorException;

import lombok.NonNull;

/**
 * {@link BaseOBAstFactory}
 *
 * @author yh263208
 * @date 2023-11-17 15:33
 * @since ODC_release_4.2.3
 */
public abstract class BaseOBAstFactory implements AbstractSyntaxTreeFactory {

    private long timeoutMillis;

    public BaseOBAstFactory(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis <= 0 ? Long.MAX_VALUE : timeoutMillis;
    }

    @Override
    public AbstractSyntaxTree buildAst(@NonNull String statement) throws SyntaxErrorException {
        long start = System.currentTimeMillis();
        SyntaxErrorException thrown;
        BaseSQLParser<? extends Lexer, ? extends Parser> sqlParser = getSqlParser();
        try {
            return doBuildAst(sqlParser.buildAst(new StringReader(statement)), sqlParser);
        } catch (SyntaxErrorException e) {
            thrown = e;
        }
        this.timeoutMillis = this.timeoutMillis - (System.currentTimeMillis() - start);
        if (this.timeoutMillis <= 0) {
            throw new ParseCancellationException("Timeout, abort!");
        }
        BaseSQLParser<? extends Lexer, ? extends Parser> plParser = getPlParser();
        try {
            return doBuildAst(plParser.buildAst(new StringReader(statement)), plParser);
        } catch (SyntaxErrorException e) {
            throw thrown;
        }
    }

    protected abstract AbstractSyntaxTree doBuildAst(ParseTree parseTree,
            BaseSQLParser<? extends Lexer, ? extends Parser> from);

    /**
     * get parser for sql parsing
     */
    protected abstract BaseSQLParser<? extends Lexer, ? extends Parser> getSqlParser();

    /**
     * get parser for pl parsing
     */
    protected abstract BaseSQLParser<? extends Lexer, ? extends Parser> getPlParser();

}
