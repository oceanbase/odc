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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.parser.DropStatement;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTableAction;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.GenerateOption.Type;
import com.oceanbase.tools.sqlparser.statement.createtable.InLineConstraint;
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

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.OFFLINE_SCHEMA_CHANGE_EXISTS;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        if (statement instanceof AlterTable) {
            AlterTable alterTable = (AlterTable) statement;
            return alterTable.getAlterTableActions().stream().flatMap(action -> {
                List<CheckViolation> violations = new ArrayList<>();
                violations.addAll(addColumnInLocation(statement, action));
                violations.addAll(changeColumnInLocation(statement, action));
                violations.addAll(addAutoIncrementColumn(statement, action));
                violations.addAll(changeColumnToAutoIncrement(statement, action));
                violations.addAll(changeColumnToPK(statement, action));
                violations.addAll(addOrDropStoredVirtualColumn(statement, action));
                violations.addAll(dropColumn(statement, action));
                violations.addAll(addOrDropPK(statement, action));
                violations.addAll(changeCharsetOrCollation(statement, action));
                violations.addAll(modifyPartition(statement, action));
                violations.addAll(dropPartition(statement, action));
                violations.addAll(truncatePartition(statement, action));
                return violations.stream();
            }).collect(Collectors.toList());
        } else if (statement instanceof TruncateTable) {
            return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(),
                    statement, getType(), new Object[] {}));
        } else if (statement instanceof DropStatement) {
            DropStatement dropStatement = (DropStatement) statement;
            if ("TABLE".equals(dropStatement.getObjectType())) {
                return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(),
                        statement, getType(), new Object[] {}));
            }
        }
        return Collections.emptyList();
    }

    protected List<CheckViolation> addColumnInLocation(Statement statement, AlterTableAction action) {
        return addColumn(action, definition -> {
            if (definition.getLocation() != null) {
                return SqlCheckUtil.buildViolation(statement.getText(), action, getType(), new Object[] {});
            }
            return null;
        });
    }

    protected List<CheckViolation> modifyPartition(Statement statement, AlterTableAction action) {
        if (action.getModifyPartition() != null) {
            return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(),
                    action, getType(), new Object[] {}));
        }
        return Collections.emptyList();
    }

    protected List<CheckViolation> dropPartition(Statement statement, AlterTableAction action) {
        if (CollectionUtils.isNotEmpty(action.getDropPartitionNames())
                || CollectionUtils.isNotEmpty(action.getDropSubPartitionNames())) {
            return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(),
                    action, getType(), new Object[] {}));
        }
        return Collections.emptyList();
    }

    protected List<CheckViolation> truncatePartition(Statement statement, AlterTableAction action) {
        if (CollectionUtils.isNotEmpty(action.getTruncatePartitionNames())
                || CollectionUtils.isNotEmpty(action.getTruncateSubPartitionNames())) {
            return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(),
                    action, getType(), new Object[] {}));
        }
        return Collections.emptyList();
    }

    protected List<CheckViolation> dropColumn(Statement statement, AlterTableAction action) {
        if (CollectionUtils.isNotEmpty(action.getDropColumns())) {
            return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(),
                    action, getType(), new Object[] {}));
        }
        return Collections.emptyList();
    }

    protected List<CheckViolation> addAutoIncrementColumn(Statement statement, AlterTableAction action) {
        return addColumn(action, definition -> {
            if (Boolean.TRUE.equals(definition.getColumnAttributes().getAutoIncrement())) {
                return SqlCheckUtil.buildViolation(statement.getText(), action, getType(), new Object[] {});
            }
            return null;
        });
    }

    protected List<CheckViolation> addOrDropStoredVirtualColumn(Statement statement, AlterTableAction action) {
        return addColumn(action, definition -> {
            if (Type.STORED.equals(definition.getGenerateOption().getType())) {
                return SqlCheckUtil.buildViolation(statement.getText(), action, getType(), new Object[] {});
            }
            return null;
        });
    }

    protected List<CheckViolation> addOrDropPK(Statement statement, AlterTableAction action) {
        List<CheckViolation> violations = new ArrayList<>();
        if (action == null) {
            return violations;
        }
        if (Boolean.TRUE.equals(action.getDropPrimaryKey())) {
            violations.add(SqlCheckUtil.buildViolation(statement.getText(), action, getType(), new Object[] {}));
        }
        if (action.getAddConstraint() != null && action.getAddConstraint().isPrimaryKey()) {
            violations.add(SqlCheckUtil.buildViolation(statement.getText(), action, getType(), new Object[] {}));
        }
        return violations;
    }

    protected List<CheckViolation> changeColumnInLocation(Statement statement, AlterTableAction action) {
        return changeColumn(action, definition -> {
            if (definition.getLocation() != null) {
                return SqlCheckUtil.buildViolation(statement.getText(), action, getType(), new Object[] {});
            }
            return null;
        });
    }

    protected List<CheckViolation> changeColumnToAutoIncrement(Statement statement, AlterTableAction action) {
        return changeColumn(action, definition -> {
            if (Boolean.TRUE.equals(definition.getColumnAttributes().getAutoIncrement())) {
                return SqlCheckUtil.buildViolation(statement.getText(), action, getType(), new Object[] {});
            }
            return null;
        });
    }

    protected List<CheckViolation> changeColumnToPK(Statement statement, AlterTableAction action) {
        return changeColumn(action, definition -> {
            if (CollectionUtils.isEmpty(definition.getColumnAttributes().getConstraints())) {
                return null;
            }
            if (definition.getColumnAttributes().getConstraints().stream().anyMatch(InLineConstraint::isPrimaryKey)) {
                return SqlCheckUtil.buildViolation(statement.getText(), action, getType(), new Object[] {});

            }
            return null;
        });
    }

    protected List<CheckViolation> changeCharsetOrCollation(Statement statement, AlterTableAction action) {
        if (action.getCharset() != null || action.getCollation() != null) {
            return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(),
                    action, getType(), new Object[] {}));
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

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Collections.singletonList(DialectType.OB_MYSQL);
    }

}
