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
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.tree.TerminalNode;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalTableExpressionBaseVisitor;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalTableExpressionParser;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalTableExpressionParser.ConsecutiveRangeContext;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalTableExpressionParser.EnumRangeContext;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalTableExpressionParser.LogicalTableExpressionContext;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalTableExpressionParser.SchemaExpressionContext;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalTableExpressionParser.SchemaSliceRangeWithBracketContext;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalTableExpressionParser.SliceRangeContext;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalTableExpressionParser.SliceRangeWithDoubleBracketContext;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalTableExpressionParser.SliceRangeWithSingeBracketContext;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalTableExpressionParser.SteppedRangeContext;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalTableExpressionParser.TableExpressionContext;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalTableExpressionParser.TableSliceRangeWithBracketContext;
import com.oceanbase.tools.sqlparser.statement.BaseStatement;

/**
 * @Author: Lebie
 * @Date: 2024/4/22 10:04
 * @Description: []
 */
public class LogicalTableExpressionVisitor extends LogicalTableExpressionBaseVisitor<BaseStatement> {
    @Override
    public BaseStatement visitLogicalTableExpressionList(
            LogicalTableExpressionParser.LogicalTableExpressionListContext ctx) {
        PreConditions.notEmpty(ctx.logicalTableExpression(), "logicalTableExpression");
        LogicalTableExpressions expressions = new LogicalTableExpressions(ctx);
        List<LogicalTableExpression> expressionList = new ArrayList<>();
        for (LogicalTableExpressionContext expression : ctx.logicalTableExpression()) {
            expressionList.add((LogicalTableExpression) visitLogicalTableExpression(expression));
        }
        expressions.setExpressions(expressionList);
        return expressions;
    }

    @Override
    public BaseStatement visitLogicalTableExpression(LogicalTableExpressionContext ctx) {
        PreConditions.notNull(ctx.schemaExpression(), "schemaExpression");
        PreConditions.notNull(ctx.tableExpression(), "tableExpression");
        LogicalTableExpression expression = new LogicalTableExpression(ctx);
        expression.setSchemaExpression((SchemaExpression) visitSchemaExpression(ctx.schemaExpression()));
        expression.setTableExpression((TableExpression) visitTableExpression(ctx.tableExpression()));
        return expression;
    }

    @Override
    public BaseStatement visitSchemaExpression(SchemaExpressionContext ctx) {
        PreConditions.notEmpty(ctx.IDENTIFIER(), "schemaExpression.IDENTIFIER");
        SchemaExpression expression = new SchemaExpression(ctx);
        if (ctx.schemaSliceRangeWithBracket() != null) {
            List<BaseRangeExpression> ranges = new ArrayList<>();
            for (SchemaSliceRangeWithBracketContext sliceRange : ctx.schemaSliceRangeWithBracket()) {
                ranges.add((BaseRangeExpression) visitSchemaSliceRangeWithBracket(sliceRange));
            }
            expression.setSliceRanges(ranges);
        }
        return expression;
    }

    @Override
    public BaseStatement visitTableExpression(TableExpressionContext ctx) {
        PreConditions.notEmpty(ctx.IDENTIFIER(), "tableExpression.IDENTIFIER");
        TableExpression expression = new TableExpression(ctx);
        if (ctx.tableSliceRangeWithBracket() != null) {
            List<BaseRangeExpression> ranges = new ArrayList<>();
            for (TableSliceRangeWithBracketContext sliceRange : ctx.tableSliceRangeWithBracket()) {
                if (sliceRange.sliceRangeWithDoubleBracket() != null) {
                    expression.setRepeat(true);
                }
                ranges.add((BaseRangeExpression) visitTableSliceRangeWithBracket(sliceRange));
            }
            expression.setSliceRanges(ranges);
        }
        return expression;
    }

    @Override
    public BaseStatement visitSchemaSliceRangeWithBracket(SchemaSliceRangeWithBracketContext ctx) {
        if (ctx.sliceRangeWithSingeBracket() != null) {
            return visitSliceRangeWithSingeBracket(ctx.sliceRangeWithSingeBracket());
        }
        return null;
    }

    @Override
    public BaseStatement visitTableSliceRangeWithBracket(TableSliceRangeWithBracketContext ctx) {
        if (ctx.sliceRangeWithSingeBracket() != null) {
            return visitSliceRangeWithSingeBracket(ctx.sliceRangeWithSingeBracket());
        } else if (ctx.sliceRangeWithDoubleBracket() != null) {
            return visitSliceRangeWithDoubleBracket(ctx.sliceRangeWithDoubleBracket());
        } else {
            return null;
        }
    }

    @Override
    public BaseStatement visitSliceRangeWithSingeBracket(SliceRangeWithSingeBracketContext ctx) {
        if (ctx.sliceRange() != null) {
            return visitSliceRange(ctx.sliceRange());
        }
        return null;
    }

    @Override
    public BaseStatement visitSliceRangeWithDoubleBracket(SliceRangeWithDoubleBracketContext ctx) {
        if (ctx.sliceRange() != null) {
            return visitSliceRange(ctx.sliceRange());
        }
        return null;
    }

    @Override
    public BaseStatement visitSliceRange(SliceRangeContext ctx) {
        BaseRangeExpression range = null;
        if (ctx.consecutiveRange() != null) {
            range = (BaseRangeExpression) visitConsecutiveRange(ctx.consecutiveRange());
        } else if (ctx.steppedRange() != null) {
            range = (BaseRangeExpression) visitSteppedRange(ctx.steppedRange());
        } else if (ctx.enumRange() != null) {
            range = (BaseRangeExpression) visitEnumRange(ctx.enumRange());
        }
        return range;
    }

    @Override
    public BaseStatement visitConsecutiveRange(ConsecutiveRangeContext ctx) {
        List<TerminalNode> numbers = ctx.NUMBER();
        Verify.equals(2, numbers.size(), "consecutiveRange.NUMBER");
        return new ConsecutiveSliceRange(ctx.getParent().getParent(), numbers.get(0).getText(),
                numbers.get(1).getText());
    }

    @Override
    public BaseStatement visitSteppedRange(SteppedRangeContext ctx) {
        List<TerminalNode> numbers = ctx.NUMBER();
        Verify.equals(3, numbers.size(), "steppedRange.NUMBER");
        return new SteppedRange(ctx.getParent().getParent(), numbers.get(0).getText(),
                numbers.get(1).getText(), numbers.get(2).getText());
    }

    @Override
    public BaseStatement visitEnumRange(EnumRangeContext ctx) {
        List<TerminalNode> numbers = ctx.NUMBER();
        Verify.notEmpty(numbers, "enumRange.NUMBER");
        return new EnumRange(ctx.getParent().getParent(),
                numbers.stream().map(node -> node.getText()).collect(Collectors.toList()));
    }
}
