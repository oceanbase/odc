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
import java.util.List;
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
import com.oceanbase.tools.sqlparser.statement.expression.CollectionExpression;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;

import lombok.NonNull;

/**
 * {@link TooManyInExpression}
 *
 * @author yh263208
 * @date 2022-12-26 17:34
 * @since ODC_release_4.1.0
 * @see SqlCheckRule
 */
public class TooManyInExpression implements SqlCheckRule {

    private final Integer maxInExprCount;

    public TooManyInExpression(@NonNull Integer maxInExprCount) {
        this.maxInExprCount = maxInExprCount <= 0 ? 1 : maxInExprCount;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        return getTooManyInExprs(SqlCheckUtil.getWhereClauses(statement)).stream().map(e -> {
            CompoundExpression c = (CompoundExpression) e;
            int count = ((CollectionExpression) c.getRight()).getExpressionList().size();
            return SqlCheckUtil.buildViolation(statement.getText(), e, getType(), context.getStatementOffset(statement),
                    new Object[] {maxInExprCount, count});
        }).collect(Collectors.toList());
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.TOO_MANY_IN_EXPR;
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.OB_MYSQL, DialectType.MYSQL, DialectType.OB_ORACLE,
                DialectType.ODP_SHARDING_OB_MYSQL);
    }

    protected List<Expression> getTooManyInExprs(List<Expression> wheres) {
        return wheres.stream().flatMap(e -> SqlCheckUtil.findAll(e, e1 -> {
            if (!(e1 instanceof CompoundExpression)) {
                return false;
            }
            CompoundExpression c = (CompoundExpression) e1;
            // 匹配 in 或者 not in 表达式
            return c.getOperator() == Operator.IN || c.getOperator() == Operator.NOT_IN;
        }).stream().filter(e2 -> {
            CompoundExpression c = (CompoundExpression) e2;
            if (!(c.getRight() instanceof CollectionExpression)) {
                return false;
            }
            return ((CollectionExpression) c.getRight()).getExpressionList().size() >= maxInExprCount;
        })).collect(Collectors.toList());
    }

}
