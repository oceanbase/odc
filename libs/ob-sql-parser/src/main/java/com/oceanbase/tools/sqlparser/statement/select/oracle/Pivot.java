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
import org.apache.commons.lang3.StringUtils;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.FunctionCall;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link Pivot}
 *
 * @author yh263208
 * @date 2023-03-01 10:51
 * @since ODC_release_4.1.2
 * @see BaseStatement
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class Pivot extends BaseStatement {

    private final List<FunctionItem> functionItems;
    private final List<ColumnReference> forColumns;
    private final List<ExpressionItem> inItems;
    @Setter
    private String alias;

    public Pivot(@NonNull ParserRuleContext context,
            @NonNull List<FunctionItem> functionItems,
            @NonNull List<ColumnReference> forColumns,
            @NonNull List<ExpressionItem> inItems) {
        super(context);
        this.forColumns = forColumns;
        this.inItems = inItems;
        this.functionItems = functionItems;
    }

    public Pivot(@NonNull List<FunctionItem> functionItems,
            @NonNull List<ColumnReference> forColumns,
            @NonNull List<ExpressionItem> inItems) {
        this.forColumns = forColumns;
        this.inItems = inItems;
        this.functionItems = functionItems;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("PIVOT(");
        builder.append(this.functionItems.stream()
                .map(FunctionItem::toString)
                .collect(Collectors.joining(","))).append(" FOR");
        if (this.forColumns.size() == 1) {
            builder.append(" ").append(this.forColumns.get(0));
        } else {
            builder.append("(").append(this.forColumns.stream()
                    .map(ColumnReference::toString)
                    .collect(Collectors.joining(","))).append(")");
        }
        builder.append(" IN(").append(this.inItems.stream()
                .map(ExpressionItem::toString)
                .collect(Collectors.joining(","))).append(")").append(")");
        if (StringUtils.isNotBlank(this.alias)) {
            builder.append(" ").append(this.alias);
        }
        return builder.toString();
    }

    @Getter
    @EqualsAndHashCode(callSuper = false)
    public static class FunctionItem extends BaseStatement {

        private final String alias;
        private final FunctionCall functionCall;

        public FunctionItem(@NonNull ParserRuleContext context, @NonNull FunctionCall functionCall, String alias) {
            super(context);
            this.functionCall = functionCall;
            this.alias = alias;
        }

        public FunctionItem(@NonNull FunctionCall functionCall, String alias) {
            this.alias = alias;
            this.functionCall = functionCall;
        }

        @Override
        public String toString() {
            if (StringUtils.isBlank(this.alias)) {
                return this.functionCall.toString();
            }
            return this.functionCall.toString() + " AS " + this.alias;
        }
    }

    @Getter
    @EqualsAndHashCode(callSuper = false)
    public static class ExpressionItem extends BaseStatement {

        private final String alias;
        private final Expression expression;

        public ExpressionItem(@NonNull ParserRuleContext context, @NonNull Expression expression, String alias) {
            super(context);
            this.expression = expression;
            this.alias = alias;
        }

        public ExpressionItem(@NonNull Expression expression, String alias) {
            this.alias = alias;
            this.expression = expression;
        }

        @Override
        public String toString() {
            if (StringUtils.isBlank(this.alias)) {
                return this.expression.toString();
            }
            return this.expression.toString() + " AS " + this.alias;
        }
    }

}
