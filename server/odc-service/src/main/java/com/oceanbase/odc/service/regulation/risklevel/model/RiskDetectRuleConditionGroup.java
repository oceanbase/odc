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

import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2023/8/4 16:05
 * @Description: []
 */
@Data
@NoArgsConstructor
@JsonTypeName("CONDITION_GROUP")
@Validated
public class RiskDetectRuleConditionGroup extends BaseTreeNode {
    private final NodeType type = NodeType.CONDITION_GROUP;

    @NotNull
    private BooleanOperator booleanOperator;

    @NotEmpty
    private List<BaseTreeNode> children;

    @Override
    public boolean evaluate(@NonNull RiskLevelDescriber describer) {
        boolean validated;
        if (booleanOperator == BooleanOperator.AND) {
            validated = true;
            for (BaseTreeNode node : children) {
                validated = validated && node.evaluate(describer);
            }
        } else {
            validated = false;
            for (BaseTreeNode node : children) {
                validated = validated || node.evaluate(describer);
            }
        }
        return validated;
    }
}
