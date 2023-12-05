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
import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.select.FromReference;
import com.oceanbase.tools.sqlparser.statement.select.JoinReference;
import com.oceanbase.tools.sqlparser.statement.select.RelatedSelectBody;
import com.oceanbase.tools.sqlparser.statement.select.Select;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;

import lombok.NonNull;

/**
 * {@link TooManyTableJoin}
 *
 * @author yh263208
 * @date 2022-12-26 17:36
 * @since ODC_release_4.1.0
 * @see SqlCheckRule
 */
public class TooManyTableJoin implements SqlCheckRule {

    private final Integer maxJoinTableCount;

    public TooManyTableJoin(@NonNull Integer maxJoinTableCount) {
        this.maxJoinTableCount = maxJoinTableCount < 0 ? 0 : maxJoinTableCount;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        if (!(statement instanceof Select)) {
            return Collections.emptyList();
        }
        SelectBody select = ((Select) statement).getSelectBody();
        List<JoinReference> joins = select.getFroms().stream()
                .filter(r -> r instanceof JoinReference)
                .map(r -> (JoinReference) r).collect(Collectors.toList());
        /**
         * 避免死循环
         */
        int exceed = 1000;
        RelatedSelectBody related = select.getRelatedSelect();
        while (related != null && (--exceed) > 0) {
            // 如果 select 语句 union 了其他 select，这里需要挨个检查 join
            select = related.getSelect();
            joins.addAll(select.getFroms().stream()
                    .filter(r -> r instanceof JoinReference)
                    .map(r -> (JoinReference) r).collect(Collectors.toList()));
            related = select.getRelatedSelect();
        }
        return getTooManyJoinRefs(joins).stream().map(j -> {
            int count = getJoinTableCount(j);
            return SqlCheckUtil.buildViolation(statement.getText(), j, getType(), context.getStatementOffset(statement),
                    new Object[] {maxJoinTableCount, count});
        }).collect(Collectors.toList());
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.TOO_MANY_TABLE_JOIN;
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.OB_MYSQL, DialectType.MYSQL, DialectType.OB_ORACLE,
                DialectType.ODP_SHARDING_OB_MYSQL);
    }

    protected List<JoinReference> getTooManyJoinRefs(List<JoinReference> joins) {
        return joins.stream().filter(join -> getJoinTableCount(join) >= maxJoinTableCount).collect(Collectors.toList());
    }

    private int getJoinTableCount(JoinReference join) {
        int count = 1;
        FromReference from = join.getLeft();
        while (from instanceof JoinReference) {
            count++;
            from = ((JoinReference) from).getLeft();
        }
        return count + 1;
    }

}
