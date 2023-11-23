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

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;

import com.oceanbase.tools.sqlparser.BaseSQLParser;

/**
 * {@link OBMySQLAstFactory}
 *
 * @author yh263208
 * @date 2023-11-17 15:56
 * @since ODC_release_4.2.3
 */
class OBMySQLAstFactory extends BaseOBAstFactory {

    public OBMySQLAstFactory(long timeoutMillis) {
        super(timeoutMillis);
    }

    @Override
    protected AbstractSyntaxTree doBuildAst(ParseTree parseTree,
            BaseSQLParser<? extends Lexer, ? extends Parser> from) {
        return new OBMySQLAst(parseTree, from);
    }

    @Override
    protected BaseSQLParser<? extends Lexer, ? extends Parser> getSqlParser() {
        return new OBMySQLParser();
    }

    @Override
    protected BaseSQLParser<? extends Lexer, ? extends Parser> getPlParser() {
        return new OBMySQLPLParser();
    }

}
