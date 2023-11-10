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

import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.StringUtils;

import com.oceanbase.tools.sqlparser.statement.select.OrderBy;

import lombok.EqualsAndHashCode;
import lombok.NonNull;

/**
 * {@link GroupConcat}
 *
 * @author yh263208
 * @date 2022-12-09 17:39
 * @since ODC_release_4.1.0
 * @see FunctionCall
 */
@EqualsAndHashCode(callSuper = true)
public class GroupConcat extends FunctionCall {

    private static final String SEPARATOR_KEY = "SEPARATOR";

    public GroupConcat(@NonNull ParserRuleContext context,
            @NonNull List<FunctionParam> functionParams) {
        super(context, "GROUP_CONCAT", functionParams);
    }

    public GroupConcat(@NonNull List<FunctionParam> functionParams) {
        super("GROUP_CONCAT", functionParams);
    }

    public String getSeparator() {
        return getOptions().stream().filter(s -> {
            if (!(s instanceof ConstExpression)) {
                return false;
            }
            ConstExpression c = (ConstExpression) s;
            return StringUtils.startsWithIgnoreCase(c.getExprConst(), SEPARATOR_KEY);
        }).map(s -> {
            String c = ((ConstExpression) s).getExprConst();
            int begin = StringUtils.indexOfIgnoreCase(c, SEPARATOR_KEY) + SEPARATOR_KEY.length();
            return c.substring(begin).trim();
        }).findFirst().orElse(null);
    }

    public OrderBy getOrderBy() {
        return getOptions().stream().filter(s -> s instanceof OrderBy)
                .map(s -> (OrderBy) s).findFirst().orElse(null);
    }

    @Override
    public String doToString() {
        StringBuilder builder = new StringBuilder(getFunctionName());
        builder.append("(");
        if (getAggregator() != null) {
            builder.append(getAggregator()).append(" ");
        }
        builder.append(getParamList().stream().map(Object::toString).collect(Collectors.joining(",")));
        if (getOrderBy() != null) {
            builder.append(" ").append(getOrderBy().toString());
        }
        if (getSeparator() != null) {
            builder.append(" ")
                    .append(SEPARATOR_KEY).append(" ")
                    .append(getSeparator());
        }
        builder.append(")");
        if (getWindow() != null) {
            builder.append(" OVER ").append(getWindow().toString());
        }
        return builder.toString();
    }

}
