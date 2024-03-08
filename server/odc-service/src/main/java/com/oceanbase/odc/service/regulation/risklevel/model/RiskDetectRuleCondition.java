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
package com.oceanbase.odc.service.regulation.risklevel.model;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.oceanbase.odc.common.i18n.Internationalizable;
import com.oceanbase.odc.service.regulation.risklevel.operator.OperatorFactory;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2023/5/11 14:34
 * @Description: [Represent the RiskLevel Rule Condition. For example, if expression is Environment,
 *               operation is == and value is PROD, then it indicates the condition where
 *               environment is PROD.]
 */
@Data
@NoArgsConstructor
@JsonTypeName("CONDITION")
@Validated
public class RiskDetectRuleCondition extends BaseTreeNode {
    private final NodeType type = NodeType.CONDITION;

    /**
     * condition expression, e.g., ProjectName, EnvironmentId, TaskType, SqlCheckResult, etc.
     */
    @NotEmpty
    @Valid
    private ConditionExpression expression;

    /**
     * condition operation, e.g., equals, contains, matches, etc.
     */
    @NotEmpty
    @Valid
    private String operator;

    /**
     * condition value, any string, e.g., PROD, IMPORT
     */
    @NotEmpty
    @Valid
    private Object value;

    @Override
    public boolean evaluate(@NonNull RiskLevelDescriber describer) {
        return OperatorFactory.createOperator(this.operator).evaluate(describer.describe(expression), value);
    }

    @Override
    public boolean find(@NotNull String key, Object value) {
        return StringUtils.equals(key, this.expression.name())
                && StringUtils.equals(String.valueOf(value), String.valueOf(this.value));
    }
}
