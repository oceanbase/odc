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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link IntervalExpression}
 *
 * @author yh263208
 * @date 2022-12-11 16:33
 * @since ODC_release_4.1.0
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class IntervalExpression extends BaseExpression {

    private final String dateUnit;
    private final Expression target;

    public IntervalExpression(@NonNull ParserRuleContext context,
            @NonNull Expression target, @NonNull String dateUnit) {
        super(context);
        this.target = target;
        this.dateUnit = dateUnit;
    }

    public IntervalExpression(@NonNull Expression target, @NonNull String dateUnit) {
        this.target = target;
        this.dateUnit = dateUnit;
    }

    @Override
    public String doToString() {
        return "INTERVAL " + this.target.toString() + " " + this.dateUnit;
    }

}
