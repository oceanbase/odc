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
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * {@link CollectionExpression}
 *
 * @author yh263208
 * @date 2022-12-03 20:45
 * @since ODC_release_4.1.0
 * @see BaseStatement
 */
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CollectionExpression extends BaseStatement implements Expression {

    private final List<Expression> expressionList = new ArrayList<>();

    public CollectionExpression(ParserRuleContext context) {
        this(context, null);
    }

    public CollectionExpression(ParserRuleContext context, List<Expression> expressions) {
        super((context));
        if (CollectionUtils.isNotEmpty(expressions)) {
            this.expressionList.addAll(expressions);
        }
    }

    public void addExpression(@NonNull Expression expression) {
        this.expressionList.add(expression);
    }

    @Override
    public String toString() {
        return "(" + this.expressionList.stream().map(Object::toString).collect(Collectors.joining(",")) + ")";
    }

}
