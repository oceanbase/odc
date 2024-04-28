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
package com.oceanbase.odc.service.connection.logicaldatabase.parser;

import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: Lebie
 * @Date: 2024/4/23 19:38
 * @Description: []
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogicalTableExpressions extends BaseLogicalTableExpression {
    private List<LogicalTableExpression> expressions;

    LogicalTableExpressions(ParserRuleContext ruleNode) {
        super(ruleNode);
    }

    @Override
    public List<String> evaluate() throws BadExpressionException {
        return this.expressions.stream().flatMap(expression -> expression.evaluate().stream())
                .collect(Collectors.toList());
    }
}
