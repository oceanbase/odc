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

import java.io.IOException;
import java.io.Reader;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import com.oceanbase.tools.sqlparser.obmysql.OBLexer;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;

/**
 * {@link OBMySQLParser}
 *
 * @author yh263208
 * @date 2022-12-13 23:37
 * @since ODC_release_4.1.0
 * @see SQLParser
 */
public class OBMySQLParser extends BaseSQLParser<OBLexer, OBParser> {

    @Override
    protected OBLexer getLexer(Reader statementReader) throws IOException {
        return new OBLexer(CharStreams.fromReader(statementReader));
    }

    @Override
    protected OBParser getParser(TokenStream tokens) {
        return new OBParser(tokens);
    }

    @Override
    protected String getStatementFactoryBasePackage() {
        return "com.oceanbase.tools.sqlparser.adapter.mysql";
    }

    @Override
    protected ParseTree doParse(OBParser parser) {
        return parser.stmt().getChild(0);
    }

}

