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
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Statement;

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
@Getter
@EqualsAndHashCode(callSuper = false)
public class FunctionCall extends BaseStatement implements Expression {

    private final String functionName;
    private final List<Statement> paramsOptions = new ArrayList<>();
    private final List<FunctionParam> paramList;
    @Setter
    private String paramsFlag;

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

    public void addParamsOption(@NonNull Statement paramsOpt) {
        this.paramsOptions.add(paramsOpt);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.functionName);
        builder.append("(");
        if (this.paramsFlag != null) {
            builder.append(this.paramsFlag).append(" ");
        }
        builder.append(paramList.stream().map(Object::toString).collect(Collectors.joining(",")))
                .append(")");
        return builder.toString();
    }

}
