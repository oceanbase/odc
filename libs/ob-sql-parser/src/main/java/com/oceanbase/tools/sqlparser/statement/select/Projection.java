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
package com.oceanbase.tools.sqlparser.statement.select;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Statement;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link Projection}
 *
 * @author yh263208
 * @date 2022-11-24 21:43
 * @since ODC_release_4.1.0
 * @see Statement
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class Projection extends BaseStatement {

    private final Expression column;
    private final String columnLabel;
    private final boolean star;

    public Projection(@NonNull ParserRuleContext ruleContext,
            @NonNull Expression column, String columnLabel) {
        super(ruleContext);
        this.column = column;
        this.columnLabel = columnLabel;
        this.star = false;
    }

    public Projection(@NonNull TerminalNode terminalNode) {
        super(terminalNode);
        this.column = null;
        this.columnLabel = null;
        this.star = true;
    }

    public Projection(@NonNull Expression column, String columnLabel) {
        this.column = column;
        this.columnLabel = columnLabel;
        this.star = false;
    }

    public Projection() {
        this.column = null;
        this.columnLabel = null;
        this.star = true;
    }

    @Override
    public String toString() {
        if (this.star) {
            return "*";
        }
        StringBuilder builder = new StringBuilder();
        if (this.column != null) {
            builder.append(this.column.toString());
        }
        if (this.columnLabel != null) {
            builder.append(" AS ").append(this.columnLabel);
        }
        return builder.toString();
    }

}
