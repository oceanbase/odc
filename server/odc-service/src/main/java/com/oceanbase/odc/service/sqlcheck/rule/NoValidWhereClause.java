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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Operator;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.delete.Delete;
import com.oceanbase.tools.sqlparser.statement.expression.BoolValue;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.update.Update;

import lombok.NonNull;

/**
 * {@link NoValidWhereClause}
 *
 * @author yh263208
 * @date 2022-12-26 17:46
 * @since ODC_release_4.1.0
 */
public class NoValidWhereClause implements SqlCheckRule {

    private static final Set<Operator> BOOL_OPERATOR = new HashSet<>(Arrays.asList(
            Operator.EQ, Operator.GE, Operator.LE, Operator.GT, Operator.LT,
            Operator.NE_PL, Operator.NE, Operator.CNNOP, Operator.IN, Operator.NOT_IN,
            Operator.BETWEEN, Operator.NOT_BETWEEN, Operator.LIKE, Operator.NOT_LIKE));

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        if (!(statement instanceof Delete) && !(statement instanceof Update)) {
            // 只对 update 和 delete 语句有效
            return Collections.emptyList();
        }
        List<Expression> wheres = SqlCheckUtil.getWhereClauses(statement);
        if (wheres.isEmpty()) {
            return Collections.emptyList();
        }
        return getExpressionIsAlwaysTrueOrFalse(wheres).stream()
                .map(e -> SqlCheckUtil.buildViolation(statement.getText(), e, getType(),
                        context.getStatementOffset(statement), null))
                .collect(Collectors.toList());
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.NO_VALID_WHERE_CLAUSE;
    }

    // 检测 where 条件是否恒为真/假只需要判断运算符两边是否都为常量即可
    protected List<Expression> getExpressionIsAlwaysTrueOrFalse(List<Expression> where) {
        return where.stream().flatMap(e1 -> SqlCheckUtil.findAll(e1, e2 -> {
            if (!(e2 instanceof CompoundExpression)) {
                return false;
            }
            CompoundExpression c = (CompoundExpression) e2;
            return BOOL_OPERATOR.contains(c.getOperator());
        }).stream().filter(e3 -> {
            CompoundExpression c = (CompoundExpression) e3;
            Predicate<Expression> pri = e -> e instanceof ConstExpression
                    || e instanceof BoolValue
                    || e instanceof CompoundExpression;
            boolean res = SqlCheckUtil.allMatch(c.getLeft(), pri);
            if (!res) {
                return false;
            }
            return SqlCheckUtil.allMatch(c.getRight(), pri);
        })).collect(Collectors.toList());
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.OB_MYSQL, DialectType.MYSQL, DialectType.OB_ORACLE,
                DialectType.ODP_SHARDING_OB_MYSQL);
    }

}
