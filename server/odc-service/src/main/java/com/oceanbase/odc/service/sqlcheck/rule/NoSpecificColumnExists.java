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
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.insert.Insert;
import com.oceanbase.tools.sqlparser.statement.insert.InsertTable;

import lombok.NonNull;

/**
 * {@link NoSpecificColumnExists}
 *
 * @author yh263208
 * @date 2022-12-26 17:48
 * @since ODC_release_4.1.0
 * @see SqlCheckRule
 */
public class NoSpecificColumnExists implements SqlCheckRule {

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        if (!(statement instanceof Insert)) {
            return Collections.emptyList();
        }
        Insert insert = (Insert) statement;
        List<InsertTable> insertTables = insert.getTableInsert();
        if (CollectionUtils.isNotEmpty(insertTables)) {
            return build(statement.getText(), insertTables.stream());
        }
        List<CheckViolation> violations = build(statement.getText(),
                insert.getConditionalInsert().getConditions().stream().flatMap(i -> i.getThen().stream()));
        violations.addAll(build(statement.getText(),
                insert.getConditionalInsert().getElseClause().stream()));
        return violations;
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.NO_SPECIFIC_COLUMN_EXISTS;
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.OB_MYSQL, DialectType.MYSQL, DialectType.OB_ORACLE,
                DialectType.ODP_SHARDING_OB_MYSQL);
    }

    private List<CheckViolation> build(String sql, Stream<InsertTable> insertTables) {
        return insertTables
                .filter(i -> CollectionUtils.isEmpty(i.getSetColumns()))
                .filter(i -> CollectionUtils.isEmpty(i.getColumns()))
                .map(i -> SqlCheckUtil.buildViolation(sql, i, getType(), null))
                .collect(Collectors.toList());
    }

}
