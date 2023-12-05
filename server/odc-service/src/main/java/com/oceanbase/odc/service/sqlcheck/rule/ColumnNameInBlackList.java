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
import java.util.stream.Stream;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;

import lombok.NonNull;

/**
 * {@link ColumnNameInBlackList}
 *
 * @author yh263208
 * @date 2023-06-26 20:59
 * @since ODC_release_4.2.0
 */
public class ColumnNameInBlackList implements SqlCheckRule {

    private final Set<String> blackList;

    public ColumnNameInBlackList(@NonNull Set<String> blackList) {
        this.blackList = blackList;
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.COLUMN_NAME_IN_BLACK_LIST;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        int offset = context.getStatementOffset(statement);
        if (statement instanceof CreateTable) {
            return builds(statement.getText(), offset, ((CreateTable) statement).getColumnDefinitions().stream());
        } else if (statement instanceof AlterTable) {
            AlterTable alterTable = (AlterTable) statement;
            List<CheckViolation> r = builds(statement.getText(), offset, SqlCheckUtil.fromAlterTable(alterTable));
            r.addAll(alterTable.getAlterTableActions().stream().filter(a -> {
                if (a.getRenameFromColumn() == null) {
                    return false;
                }
                return containsIgnoreCase(a.getRenameToColumnName());
            }).map(a -> SqlCheckUtil.buildViolation(statement.getText(), a, getType(), offset,
                    new Object[] {a.getRenameToColumnName(), String.join(",", blackList)}))
                    .collect(Collectors.toList()));
            return r;
        }
        return Collections.emptyList();
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.OB_MYSQL, DialectType.MYSQL, DialectType.OB_ORACLE,
                DialectType.ODP_SHARDING_OB_MYSQL);
    }

    private String unquoteIdentifier(String name) {
        String dest = StringUtils.unquoteMySqlIdentifier(name);
        return StringUtils.unquoteOracleIdentifier(dest);
    }

    private boolean containsIgnoreCase(String name) {
        final String str = unquoteIdentifier(name);
        return this.blackList.stream().anyMatch(s -> StringUtils.equalsIgnoreCase(s, str));
    }

    private List<CheckViolation> builds(String sql, int offset, Stream<ColumnDefinition> stream) {
        return stream.filter(d -> containsIgnoreCase(d.getColumnReference().getColumn()))
                .map(d -> SqlCheckUtil.buildViolation(sql, d.getColumnReference(), getType(), offset, new Object[] {
                        d.getColumnReference().getColumn(), String.join(",", blackList)}))
                .collect(Collectors.toList());
    }

}
