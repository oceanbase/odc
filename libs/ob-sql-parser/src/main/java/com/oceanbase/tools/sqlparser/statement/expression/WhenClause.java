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

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * @author gaoda.xy
 * @date 2023/6/26 14:49
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class WhenClause extends BaseStatement implements Expression {

    private final Expression when;
    private final Expression then;

    public WhenClause(@NonNull ParserRuleContext context, @NonNull Expression when, @NonNull Expression then) {
        super(context);
        this.when = when;
        this.then = then;
    }

    public WhenClause(@NonNull Expression when, @NonNull Expression then) {
        this.when = when;
        this.then = then;
    }

    @Override
    public String toString() {
        return "WHEN " + this.when + " THEN " + this.then;
    }

}
