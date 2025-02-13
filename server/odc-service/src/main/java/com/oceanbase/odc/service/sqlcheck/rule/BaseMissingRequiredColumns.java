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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;

import lombok.NonNull;

/**
 * {@link BaseMissingRequiredColumns}
 *
 * @author yh263208
 * @date 2023-06-27 15:46
 * @since ODC_release_4.2.0
 */
public abstract class BaseMissingRequiredColumns implements SqlCheckRule {

    private final Set<String> requiredColumns;

    public BaseMissingRequiredColumns(@NonNull Set<String> requiredColumns) {
        this.requiredColumns = requiredColumns;
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.MISSING_REQUIRED_COLUMNS;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        if (!(statement instanceof CreateTable)) {
            return Collections.emptyList();
        }
        CreateTable createTable = (CreateTable) statement;
        if (Objects.nonNull(createTable.getLikeTable()) || Objects.nonNull(createTable.getAs())) {
            return Collections.emptyList();
        }
        List<String> columns = createTable.getColumnDefinitions().stream()
                .map(d -> unquoteIdentifier(d.getColumnReference().getColumn())).collect(Collectors.toList());
        Set<String> tmp = new HashSet<>(requiredColumns);
        tmp.removeIf(s -> columns.contains(unquoteIdentifier(s)));
        if (tmp.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(SqlCheckUtil.buildViolation(
                statement.getText(), statement, getType(), new Object[] {String.join(",", tmp)}));
    }

    protected abstract String unquoteIdentifier(String identifier);

}
