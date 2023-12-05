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

import java.util.ArrayList;
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
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;
import com.oceanbase.tools.sqlparser.statement.select.RelatedSelectBody;
import com.oceanbase.tools.sqlparser.statement.select.Select;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;

import lombok.NonNull;

/**
 * {@link SelectStarExists}
 *
 * @author yh263208
 * @date 2023-06-27 13:48
 * @since ODC_release_4.2.0
 */
public class SelectStarExists implements SqlCheckRule {

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.SELECT_STAR_EXISTS;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        if (!(statement instanceof Select)) {
            return Collections.emptyList();
        }
        SelectBody selectBody = ((Select) statement).getSelectBody();
        List<CheckViolation> violations = new ArrayList<>();
        while (selectBody != null) {
            violations.addAll(selectBody.getSelectItems().stream().filter(p -> {
                if (p.isStar()) {
                    return true;
                }
                Expression e = p.getColumn();
                if (e instanceof ColumnReference) {
                    return "*".equals(((ColumnReference) e).getColumn());
                } else if (e instanceof RelationReference) {
                    RelationReference r = (RelationReference) e;
                    while (r != null) {
                        if ("*".equals(r.getRelationName())) {
                            return true;
                        }
                        if (!(r.getReference() instanceof RelationReference)) {
                            return false;
                        }
                        r = (RelationReference) r.getReference();
                    }
                }
                return false;
            }).map(p -> SqlCheckUtil.buildViolation(statement.getText(), p, getType(),
                    context.getStatementOffset(statement), new Object[] {}))
                    .collect(Collectors.toList()));
            RelatedSelectBody rs = selectBody.getRelatedSelect();
            selectBody = rs == null ? null : rs.getSelect();
        }
        return violations;
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.OB_MYSQL, DialectType.MYSQL, DialectType.OB_ORACLE,
                DialectType.ODP_SHARDING_OB_MYSQL);
    }

}
