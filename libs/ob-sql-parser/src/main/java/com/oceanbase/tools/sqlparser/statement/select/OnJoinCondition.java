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

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link OnJoinCondition}
 *
 * @author yh263208
 * @date 2022-11-25 14:56
 * @since ODC_release_4.1.0
 * @see JoinCondition
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class OnJoinCondition extends BaseStatement implements JoinCondition {

    private final Expression expression;

    public OnJoinCondition(@NonNull ParserRuleContext context, @NonNull Expression expression) {
        super(context);
        this.expression = expression;
    }

    public OnJoinCondition(@NonNull Expression expression) {
        this.expression = expression;
    }

    @Override
    public int getConditionType() {
        return JoinCondition.ON;
    }

    @Override
    public String toString() {
        return "ON " + this.expression.toString();
    }

}
