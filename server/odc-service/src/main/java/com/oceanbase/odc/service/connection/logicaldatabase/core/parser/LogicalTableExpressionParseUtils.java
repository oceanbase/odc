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
package com.oceanbase.odc.service.connection.logicaldatabase.core.parser;

import static com.oceanbase.odc.core.shared.constant.ErrorCodes.LogicalTableExpressionNotPositiveStep;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.DataNode;
import com.oceanbase.tools.sqlparser.SyntaxErrorException;

/**
 * @Author: Lebie
 * @Date: 2024/4/22 15:40
 * @Description: []
 */
public class LogicalTableExpressionParseUtils {
    private static final DefaultLogicalTableExpressionParser parser = new DefaultLogicalTableExpressionParser();

    public static List<DataNode> resolve(String expression) {
        PreConditions.notEmpty(expression, "expression");
        LogicalTableExpressions logicalTableExpression;
        try {
            logicalTableExpression = (LogicalTableExpressions) parser.parse(new StringReader(expression));
        } catch (SyntaxErrorException e) {
            throw new BadLogicalTableExpressionException(e);
        } catch (Exception e) {
            throw new UnexpectedException("failed to parse logical table expression", e);
        }
        return logicalTableExpression.evaluate().stream().map(name -> {
            String[] parts = name.split("\\.");
            if (parts.length != 2) {
                throw new UnexpectedException("invalid logical table expression");
            }
            return new DataNode(parts[0], parts[1]);
        }).collect(Collectors.toList());
    }


    public static List<String> listSteppedRanges(String start, String end, String step, String text)
            throws BadLogicalTableExpressionException {
        PreConditions.notEmpty(start, "start");
        PreConditions.notEmpty(end, "end");
        PreConditions.notEmpty(step, "step");

        int startInt, endInt, stepInt;
        try {
            startInt = Integer.parseInt(start);
            endInt = Integer.parseInt(end);
            stepInt = Integer.parseInt(step);
        } catch (NumberFormatException e) {
            throw new BadLogicalTableExpressionException(ErrorCodes.LogicalTableExpressionNotValidIntegerRange,
                    new Object[] {text},
                    ErrorCodes.LogicalTableExpressionNotValidIntegerRange.getEnglishMessage(new Object[] {text}));
        }

        if (stepInt <= 0) {
            throw new BadLogicalTableExpressionException(LogicalTableExpressionNotPositiveStep,
                    new Object[] {text, stepInt},
                    LogicalTableExpressionNotPositiveStep.getEnglishMessage(new Object[] {text, stepInt}));
        }
        if (startInt > endInt) {
            throw new BadLogicalTableExpressionException(ErrorCodes.LogicalTableExpressionRangeStartGreaterThanEnd,
                    new Object[] {text, startInt, endInt},
                    ErrorCodes.LogicalTableExpressionRangeStartGreaterThanEnd
                            .getEnglishMessage(new Object[] {text, startInt, endInt}));
        }

        boolean includeLeadingZeros = start.startsWith("0") && start.length() > 1;
        int maxLength = includeLeadingZeros ? start.length() : 0;

        List<String> result = new ArrayList<>();
        for (int i = startInt; i <= endInt; i += stepInt) {
            String format = includeLeadingZeros ? "%0" + maxLength + "d" : "%d";
            result.add(String.format(format, i));
        }

        return result;
    }
}
