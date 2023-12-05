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

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.OBMySQLParser;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTableAction;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnAttributes;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.InLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.SortColumn;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;

import lombok.NonNull;

/**
 * {@link MySQLRestrictPKAutoIncrement}
 *
 * @author yh263208
 * @date 2023-06-20 18:26
 * @since ODC_release_4.2.0
 */
public class MySQLRestrictPKAutoIncrement implements SqlCheckRule {

    private final JdbcOperations jdbcOperations;

    public MySQLRestrictPKAutoIncrement(JdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.RESTRICT_PK_AUTO_INCREMENT;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        if (statement instanceof CreateTable) {
            CreateTable createTable = (CreateTable) statement;
            String ddl = statement.getText();
            List<CheckViolation> violations = builds(ddl, createTable.getColumnDefinitions().stream());
            Map<String, Boolean> col2AutoIncrement = getColumnName2AutoIncrement(createTable);
            violations.addAll(builds(ddl, col2AutoIncrement, createTable.getConstraints().stream()));
            return violations;
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
            Map<String, Boolean> col2AutoIncrement = getColumnName2AutoIncrement(createTable);
            violations.addAll(builds(ddl, col2AutoIncrement, alterTable.getAlterTableActions().stream()
                    .filter(a -> a.getAddConstraint() != null).map(AlterTableAction::getAddConstraint)));
            return violations;
        }
        return Collections.emptyList();
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.OB_MYSQL, DialectType.MYSQL, DialectType.ODP_SHARDING_OB_MYSQL);
    }

    private Map<String, Boolean> getColumnName2AutoIncrement(CreateTable createTable) {
        Map<String, Boolean> col2AutoIncrement = new HashMap<>();
        createTable.getColumnDefinitions().forEach(d -> {
            String column = SqlCheckUtil.unquoteMySQLIdentifier(d.getColumnReference().getColumn());
            ColumnAttributes ca = d.getColumnAttributes();
            col2AutoIncrement.put(column, ca != null && Boolean.TRUE.equals(ca.getAutoIncrement()));
        });
        return col2AutoIncrement;
    }

    private Predicate<SortColumn> notAutoIncrement(Map<String, Boolean> col2AutoIncrement) {
        return sortColumn -> {
            if (!(sortColumn.getColumn() instanceof ColumnReference)) {
                return true;
            }
            ColumnReference c = (ColumnReference) sortColumn.getColumn();
            String columnName = SqlCheckUtil.unquoteMySQLIdentifier(c.getColumn());
            return !col2AutoIncrement.getOrDefault(columnName, false);
        };
    }

    private CreateTable getTable(String schema, String tableName, SqlCheckContext context) {
        List<CreateTable> tables = context.getAllCheckedStatements(CreateTable.class).stream().map(p -> p.left)
                .collect(Collectors.toList());;
        if (CollectionUtils.isEmpty(tables)) {
            return getTableFromRemote(jdbcOperations, schema, tableName);
        }
        Optional<CreateTable> optional = tables.stream().filter(
                t -> Objects.equals(SqlCheckUtil.unquoteMySQLIdentifier(t.getTableName()),
                        SqlCheckUtil.unquoteMySQLIdentifier(tableName)))
                .findAny();
        return optional.orElseGet(() -> getTableFromRemote(jdbcOperations, schema, tableName));
    }

    private CreateTable getTableFromRemote(JdbcOperations jdbcOperations, String schema, String tableName) {
        String sql = "SHOW CREATE TABLE " + (schema == null ? tableName : (schema + "." + tableName));
        try {
            String ddl = jdbcOperations.queryForObject(sql, (rs, rowNum) -> rs.getString(2));
            if (ddl == null) {
                return null;
            }
            Statement statement = new OBMySQLParser().parse(new StringReader(ddl));
            return statement instanceof CreateTable ? (CreateTable) statement : null;
        } catch (Exception e) {
            return null;
        }
    }

    private List<CheckViolation> builds(String sql, Map<String, Boolean> col2AutoIncrement,
            Stream<OutOfLineConstraint> stream) {
        return stream.filter(c -> c.isPrimaryKey()
                && c.getColumns().size() == 1
                && c.getColumns().stream().anyMatch(notAutoIncrement(col2AutoIncrement)))
                .flatMap(c -> c.getColumns().stream().filter(notAutoIncrement(col2AutoIncrement))
                        .map(s -> SqlCheckUtil.buildViolation(sql, s, getType(), new Object[] {})))
                .collect(Collectors.toList());
    }

    private List<CheckViolation> builds(String sql, Stream<ColumnDefinition> stream) {
        return stream.filter(t -> {
            ColumnAttributes a = t.getColumnAttributes();
            if (a == null || CollectionUtils.isEmpty(a.getConstraints())) {
                return false;
            }
            return a.getConstraints().stream().anyMatch(InLineConstraint::isPrimaryKey)
                    && !Boolean.TRUE.equals(a.getAutoIncrement());
        }).map(t -> SqlCheckUtil.buildViolation(sql, t, getType(), new Object[] {}))
                .collect(Collectors.toList());
    }

}
