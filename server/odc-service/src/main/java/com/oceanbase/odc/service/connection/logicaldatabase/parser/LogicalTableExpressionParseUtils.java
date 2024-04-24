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

import static com.oceanbase.odc.core.shared.constant.ErrorCodes.NotPositiveLogicalTableExpressionStep;

import java.util.ArrayList;
import java.util.List;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.service.connection.logicaldatabase.BadExpressionException;

/**
 * @Author: Lebie
 * @Date: 2024/4/22 15:40
 * @Description: []
 */
public class LogicalTableExpressionParseUtils {
    public static List<String> listSteppedRanges(String start, String end, String step, String text)
            throws BadExpressionException {
        PreConditions.notEmpty(start, "start");
        PreConditions.notEmpty(end, "end");
        PreConditions.notEmpty(step, "step");

        int startInt, endInt, stepInt;
        try {
            startInt = Integer.parseInt(start);
            endInt = Integer.parseInt(end);
            stepInt = Integer.parseInt(step);
        } catch (NumberFormatException e) {
            throw new BadExpressionException(ErrorCodes.NotValidIntegerRangeInLogicalTableExpression,
                    new Object[] {text},
                    ErrorCodes.NotValidIntegerRangeInLogicalTableExpression.getEnglishMessage(new Object[] {text}));
        }

        if (stepInt <= 0) {
            throw new BadExpressionException(NotPositiveLogicalTableExpressionStep, new Object[] {text, stepInt},
                    NotPositiveLogicalTableExpressionStep.getEnglishMessage(new Object[] {text, stepInt}));
        }
        if (startInt > endInt) {
            throw new BadExpressionException(ErrorCodes.RangeStartGreaterThanEndInLogicalTableExpression,
                    new Object[] {text, startInt, endInt},
                    ErrorCodes.RangeStartGreaterThanEndInLogicalTableExpression
                            .getEnglishMessage(new Object[] {text, startInt, endInt}));
        }

        boolean includeLeadingZeros = start.length() > String.valueOf(startInt).length();

        int leadingZerosCount = includeLeadingZeros ? start.length() - String.valueOf(startInt).length() : 0;

        List<String> result = new ArrayList<>();
        for (int i = startInt; i <= endInt; i += stepInt) {
            String format = includeLeadingZeros ? "%0" + (leadingZerosCount + String.valueOf(i).length()) + "d" : "%d";
            result.add(String.format(format, i));
        }

        return result;
    }

    public static List<List<String>> cartesianProduct(List<List<String>> lists) {
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
