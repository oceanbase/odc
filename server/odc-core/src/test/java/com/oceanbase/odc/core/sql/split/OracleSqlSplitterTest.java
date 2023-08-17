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
import org.junit.Test;

import com.oceanbase.tools.sqlparser.oracle.PlSqlLexer;

public class OracleSqlSplitterTest extends AbstractSqlSplitterTest {
    @Override
    protected Class<? extends Lexer> lexerType() {
        return PlSqlLexer.class;
    }

    /**
     * OB parser cannot handle string with backslash inside scenario
     */
    @Test
    public void split_StringWithBackslashInside() {
        verifyByFileName("src/test/resources/sql/split/sql-splitter-10-string-with-backslash-inside.yml");
    }
}
