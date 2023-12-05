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
 * {@link BaseLeftFuzzyMatch}
 *
 * @author yh263208
 * @date 2022-12-26 17:28
 * @since ODC_release_4.1.0
 * @see SqlCheckRule
 */
abstract class BaseLeftFuzzyMatch implements SqlCheckRule {

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        return getLeftFuzzyMatch(SqlCheckUtil.getWhereClauses(statement)).stream()
                .map(e -> SqlCheckUtil.buildViolation(statement.getText(), e, getType(), null))
                .collect(Collectors.toList());
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.INDEX_COLUMN_FUZZY_MATCH;
    }

    protected List<Expression> getLeftFuzzyMatch(List<Expression> wheres) {
        return wheres.stream().flatMap(e -> SqlCheckUtil.findAll(e, e1 -> {
            if (!(e1 instanceof CompoundExpression)) {
                return false;
            }
            CompoundExpression c = (CompoundExpression) e1;
            // 匹配 like 或者 not like 表达式
            return c.getOperator() == Operator.LIKE || c.getOperator() == Operator.NOT_LIKE;
        }).stream().filter(e2 -> {
            CompoundExpression c = (CompoundExpression) e2;
            return containsLeftFuzzy(c.getRight());
        })).collect(Collectors.toList());
    }

    /**
     * 这里匹配 1 种情况：左模糊匹配：like/not like '%xxx'，当然，'%xxx%' 也属于，需要剔除的情况是：
     *
     * <pre>
     *     1. like/not like 'xxx'
     *     2. like/not like 'xxx%'，右模糊匹配
     * </pre>
     */
    protected abstract boolean containsLeftFuzzy(Expression expr);

}
