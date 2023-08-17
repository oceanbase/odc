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
package com.oceanbase.tools.sqlparser.statement.createtable;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link SetComment}
 *
 * @author yh263208
 * @da 2023-07-31 11:03
 * @since ODC_release_4.2.0
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class SetComment extends BaseStatement {

    private final RelationFactor table;
    private final ColumnReference column;
    private final String comment;

    public SetComment(@NonNull ParserRuleContext context, @NonNull RelationFactor table, @NonNull String comment) {
        super(context);
        this.comment = comment;
        this.table = table;
        this.column = null;
    }

    public SetComment(@NonNull ParserRuleContext context, @NonNull ColumnReference column, @NonNull String comment) {
        super(context);
        this.comment = comment;
        this.table = null;
        this.column = column;
    }

    public SetComment(@NonNull RelationFactor table, @NonNull String comment) {
        this.comment = comment;
        this.table = table;
        this.column = null;
    }

    public SetComment(@NonNull ColumnReference column, @NonNull String comment) {
        this.comment = comment;
        this.table = null;
        this.column = column;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("COMMENT ON");
        if (this.table != null) {
            builder.append(" TABLE ").append(this.table);
        } else if (this.column != null) {
            builder.append(" COLUMN ").append(this.column);
        }
        return builder.append(" IS ").append(this.comment).toString();
    }

}
