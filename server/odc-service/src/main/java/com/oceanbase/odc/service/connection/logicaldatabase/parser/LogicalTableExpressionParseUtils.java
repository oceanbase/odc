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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import com.oceanbase.odc.service.connection.logicaldatabase.LogicalTableExpressionLexer;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalTableExpressionParser;

/**
 * @Author: Lebie
 * @Date: 2024/4/22 15:40
 * @Description: []
 */
public class LogicalTableExpressionParseUtils {
    public static void main(String[] args) {
        String input = "db_[0-1].tb_[[1-4]]";
        LogicalTableExpressionLexer lexer = new LogicalTableExpressionLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LogicalTableExpressionParser parser = new LogicalTableExpressionParser(tokens);

        LogicalTableExpressionVisitor visitor = new LogicalTableExpressionVisitor();
        LogicalTableExpression expression = (LogicalTableExpression) visitor.visit(parser.logicalTableExpression());
        System.out.println(expression.getSchemaExpression());
        System.out.println(expression.getTableExpression());
        List<BaseRangeExpression> ranges = expression.getTableExpression().getSliceRanges();
        for (BaseRangeExpression range : ranges) {
            if (range instanceof ConsecutiveSliceRange) {
                ConsecutiveSliceRange consecutiveSliceRange = (ConsecutiveSliceRange) range;
                System.out.println(consecutiveSliceRange.getRangeStart());
                System.out.println(consecutiveSliceRange.getRangeEnd());
                System.out.println(consecutiveSliceRange.getText());
                System.out.println(consecutiveSliceRange.getStart());
                System.out.println(consecutiveSliceRange.getStop());
            }
        }
    }
}
