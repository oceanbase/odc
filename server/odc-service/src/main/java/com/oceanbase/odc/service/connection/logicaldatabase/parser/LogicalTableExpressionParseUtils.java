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
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;

/**
 * @Author: Lebie
 * @Date: 2024/4/22 15:40
 * @Description: []
 */
public class LogicalTableExpressionParseUtils {
    public static List<String> listSteppedRanges(String start, String end, String step) {
        PreConditions.notEmpty(start, "start");
        PreConditions.notEmpty(end, "end");
        PreConditions.notEmpty(step, "step");

        int startInt, endInt, stepInt;
        try {
            startInt = Integer.parseInt(start);
            endInt = Integer.parseInt(end);
            stepInt = Integer.parseInt(step);
        } catch (NumberFormatException e) {
            throw new BadArgumentException("Input strings must be valid integers", e);
        }

        Verify.greaterThan(stepInt, 0, "step");
        Verify.lessThan(startInt, endInt, "start");

        boolean includeLeadingZeros = start.length() > String.valueOf(startInt).length();

        int leadingZerosCount = includeLeadingZeros ? start.length() - String.valueOf(startInt).length() : 0;

        List<String> result = new ArrayList<>();
        for (int i = startInt; i <= endInt; i++) {
            String format = includeLeadingZeros ? "%0" + (leadingZerosCount + String.valueOf(i).length()) + "d" : "%d";
            result.add(String.format(format, i));
        }

        return result;
    }

    public static List<String> listNames(String expression, int offset, List<BaseRangeExpression> sliceRanges) {
        PreConditions.notEmpty(expression, "expression");

        if (CollectionUtils.isEmpty(sliceRanges)) {
            return Arrays.asList(expression);
        }

        Verify.notGreaterThan(sliceRanges.size(), 2, "range expression");

        List<Pair<Integer, Integer>> rangeIndexes =
                sliceRanges.stream().map(stmt -> new Pair<>(stmt.getStart() - offset, stmt.getStop() - offset)).collect(
                        Collectors.toList());
        List<List<String>> ranges = LogicalTableExpressionParseUtils
                .cartesianProduct(sliceRanges.stream().map(BaseRangeExpression::listRanges)
                        .collect(Collectors.toList()));
        List<String> names = new ArrayList<>();
        /**
         * we need to iterate in reverse order to replace the ranges from right to left; otherwise, we may
         * lose the correct indexes of the original expression!
         */
        for (int i = ranges.size() - 1; i >= 0; i--) {
            List<String> range = ranges.get(i);
            StringBuilder sb = new StringBuilder(expression);
            for (int j = range.size() - 1; j >= 0; j--) {
                Pair<Integer, Integer> rangeIndex = rangeIndexes.get(j);
                sb.replace(rangeIndex.left, rangeIndex.right + 1, range.get(j));
            }
            names.add(sb.toString());
        }
        return names;
    }

    private static List<List<String>> cartesianProduct(List<List<String>> lists) {
        List<List<String>> result = new ArrayList<>();
        cartesianProductRecursive(lists, result, 0, new ArrayList<>());
        return result;
    }

    private static void cartesianProductRecursive(List<List<String>> lists, List<List<String>> result, int depth,
            List<String> current) {
        if (depth == lists.size()) {
            result.add(new ArrayList<>(current));
            return;
        }

        for (String s : lists.get(depth)) {
            List<String> newCurrent = new ArrayList<>(current);
            newCurrent.add(s);
            cartesianProductRecursive(lists, result, depth + 1, newCurrent);
        }
    }
}
