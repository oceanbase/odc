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
import com.oceanbase.tools.sqlparser.statement.Statement;

import lombok.NonNull;

/**
 * {@link BaseAst}
 *
 * @author yh263208
 * @date 2023-11-17 15:34
 * @since ODC_release_4.2.3
 */
abstract class BaseAst implements AbstractSyntaxTree {

    private final ParseTree root;
    protected final BaseSQLParser<? extends Lexer, ? extends Parser> from;

    public BaseAst(@NonNull ParseTree root,
            @NonNull BaseSQLParser<? extends Lexer, ? extends Parser> from) {
        this.root = root;
        this.from = from;
    }

    @Override
    public ParseTree getRoot() {
        return this.root;
    }

    @Override
    public Statement getStatement() {
        return this.from.buildStatement(this.root);
    }

}
