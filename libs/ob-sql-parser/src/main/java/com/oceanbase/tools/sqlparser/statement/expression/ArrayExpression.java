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

import com.oceanbase.tools.sqlparser.statement.Expression;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2024/10/14 21:00
 * @Description: []
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class ArrayExpression extends BaseExpression {

    private final List<Expression> expressions;

    public ArrayExpression(@NonNull ParserRuleContext context, @NonNull List<Expression> expressions) {
        super(context);
        this.expressions = expressions;
    }

    public ArrayExpression(@NonNull List<Expression> expressions) {
        this.expressions = expressions;
    }

    @Override
    protected String doToString() {
        return "[" + this.expressions.stream().map(Object::toString).collect(Collectors.joining(",")) + "]";
    }

}
