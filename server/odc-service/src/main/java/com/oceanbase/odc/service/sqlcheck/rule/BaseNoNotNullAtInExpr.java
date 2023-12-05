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

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

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
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;

import lombok.NonNull;

/**
 * {@link BaseNoNotNullAtInExpr}
 *
 * @author yh263208
 * @date 2022-12-26 17:40
 * @since ODC_release_4.1.0
 * @see SqlCheckRule
 */
abstract class BaseNoNotNullAtInExpr implements SqlCheckRule {

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        return getNoNotNullAtNotInExpr(SqlCheckUtil.getWhereClauses(statement)).stream()
                .map(e -> SqlCheckUtil.buildViolation(statement.getText(), e, getType(), null))
                .collect(Collectors.toList());
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.NO_NOT_NULL_EXISTS_NOT_IN;
    }

    protected List<Expression> getNoNotNullAtNotInExpr(List<Expression> wheres) {
        return wheres.stream().flatMap(expr -> SqlCheckUtil.findAll(expr, e1 -> {
            if (!(e1 instanceof CompoundExpression)) {
                return false;
            }
            return ((CompoundExpression) e1).getOperator() == Operator.NOT_IN;
        }).stream().filter(e2 -> {
            CompoundExpression c = (CompoundExpression) e2;
            Expression right = c.getRight();
            if (right instanceof SelectBody) {
                return !containsNotNullForColumnReference((SelectBody) right);
            } else if (right instanceof CollectionExpression) {
                CollectionExpression cl = (CollectionExpression) right;
                return cl.getExpressionList().stream().anyMatch(child -> {
                    if (!(child instanceof ConstExpression)) {
                        return false;
                    }
                    ConstExpression ce = (ConstExpression) child;
                    return StringUtils.containsIgnoreCase(ce.getExprConst(), "null");
                });
            }
            return false;
        })).collect(Collectors.toList());
    }

    /**
     * 匹配 select 语句中是否对每个 column ref 都有 not null 条件过滤 例如：select col from tab where col is not null
     */
    protected abstract boolean containsNotNullForColumnReference(SelectBody selectBody);

}
