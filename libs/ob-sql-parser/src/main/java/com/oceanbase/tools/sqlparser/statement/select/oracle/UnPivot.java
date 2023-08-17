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
package com.oceanbase.tools.sqlparser.statement.select.oracle;

import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link UnPivot}
 *
 * @author yh263208
 * @date 2023-03-01 13:40
 * @since ODC_release_4.1.0
 * @see BaseStatement
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class UnPivot extends BaseStatement {

    private final boolean includeNulls;
    private final List<ColumnReference> forColumns;
    private final List<InItem> inItems;
    private final List<ColumnReference> unpivotColumns;
    @Setter
    private String alias;

    public UnPivot(@NonNull ParserRuleContext context,
            boolean includeNulls,
            @NonNull List<ColumnReference> unpivotColumns,
            @NonNull List<ColumnReference> forColumns,
            @NonNull List<InItem> inItems) {
        super(context);
        this.inItems = inItems;
        this.includeNulls = includeNulls;
        this.unpivotColumns = unpivotColumns;
        this.forColumns = forColumns;
    }

    public UnPivot(boolean includeNulls,
            @NonNull List<ColumnReference> unpivotColumns,
            @NonNull List<ColumnReference> forColumns,
            @NonNull List<InItem> inItems) {
        this.inItems = inItems;
        this.includeNulls = includeNulls;
        this.unpivotColumns = unpivotColumns;
        this.forColumns = forColumns;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("UNPIVOT");
        if (this.includeNulls) {
            builder.append(" INCLUDE NULLS");
        } else {
            builder.append(" EXCLUDE NULLS");
        }
        builder.append("(");
        if (this.unpivotColumns.size() == 1) {
            builder.append(this.unpivotColumns.get(0));
        } else {
            builder.append("(")
                    .append(this.unpivotColumns.stream()
                            .map(ColumnReference::toString)
                            .collect(Collectors.joining(",")))
                    .append(")");
        }
        builder.append(" FOR ");
        if (this.forColumns.size() == 1) {
            builder.append(this.forColumns.get(0));
        } else {
            builder.append("(")
                    .append(this.forColumns.stream()
                            .map(ColumnReference::toString)
                            .collect(Collectors.joining(",")))
                    .append(")");
        }
        builder.append(" IN(").append(this.inItems.stream()
                .map(InItem::toString)
                .collect(Collectors.joining(",")))
                .append(")").append(")");
        if (this.alias != null) {
            builder.append(" ").append(this.alias);
        }
        return builder.toString();
    }

    @Getter
    @EqualsAndHashCode(callSuper = false)
    public static class InItem extends BaseStatement {

        private final Expression as;
        private final List<ColumnReference> columns;

        public InItem(@NonNull ParserRuleContext context, @NonNull List<ColumnReference> columns, Expression as) {
            super(context);
            this.as = as;
            this.columns = columns;
        }

        public InItem(@NonNull List<ColumnReference> columns, Expression as) {
            this.as = as;
            this.columns = columns;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            if (this.columns.size() > 1) {
                builder.append("(");
            }
            builder.append(this.columns.stream()
                    .map(ColumnReference::toString)
                    .collect(Collectors.joining(",")));
            if (this.columns.size() > 1) {
                builder.append(")");
            }
            if (this.as != null) {
                builder.append(" AS ").append(this.as.toString());
            }
            return builder.toString();
        }
    }

}
