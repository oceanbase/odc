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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;

import lombok.NonNull;

public abstract class BaseTooManyAlterStatement implements SqlCheckRule {

    private final Integer maxAlterCount;

    public BaseTooManyAlterStatement(@NonNull Integer maxAlterCount) {
        this.maxAlterCount = maxAlterCount <= 0 ? 1 : maxAlterCount;
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.TOO_MANY_ALTER_STATEMENT;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        List<AlterTable> alterTables = context.getAllCheckedStatements(AlterTable.class).stream().map(p -> p.left)
                .collect(Collectors.toList());
        Map<String, List<CheckViolation>> sql2Violations = context.getAllCheckViolations().stream()
                .filter(v -> v.getType() == getType())
                .collect(Collectors.groupingBy(v -> v.getArgs()[1].toString()));
        Map<String, List<AlterTable>> tableName2Alters = alterTables.stream()
                .collect(Collectors.groupingBy(a -> unquoteIdentifier(a.getTableName())));
        if (statement instanceof AlterTable) {
            AlterTable alterTable = (AlterTable) statement;
            List<AlterTable> tables = tableName2Alters.computeIfAbsent(
                    unquoteIdentifier(alterTable.getTableName()), s -> new ArrayList<>());
            tables.add(alterTable);
        }
        return tableName2Alters.entrySet().stream()
                .filter(e -> e.getValue().size() > maxAlterCount).filter(e -> !sql2Violations.containsKey(e.getKey()))
                .map(e -> {
                    List<AlterTable> as = e.getValue();
                    String sql = as.stream().map(AlterTable::getText).collect(Collectors.joining(";"));
                    return new CheckViolation(sql, 1, 0, 0, sql.length() - 1, getType(), 0, new Object[] {
                            as.size(), e.getKey(), maxAlterCount});
                }).collect(Collectors.toList());
    }

    protected abstract String unquoteIdentifier(String identifier);

}
