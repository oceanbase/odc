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

import java.io.IOException;
import java.io.Reader;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import com.oceanbase.tools.sqlparser.BaseSQLParser;
import com.oceanbase.tools.sqlparser.obmysql.PLLexer;
import com.oceanbase.tools.sqlparser.obmysql.PLParser;
import com.oceanbase.tools.sqlparser.statement.Statement;

/**
 * {@link OBMySQLPLParser}
 *
 * @author yh263208
 * @date 2023-11-17 15:06
 * @since ODC_release_4.2.3
 */
final class OBMySQLPLParser extends BaseSQLParser<PLLexer, PLParser> {

    @Override
    protected String getStatementFactoryBasePackage() {
        return null;
    }

    @Override
    public Statement buildStatement(ParseTree root) {
        return null;
    }

    @Override
    protected ParseTree doParse(PLParser parser) {
        return parser.stmt_block();
    }

    @Override
    protected PLLexer getLexer(Reader statementReader) throws IOException {
        return new PLLexer(CharStreams.fromReader(statementReader));
    }

    @Override
    protected PLParser getParser(TokenStream tokens) {
        return new PLParser(tokens);
    }

}
