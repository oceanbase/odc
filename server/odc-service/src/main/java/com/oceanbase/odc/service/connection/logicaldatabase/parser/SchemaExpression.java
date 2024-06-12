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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.collections.CollectionUtils;

import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.common.util.ListUtils;
import com.oceanbase.odc.core.shared.PreConditions;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: Lebie
 * @Date: 2024/4/22 11:04
 * @Description: []
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaExpression extends BaseLogicalTableExpression {
    protected List<BaseRangeExpression> sliceRanges;

    SchemaExpression(ParserRuleContext ruleNode) {
        super(ruleNode);
    }

    @Override
    public List<String> evaluate() throws BadExpressionException {
        PreConditions.notEmpty(this.getText(), "expression");

        if (CollectionUtils.isEmpty(sliceRanges)) {
            return Arrays.asList(this.getText());
        }

        List<Pair<Integer, Integer>> rangeIndexes =
                sliceRanges.stream()
                        .map(stmt -> new Pair<>(stmt.getStart() - this.getStart(), stmt.getStop() - this.getStart()))
                        .collect(
                                Collectors.toList());
        List<List<String>> ranges = ListUtils.cartesianProduct(sliceRanges.stream().map(BaseRangeExpression::listRanges)
                .collect(Collectors.toList()));
        List<String> names = new ArrayList<>();
        /**
         * we need to iterate in reverse order to replace the ranges from right to left; otherwise, we may
         * lose the correct indexes of the original expression!
         */
        for (int i = ranges.size() - 1; i >= 0; i--) {
            List<String> range = ranges.get(i);
            StringBuilder sb = new StringBuilder(this.getText());
            for (int j = range.size() - 1; j >= 0; j--) {
                Pair<Integer, Integer> rangeIndex = rangeIndexes.get(j);
                sb.replace(rangeIndex.left, rangeIndex.right + 1, range.get(j));
            }
            names.add(sb.toString());
        }
        Collections.reverse(names);
        return names;
    }
}
