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

import com.oceanbase.tools.sqlparser.statement.select.OrderBy;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link GroupConcat}
 *
 * @author yh263208
 * @date 2022-12-09 17:39
 * @since ODC_release_4.1.0
 * @see FunctionCall
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class GroupConcat extends WindowFunction {

    private OrderBy orderBy;
    private String separator;

    public GroupConcat(@NonNull ParserRuleContext context,
            @NonNull List<FunctionParam> functionParams) {
        super(context, "GROUP_CONCAT", functionParams);
    }

    public GroupConcat(@NonNull List<FunctionParam> functionParams) {
        super("GROUP_CONCAT", functionParams);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getFunctionName());
        builder.append("(");
        if (getParamsFlag() != null) {
            builder.append(getParamsFlag()).append(" ");
        }
        builder.append(getParamList().stream().map(Object::toString).collect(Collectors.joining(",")));
        if (this.orderBy != null) {
            builder.append(" ").append(this.orderBy.toString());
        }
        if (this.separator != null) {
            builder.append(" SEPARATOR ").append(this.separator);
        }
        builder.append(")");
        if (getWindow() != null) {
            builder.append(" OVER ").append(getWindow().toString());
        }
        return builder.toString();
    }

}
