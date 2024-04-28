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

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.odc.core.shared.constant.ErrorCodes;

import lombok.Getter;

/**
 * @Author: Lebie
 * @Date: 2024/4/22 13:31
 * @Description: []
 */
@Getter
public class EnumRange extends BaseRangeExpression {
    private List<String> enumValues;

    EnumRange(ParserRuleContext ruleNode, List<String> enumValues) {
        super(ruleNode);
        this.enumValues = enumValues;
    }

    @Override
    public List<String> listRanges() throws BadExpressionException {
        enumValues.stream().forEach(value -> {
            try {
                Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new BadExpressionException(ErrorCodes.LogicalTableExpressionNotValidIntegerRange,
                        new Object[] {this.getText()},
                        ErrorCodes.LogicalTableExpressionNotValidIntegerRange
                                .getEnglishMessage(new Object[] {this.getText()}));
            }
        });
        return this.enumValues;
    }
}
