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

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Operator;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link CompoundExpression}
 *
 * @author yh263208
 * @date 2022-11-25 12:00
 * @since ODC_release_4.1.0
 * @see Expression
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class CompoundExpression extends BaseExpression {

    private final Expression left;
    private final Expression right;
    private final Operator operator;

    public CompoundExpression(@NonNull ParserRuleContext context,
            @NonNull Expression left, Expression right, Operator operator) {
        super(context);
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    public CompoundExpression(@NonNull Expression left,
            Expression right, Operator operator) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    @Override
    public String doToString() {
        if (this.right == null) {
            return this.operator.getText()[0] + " " + this.left.toString();
        }
        return this.left.toString() + " " + this.operator.getText()[0] + " " + this.right.toString();
    }

}
