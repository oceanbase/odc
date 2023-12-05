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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTableAction;
import com.oceanbase.tools.sqlparser.statement.common.DataType;
import com.oceanbase.tools.sqlparser.statement.createindex.CreateIndex;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnAttributes;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineIndex;
import com.oceanbase.tools.sqlparser.statement.createtable.SortColumn;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;

import lombok.NonNull;

/**
 * {@link BaseRestrictIndexDataTypes}
 *
 * @author yh263208
 * @date 2023-06-28 17:11
 * @since ODC_release_4.2.0
 */
abstract class BaseRestrictIndexDataTypes extends BaseRestrictPKDataTypes {

    public BaseRestrictIndexDataTypes(JdbcOperations jdbcOperations, @NonNull Set<String> allowedTypeNames) {
        super(jdbcOperations, allowedTypeNames);
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.RESTRICT_INDEX_DATATYPES;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        if (statement instanceof CreateTable) {
            CreateTable createTable = (CreateTable) statement;
            String ddl = statement.getText();
            List<CheckViolation> results = builds(ddl, createTable.getColumnDefinitions().stream());
            Map<String, String> col2TypeName = getColumnName2TypeName(createTable);
            results.addAll(buildForConstraints(ddl, col2TypeName, createTable.getConstraints().stream()));
            results.addAll(buildForIndexes(ddl, col2TypeName, createTable.getIndexes().stream()));
            return results;
        } else if (statement instanceof AlterTable) {
            AlterTable alterTable = (AlterTable) statement;
            String ddl = statement.getText();
            List<CheckViolation> results = builds(ddl, SqlCheckUtil.fromAlterTable(alterTable));
            if (this.jdbcOperations == null) {
                return results;
            }
            CreateTable createTable = getTable(alterTable.getSchema(), alterTable.getTableName(), context);
            if (createTable == null) {
                return results;
            }
            Map<String, String> col2TypeName = getColumnName2TypeName(createTable);
            results.addAll(buildForConstraints(ddl, col2TypeName, alterTable.getAlterTableActions()
                    .stream().filter(a -> a.getAddConstraint() != null).map(AlterTableAction::getAddConstraint)));
            results.addAll(buildForIndexes(ddl, col2TypeName, alterTable.getAlterTableActions()
                    .stream().filter(a -> a.getAddIndex() != null).map(AlterTableAction::getAddIndex)));
            return results;
        } else if (statement instanceof CreateIndex && this.jdbcOperations != null) {
            CreateIndex createIndex = (CreateIndex) statement;
            CreateTable createTable =
                    getTable(createIndex.getOn().getSchema(), createIndex.getOn().getRelation(), context);
            if (createTable == null) {
                return Collections.emptyList();
            }
            Map<String, String> col2TypeName = getColumnName2TypeName(createTable);
            return createIndex.getColumns().stream()
                    .filter(notInTypes(col2TypeName))
                    .map(builds(statement.getText(), col2TypeName))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private Function<SortColumn, CheckViolation> builds(String sql, Map<String, String> col2TypeName) {
        return sortColumn -> {
            Expression expr = sortColumn.getColumn();
            String column;
            if (expr instanceof ColumnReference) {
                column = ((ColumnReference) expr).getColumn();
            } else {
                column = SqlCheckUtil.getColumnName((RelationReference) expr);
            }
            Object[] args = new Object[] {column, col2TypeName.get(unquoteIdentifier(column)),
                    String.join(",", allowedTypeNames)};
            return SqlCheckUtil.buildViolation(sql, sortColumn, getType(), args);
        };
    }

    private List<CheckViolation> buildForIndexes(String sql, Map<String, String> col2TypeName,
            Stream<OutOfLineIndex> stream) {
        return stream.filter(index -> index.getColumns().stream().anyMatch(notInTypes(col2TypeName)))
                .flatMap(index -> index.getColumns().stream().filter(notInTypes(col2TypeName))
                        .map(builds(sql, col2TypeName)))
                .collect(Collectors.toList());
    }

    private List<CheckViolation> buildForConstraints(String sql, Map<String, String> col2TypeName,
            Stream<OutOfLineConstraint> stream) {
        return stream.filter(c -> (c.isPrimaryKey() || c.isUniqueKey())
                && c.getColumns().stream().anyMatch(notInTypes(col2TypeName)))
                .flatMap(c -> c.getColumns().stream().filter(notInTypes(col2TypeName))
                        .map(builds(sql, col2TypeName)))
                .collect(Collectors.toList());
    }

    private List<CheckViolation> builds(String sql, Stream<ColumnDefinition> stream) {
        return stream.filter(d -> {
            ColumnAttributes a = d.getColumnAttributes();
            if (a == null || CollectionUtils.isEmpty(a.getConstraints())) {
                return false;
            }
            return a.getConstraints().stream().anyMatch(c -> c.isPrimaryKey() || c.isUniqueKey())
                    && !isTypeAllowed(d.getDataType().getName());
        }).map(d -> {
            DataType t = d.getDataType();
            return SqlCheckUtil.buildViolation(sql, t, getType(),
                    new Object[] {d.getColumnReference().getText(), t.getName(),
                            String.join(",", allowedTypeNames)});
        }).collect(Collectors.toList());
    }

}
