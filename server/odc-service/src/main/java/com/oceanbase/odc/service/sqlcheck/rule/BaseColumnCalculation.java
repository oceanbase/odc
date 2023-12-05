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
package com.oceanbase.odc.service.sqlcheck.rule;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;

import lombok.NonNull;

/**
 * {@link BaseColumnCalculation}
 *
 * @author yh263208
 * @date 2022-12-26 17:16
 * @since ODC_release_4.1.9
 */
abstract class BaseColumnCalculation implements SqlCheckRule {

    private static final Set<Operator> BOOL_OPERATOR = new HashSet<>(Arrays.asList(
            Operator.EQ, Operator.GE, Operator.LE, Operator.GT, Operator.LT,
            Operator.NE_PL, Operator.NE, Operator.CNNOP, Operator.IN, Operator.NOT_IN,
            Operator.BETWEEN, Operator.NOT_BETWEEN, Operator.LIKE, Operator.NOT_LIKE));

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        return getCalculatedReferences(SqlCheckUtil.getWhereClauses(statement)).stream()
                .map(s -> SqlCheckUtil.buildViolation(statement.getText(), s, getType(),
                        context.getStatementOffset(statement), null))
                .collect(Collectors.toList());
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.INDEX_COLUMN_CALCULATION;
    }

    protected List<Expression> getCalculatedReferences(List<Expression> wheres) {
        return wheres.stream().flatMap(e -> SqlCheckUtil.findAll(e, e1 -> {
            // 获取左值为复合表达式的算数布尔运算集合，例子：1+1<4 或 id+2=4
            if (!(e1 instanceof CompoundExpression)) {
                return false;
            }
            CompoundExpression c = (CompoundExpression) e1;
            // 复合运算表达式，属于算数布尔运算且左值不是常量，需要进一步评估是否含有列引用
            return BOOL_OPERATOR.contains(c.getOperator()) && c.getLeft() instanceof CompoundExpression;
        }).stream().filter(ce -> {
            CompoundExpression c = (CompoundExpression) ce;
            return containsColumnReference(c.getLeft());
        })).collect(Collectors.toList());
    }

    /**
     * 匹配 {@link Expression} 中是否含有列引用
     */
    protected abstract boolean containsColumnReference(Expression expr);

}
