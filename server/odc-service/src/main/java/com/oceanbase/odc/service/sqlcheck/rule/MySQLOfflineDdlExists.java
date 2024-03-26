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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.parser.DropStatement;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.OBMySQLParser;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTableAction;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.GenerateOption.Type;
import com.oceanbase.tools.sqlparser.statement.createtable.InLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.truncate.TruncateTable;

import lombok.NonNull;

/**
 * {@link MySQLOfflineDdlExists}
 *
 * @author yh263208
 * @date 2024-03-05 21:12
 * @since ODC_release_4.2.4
 * @ref https://www.oceanbase.com/docs/common-oceanbase-database-cn-1000000000252799
 */
public class MySQLOfflineDdlExists implements SqlCheckRule {

    private final JdbcOperations jdbcOperations;

    public MySQLOfflineDdlExists(JdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.OFFLINE_SCHEMA_CHANGE_EXISTS;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        if (statement instanceof AlterTable) {
            AlterTable alterTable = (AlterTable) statement;
            CreateTable createTable = getTable(alterTable.getSchema(), alterTable.getTableName(), context);
            return alterTable.getAlterTableActions().stream().flatMap(action -> {
                List<CheckViolation> violations = new ArrayList<>();
                violations.addAll(addColumnInLocation(statement, action));
                violations.addAll(changeColumnInLocation(statement, action));
                violations.addAll(addAutoIncrementColumn(statement, action));
                violations.addAll(changeColumnToAutoIncrement(statement, createTable, action));
                violations.addAll(changeColumnDataType(statement, createTable, action));
                violations.addAll(changeColumnToPrimaryKey(createTable, statement, action));
                violations.addAll(addOrDropStoredVirtualColumn(statement, action));
                violations.addAll(dropColumn(statement, action));
                violations.addAll(addOrDropPrimaryKey(createTable, statement, action));
                violations.addAll(changeCharsetOrCollation(statement, action));
                violations.addAll(changePartition(statement, action));
                violations.addAll(dropPartition(statement, action));
                violations.addAll(truncatePartition(statement, action));
                return violations.stream();
            }).collect(Collectors.toList());
        } else if (statement instanceof TruncateTable) {
            return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(),
                    statement, getType(), new Object[] {"TRUNCATE TABLE"}));
        } else if (statement instanceof DropStatement) {
            DropStatement dropStatement = (DropStatement) statement;
            if ("TABLE".equals(dropStatement.getObjectType())) {
                return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(),
                        statement, getType(), new Object[] {"DROP TABLE"}));
            }
        }
        return Collections.emptyList();
    }

    protected List<CheckViolation> addColumnInLocation(Statement statement, AlterTableAction action) {
        return addColumn(action, definition -> {
            if (definition.getLocation() != null) {
                return SqlCheckUtil.buildViolation(statement.getText(), action, getType(),
                        new Object[] {"ADD COLUMN IN THE MIDDLE (BEFORE/AFTER/FIRST)"});
            }
            return null;
        });
    }

    protected List<CheckViolation> changeColumnDataType(Statement statement, CreateTable target,
            AlterTableAction action) {
        return changeColumn(action, changed -> {
            ColumnDefinition origin = extractColumnDefFrom(target, changed.getColumnReference());
            if (origin == null || Objects.equals(origin.getDataType(), changed.getDataType())) {
                return null;
            }
            return SqlCheckUtil.buildViolation(statement.getText(), action, getType(),
                    new Object[] {"MODIFY COLUMN DATA TYPE"});
        });
    }

    protected List<CheckViolation> changePartition(Statement statement, AlterTableAction action) {
        if (action.getModifyPartition() != null) {
            return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(),
                    action, getType(), new Object[] {"MODIFY PARTITION"}));
        }
        return Collections.emptyList();
    }

    protected List<CheckViolation> dropPartition(Statement statement, AlterTableAction action) {
        if (CollectionUtils.isNotEmpty(action.getDropPartitionNames())
                || CollectionUtils.isNotEmpty(action.getDropSubPartitionNames())) {
            return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(),
                    action, getType(), new Object[] {"DROP PARTITION"}));
        }
        return Collections.emptyList();
    }

    protected List<CheckViolation> truncatePartition(Statement statement, AlterTableAction action) {
        if (CollectionUtils.isNotEmpty(action.getTruncatePartitionNames())
                || CollectionUtils.isNotEmpty(action.getTruncateSubPartitionNames())) {
            return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(),
                    action, getType(), new Object[] {"TRUNCATE PARTITION"}));
        }
        return Collections.emptyList();
    }

    protected List<CheckViolation> dropColumn(Statement statement, AlterTableAction action) {
        if (CollectionUtils.isNotEmpty(action.getDropColumns())) {
            return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(),
                    action, getType(), new Object[] {"DROP COLUMN"}));
        }
        return Collections.emptyList();
    }

    protected List<CheckViolation> addAutoIncrementColumn(Statement statement, AlterTableAction action) {
        return addColumn(action, definition -> {
            if (definition.getColumnAttributes() != null
                    && Boolean.TRUE.equals(definition.getColumnAttributes().getAutoIncrement())) {
                return SqlCheckUtil.buildViolation(statement.getText(), action, getType(),
                        new Object[] {"ADD AUTO-INCREMENT COLUMN"});
            }
            return null;
        });
    }

    protected List<CheckViolation> addOrDropStoredVirtualColumn(Statement statement, AlterTableAction action) {
        return addColumn(action, definition -> {
            if (definition.getGenerateOption() != null
                    && Type.STORED.equals(definition.getGenerateOption().getType())) {
                return SqlCheckUtil.buildViolation(statement.getText(), action, getType(),
                        new Object[] {"ADD/DROP STORED GENERATED COLUMN"});
            }
            return null;
        });
    }

    protected List<CheckViolation> addOrDropPrimaryKey(CreateTable createTable,
            Statement statement, AlterTableAction action) {
        List<CheckViolation> violations = new ArrayList<>();
        if (action == null) {
            return violations;
        }
        if (CollectionUtils.isNotEmpty(action.getDropConstraintNames()) && createTable != null) {
            List<String> pkConstraintNames = createTable.getColumnDefinitions().stream().flatMap(d -> {
                if (d.getColumnAttributes() == null
                        || CollectionUtils.isEmpty(d.getColumnAttributes().getConstraints())) {
                    return Stream.empty();
                }
                return d.getColumnAttributes().getConstraints().stream()
                        .filter(c -> c.isPrimaryKey() && c.getConstraintName() != null)
                        .map(InLineConstraint::getConstraintName);
            }).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(createTable.getConstraints())) {
                pkConstraintNames.addAll(createTable.getConstraints().stream().flatMap(c -> {
                    if (!c.isPrimaryKey() || c.getConstraintName() == null) {
                        return Stream.empty();
                    }
                    return Stream.of(c.getConstraintName());
                }).collect(Collectors.toList()));
            }
            List<String> droppedNames = action.getDropConstraintNames().stream()
                    .map(this::unquoteIdentifier).collect(Collectors.toList());
            pkConstraintNames = pkConstraintNames.stream()
                    .map(this::unquoteIdentifier).collect(Collectors.toList());
            if (CollectionUtils.containsAny(droppedNames, pkConstraintNames)) {
                violations.add(SqlCheckUtil.buildViolation(statement.getText(), action, getType(),
                        new Object[] {"DROP PRIMARY KEY"}));
            }
        }
        if (Boolean.TRUE.equals(action.getDropPrimaryKey())) {
            violations.add(SqlCheckUtil.buildViolation(statement.getText(), action, getType(),
                    new Object[] {"DROP PRIMARY KEY"}));
        }
        if (action.getAddConstraint() != null && action.getAddConstraint().isPrimaryKey()) {
            violations.add(SqlCheckUtil.buildViolation(statement.getText(), action, getType(),
                    new Object[] {"ADD PRIMARY KEY"}));
        }
        return violations;
    }

    protected List<CheckViolation> changeColumnInLocation(Statement statement, AlterTableAction action) {
        return changeColumn(action, definition -> {
            if (definition.getLocation() != null) {
                return SqlCheckUtil.buildViolation(statement.getText(), action, getType(),
                        new Object[] {"REARRANGE (BEFORE/AFTER/FIRST)"});
            }
            return null;
        });
    }

    protected List<CheckViolation> changeColumnToAutoIncrement(Statement statement, CreateTable createTable,
            AlterTableAction action) {
        return changeColumn(action, changed -> {
            if (changed.getColumnAttributes() == null
                    || !Boolean.TRUE.equals(changed.getColumnAttributes().getAutoIncrement())) {
                return null;
            }
            ColumnDefinition origin = extractColumnDefFrom(createTable, changed.getColumnReference());
            if (origin != null
                    && origin.getColumnAttributes() != null
                    && Boolean.TRUE.equals(changed.getColumnAttributes().getAutoIncrement())) {
                return null;
            }
            return SqlCheckUtil.buildViolation(statement.getText(), action, getType(),
                    new Object[] {"MODIFY TO AUTO-INCREMENT COLUMN"});
        });
    }

    protected List<CheckViolation> changeColumnToPrimaryKey(CreateTable createTable,
            Statement statement, AlterTableAction action) {
        return changeColumn(action, definition -> {
            if (definition.getColumnAttributes() == null
                    || CollectionUtils.isEmpty(definition.getColumnAttributes().getConstraints())
                    || definition.getColumnAttributes().getConstraints().stream()
                            .noneMatch(InLineConstraint::isPrimaryKey)) {
                return null;
            }
            String column = unquoteIdentifier(definition.getColumnReference().getColumn());
            Optional<ColumnDefinition> optional = createTable.getColumnDefinitions().stream()
                    .filter(def -> Objects.equals(unquoteIdentifier(def.getColumnReference().getColumn()), column))
                    .findAny();
            if (!optional.isPresent()) {
                return null;
            }
            ColumnDefinition colDef = optional.get();
            if (colDef.getColumnAttributes() != null
                    && CollectionUtils.isNotEmpty(colDef.getColumnAttributes().getConstraints())
                    && colDef.getColumnAttributes().getConstraints().stream()
                            .anyMatch(InLineConstraint::isPrimaryKey)) {
                return null;
            }
            List<String> pkColumns = createTable.getConstraints().stream()
                    .filter(OutOfLineConstraint::isPrimaryKey)
                    .flatMap(c -> c.getColumns().stream()
                            .map(sc -> unquoteIdentifier(sc.getColumn().toString())))
                    .collect(Collectors.toList());
            if (pkColumns.contains(column)) {
                return null;
            }
            return SqlCheckUtil.buildViolation(statement.getText(), action, getType(),
                    new Object[] {"MODIFY COLUMN AS PRIMARY KEY"});
        });
    }

    protected List<CheckViolation> changeCharsetOrCollation(Statement statement, AlterTableAction action) {
        if (action.getCharset() != null || action.getCollation() != null) {
            return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(),
                    action, getType(), new Object[] {"CONVERT CHAR SET"}));
        }
        return Collections.emptyList();
    }

    protected List<CheckViolation> addColumn(AlterTableAction action,
            Function<ColumnDefinition, CheckViolation> func) {
        List<CheckViolation> violations = new ArrayList<>();
        if (action == null) {
            return violations;
        }
        if (CollectionUtils.isNotEmpty(action.getAddColumns())) {
            violations.addAll(action.getAddColumns().stream().map(func)
                    .filter(Objects::nonNull).collect(Collectors.toList()));
        }
        return violations;
    }

    protected List<CheckViolation> changeColumn(AlterTableAction action,
            Function<ColumnDefinition, CheckViolation> func) {
        List<CheckViolation> violations = new ArrayList<>();
        if (action == null) {
            return violations;
        }
        if (CollectionUtils.isNotEmpty(action.getModifyColumns())) {
            violations.addAll(action.getModifyColumns().stream().map(func)
                    .filter(Objects::nonNull).collect(Collectors.toList()));
        }
        if (action.getChangeColumnDefinition() != null) {
            violations.add(func.apply(action.getChangeColumnDefinition()));
        }
        return violations;
    }

    protected CreateTable getTableFromRemote(JdbcOperations jdbcOperations, String schema, String tableName) {
        if (jdbcOperations == null) {
            return null;
        }
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

    protected String unquoteIdentifier(String identifier) {
        return SqlCheckUtil.unquoteMySQLIdentifier(identifier);
    }

    private ColumnDefinition extractColumnDefFrom(CreateTable createTable, ColumnReference columnReference) {
        if (createTable == null) {
            return null;
        }
        return createTable.getColumnDefinitions().stream().filter(d -> {
            if (columnReference.getColumn() == null || d.getColumnReference().getColumn() == null) {
                return false;
            }
            return Objects.equals(unquoteIdentifier(
                    columnReference.getColumn()), unquoteIdentifier(d.getColumnReference().getColumn()));
        }).findFirst().orElse(null);
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Collections.singletonList(DialectType.OB_MYSQL);
    }

}
