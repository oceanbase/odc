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
package com.oceanbase.odc.core.sql.split;

import org.antlr.v4.runtime.Lexer;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.tools.sqlparser.oboracle.PLLexer;
import com.oceanbase.tools.sqlparser.oracle.PlSqlLexer;

public class LexerFactories {
    private static final LexerFactory OB_ORACLE_LEXER_FACTORY = new OBOraclePLLexerFactory();
    private static final LexerFactory ORACLE_LEXER_FACTORY = new OracleLexerFactory();

    public static LexerFactory of(Class<? extends Lexer> lexerType) {
        PreConditions.notNull(lexerType, "lexerType");
        if (PLLexer.class == lexerType) {
            return OB_ORACLE_LEXER_FACTORY;
        }
        if (PlSqlLexer.class == lexerType) {
            return ORACLE_LEXER_FACTORY;
        }
        throw new UnsupportedException("LexerType not supported, lexerType=" + lexerType.getName());
    }

}
