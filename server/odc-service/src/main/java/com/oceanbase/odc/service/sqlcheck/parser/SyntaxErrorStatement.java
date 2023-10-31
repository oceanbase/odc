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

package com.oceanbase.odc.service.sqlcheck.parser;

import com.oceanbase.tools.sqlparser.SyntaxErrorException;
import com.oceanbase.tools.sqlparser.statement.Statement;

import lombok.Getter;
import lombok.NonNull;

public class SyntaxErrorStatement implements Statement {

    private final String sql;
    @Getter
    private final SyntaxErrorException exception;

    public SyntaxErrorStatement(@NonNull String sql, @NonNull SyntaxErrorException exception) {
        this.sql = sql;
        this.exception = exception;
    }

    @Override
    public String getText() {
        return this.sql;
    }

    @Override
    public int getStart() {
        return 0;
    }

    @Override
    public int getStop() {
        return this.sql.length() - 1;
    }

    @Override
    public int getLine() {
        return 1;
    }

    @Override
    public int getCharPositionInLine() {
        return 0;
    }

}
