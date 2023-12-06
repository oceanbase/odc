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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTableAction;
import com.oceanbase.tools.sqlparser.statement.common.DataType;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnAttributes;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.InLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.SortColumn;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;

import lombok.NonNull;

/**
 * {@link BaseRestrictPKDataTypes}
 *
 * @author yh263208
 * @date 2023-06-19 16:18
 * @since ODC_release_4.2.0
 */
abstract class BaseRestrictPKDataTypes implements SqlCheckRule {

    protected final JdbcOperations jdbcOperations;
    protected final Set<String> allowedTypeNames;

    public BaseRestrictPKDataTypes(JdbcOperations jdbcOperations, @NonNull Set<String> allowedTypeNames) {
        this.jdbcOperations = jdbcOperations;
        this.allowedTypeNames = allowedTypeNames;
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.RESTRICT_PK_DATATYPES;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        if (statement instanceof CreateTable) {
            CreateTable createTable = (CreateTable) statement;
            String ddl = statement.getText();
            List<CheckViolation> results = builds(ddl, createTable.getColumnDefinitions().stream());
            results.addAll(builds(ddl, getColumnName2TypeName(createTable), createTable.getConstraints().stream()));
            return results;
        } else if (statement instanceof AlterTable) {
            AlterTable alterTable = (AlterTable) statement;
            String ddl = statement.getText();
            List<CheckViolation> violations = builds(ddl, SqlCheckUtil.fromAlterTable(alterTable));
            if (jdbcOperations == null) {
                return violations;
            }
            CreateTable createTable = getTable(alterTable.getSchema(), alterTable.getTableName(), context);
            if (createTable == null) {
                return violations;
            }
            Map<String, String> col2TypeName = getColumnName2TypeName(createTable);
            violations.addAll(builds(ddl, col2TypeName, alterTable.getAlterTableActions().stream()
                    .filter(a -> a.getAddConstraint() != null).map(AlterTableAction::getAddConstraint)));
            return violations;
        }
        return Collections.emptyList();
    }

    protected abstract String unquoteIdentifier(String identifier);

    protected abstract CreateTable getTableFromRemote(JdbcOperations jdbcOperations, String schema, String tableName);

    protected CreateTable getTable(String schema, String tableName, SqlCheckContext checkContext) {
        List<CreateTable> tables = checkContext.getAllCheckedStatements(CreateTable.class).stream().map(p -> p.left)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(tables)) {
            return getTableFromRemote(jdbcOperations, schema, tableName);
        }
        Optional<CreateTable> optional = tables.stream().filter(
                t -> Objects.equals(unquoteIdentifier(t.getTableName()), unquoteIdentifier(tableName))).findAny();
        return optional.orElseGet(() -> getTableFromRemote(jdbcOperations, schema, tableName));
    }

    protected Map<String, String> getColumnName2TypeName(CreateTable createTable) {
        Map<String, String> col2TypeName = new HashMap<>();
        createTable.getColumnDefinitions().forEach(d -> {
            String column = unquoteIdentifier(d.getColumnReference().getColumn());
            col2TypeName.put(column, d.getDataType().getName());
        });
        return col2TypeName;
    }

    protected boolean isTypeAllowed(String name) {
        return this.allowedTypeNames.stream().anyMatch(s -> StringUtils.equalsIgnoreCase(s, name));
    }

    protected Predicate<SortColumn> notInTypes(Map<String, String> col2TypeName) {
        return sortColumn -> {
            Expression expr = sortColumn.getColumn();
            if (expr instanceof ColumnReference) {
                String typeName = col2TypeName.get(
                        unquoteIdentifier(((ColumnReference) expr).getColumn()));
                return typeName != null && !isTypeAllowed(typeName);
            } else if (expr instanceof RelationReference) {
                String typeName = col2TypeName.get(
                        unquoteIdentifier(SqlCheckUtil.getColumnName((RelationReference) expr)));
                return typeName != null && !isTypeAllowed(typeName);
            }
            return false;
        };
    }

    private List<CheckViolation> builds(String sql, Map<String, String> col2TypeName,
            Stream<OutOfLineConstraint> stream) {
        return stream.filter(c -> c.isPrimaryKey() && c.getColumns().stream().anyMatch(notInTypes(col2TypeName)))
                .flatMap(c -> c.getColumns().stream().filter(notInTypes(col2TypeName)).map(s -> {
                    ColumnReference cr = (ColumnReference) s.getColumn();
                    Object[] args = new Object[] {col2TypeName.get(unquoteIdentifier(cr.getColumn())),
                            String.join(",", allowedTypeNames)};
                    return SqlCheckUtil.buildViolation(sql, s, getType(), args);
                })).collect(Collectors.toList());
    }

    private List<CheckViolation> builds(String sql, Stream<ColumnDefinition> stream) {
        return stream.filter(d -> {
            ColumnAttributes a = d.getColumnAttributes();
            if (a == null || CollectionUtils.isEmpty(a.getConstraints())) {
                return false;
            }
            return a.getConstraints().stream().anyMatch(InLineConstraint::isPrimaryKey)
                    && !isTypeAllowed(d.getDataType().getName());
        }).map(d -> {
            DataType t = d.getDataType();
            return SqlCheckUtil.buildViolation(sql, t, getType(),
                    new Object[] {t.getName(), String.join(",", allowedTypeNames)});
        }).collect(Collectors.toList());
    }

}
