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
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.TableOptions;

import lombok.NonNull;

public class MySQLRestrictTableCollation implements SqlCheckRule {

    private final Set<String> allowedCollations;

    public MySQLRestrictTableCollation(@NonNull Set<String> allowedCollations) {
        this.allowedCollations = allowedCollations;
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.RESTRICT_TABLE_COLLATION;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        int offset = context.getStatementOffset(statement);
        if (statement instanceof CreateTable) {
            TableOptions options = ((CreateTable) statement).getTableOptions();
            String collation = "N/A";
            if (CollectionUtils.isNotEmpty(allowedCollations)) {
                collation = String.join(",", allowedCollations);
            }
            if (options != null && options.getCollation() != null && !containsIgnoreCase(options.getCollation())) {
                return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(), options,
                        getType(), offset, new Object[] {options.getCollation(), collation}));
            }
        } else if (statement instanceof AlterTable) {
            return ((AlterTable) statement).getAlterTableActions().stream()
                    .filter(a -> a.getCollation() != null && !containsIgnoreCase(a.getCollation()))
                    .map(a -> SqlCheckUtil.buildViolation(statement.getText(), a, getType(), offset,
                            new Object[] {a.getCollation(), String.join(",", allowedCollations)}))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.MYSQL, DialectType.OB_MYSQL, DialectType.ODP_SHARDING_OB_MYSQL);
    }

    private boolean containsIgnoreCase(String name) {
        String str = StringUtils.unwrap(name, "'");
        str = StringUtils.unwrap(str, "`");
        final String string = StringUtils.unwrap(str, "\"");
        return this.allowedCollations.stream().anyMatch(s -> StringUtils.equalsIgnoreCase(s, string));
    }

}
