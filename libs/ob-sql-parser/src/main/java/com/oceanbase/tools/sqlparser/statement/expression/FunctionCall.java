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
package com.oceanbase.tools.sqlparser.statement.expression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.common.WindowSpec;
import com.oceanbase.tools.sqlparser.statement.common.oracle.KeepClause;
import com.oceanbase.tools.sqlparser.statement.select.OrderBy;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link FunctionCall}
 *
 * @author yh263208
 * @date 2022-11-25 17:38
 * @since ODC_release_4.1.0
 * @see BaseStatement
 */
@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
public class FunctionCall extends BaseExpression {

    private static final Set<String> AGGREGATORS = new HashSet<>();

    static {
        AGGREGATORS.add("ALL");
        AGGREGATORS.add("DISTINCT");
        AGGREGATORS.add("UNIQUE");
    }

    private final String functionName;
    private final List<FunctionParam> paramList;
    private final List<Statement> options = new ArrayList<>();
    private KeepClause keep;
    private WindowSpec window;
    private OrderBy withinGroup;

    public FunctionCall(@NonNull ParserRuleContext context,
            @NonNull String functionName,
            @NonNull List<FunctionParam> functionParams) {
        super(context);
        this.functionName = functionName;
        this.paramList = functionParams;
    }

    public FunctionCall(@NonNull String functionName,
            @NonNull List<FunctionParam> functionParams) {
        this.functionName = functionName;
        this.paramList = functionParams;
    }

    public void addOption(Statement statement) {
        if (statement == null) {
            return;
        }
        this.options.add(statement);
    }

    public String getAggregator() {
        if (CollectionUtils.isEmpty(this.options)) {
            return null;
        }
        return this.options.stream().filter(s -> {
            if (!(s instanceof ConstExpression)) {
                return false;
            }
            String conS = ((ConstExpression) s).getExprConst();
            return AGGREGATORS.contains(conS.toUpperCase());
        }).map(s -> ((ConstExpression) s).getExprConst()).findFirst().orElse(null);
    }

    @Override
    public String doToString() {
        StringBuilder builder = new StringBuilder(this.functionName);
        builder.append("(");
        if (getAggregator() != null) {
            builder.append(getAggregator()).append(" ");
        }
        builder.append(paramList.stream().map(Object::toString)
                .collect(Collectors.joining(","))).append(")");
        if (this.keep != null) {
            builder.append(" KEEP (").append(this.keep).append(")");
        }
        if (this.withinGroup != null) {
            builder.append(" WITHIN GROUP (").append(this.withinGroup).append(")");
        }
        if (this.window != null) {
            builder.append(" OVER (").append(this.window.toString()).append(")");
        }
        return builder.toString();
    }

}
