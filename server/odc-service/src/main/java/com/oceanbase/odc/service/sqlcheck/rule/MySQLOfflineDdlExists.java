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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.VersionUtils;
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
import com.oceanbase.tools.sqlparser.statement.common.CharacterType;
import com.oceanbase.tools.sqlparser.statement.common.DataType;
import com.oceanbase.tools.sqlparser.statement.common.NumberType;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnAttributes;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.GenerateOption.Type;
import com.oceanbase.tools.sqlparser.statement.createtable.InLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.TableOptions;
import com.oceanbase.tools.sqlparser.statement.droptable.DropTable;
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
 * @ref https://www.oceanbase.com/docs/common-oceanbase-database-cn-1000000002017083
 */
public class MySQLOfflineDdlExists implements SqlCheckRule {
    // compatible map
    // array[0] represent ranking
    // array[1] represent default precision
    private static final Map<String, int[]> INTEGER_RANKING_MAP = new HashMap<String, int[]>() {
        {
            put("BOOL", new int[] {0, 1});
            put("BOOLEAN", new int[] {0, 1});
            put("TINYINT", new int[] {1, 4});
            put("SMALLINT", new int[] {2, 6});
            put("MEDIUMINT", new int[] {4, 9});
            put("INT", new int[] {4, 11});
            put("INTEGER", new int[] {4, 11});
            put("BIGINT", new int[] {5, 20});
        }
    };

    private static final int[] INVALID_INTEGER_RANKING_VALUE = new int[] {-1, 0};

    private static final Map<String, Integer> TEXT_RANKING_MAP = new HashMap<String, Integer>() {
        {
            put("TEXT", 0);
            put("MEDIUMTEXT", 1);
            put("LONGTEXT", 2);
        }
    };

    private final JdbcOperations jdbcOperations;
    private final Supplier<String> dbVersionSupplier;

    public MySQLOfflineDdlExists(Supplier<String> dbVersionSupplier, JdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
        this.dbVersionSupplier = dbVersionSupplier;
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
            String dbVersion = getDBVersion();
            return alterTable.getAlterTableActions().stream().flatMap(action -> {
                List<CheckViolation> violations = new ArrayList<>();
                violations.addAll(addColumnInLocation(statement, action));
                violations.addAll(changeColumnInLocation(statement, action));
                violations.addAll(addAutoIncrementColumn(statement, action));
                violations.addAll(changeColumnToAutoIncrement(statement, createTable, action));
                violations.addAll(changeColumnDataType(dbVersion, statement, createTable, action));
                violations.addAll(changeColumnToPrimaryKey(createTable, statement, action));
                violations.addAll(addOrDropStoredVirtualColumn(statement, action));
                violations.addAll(dropColumn(statement, action));
                violations.addAll(addOrDropPrimaryKey(createTable, statement, action));
                violations.addAll(changeCharsetOrCollation(statement, action));
                violations.addAll(changePartition(statement, action));
                violations.addAll(dropPartition(statement, action));
                violations.addAll(truncatePartition(statement, action));
                return violations.stream();
            }).filter(Objects::nonNull).collect(Collectors.toList());
        } else if (statement instanceof TruncateTable) {
            return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(),
                    statement, getType(), new Object[] {"TRUNCATE TABLE"}));
        } else if (statement instanceof DropTable) {
            return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(),
                    statement, getType(), new Object[] {"DROP TABLE"}));
        }
        return Collections.emptyList();
    }

    protected String getDBVersion() {
        String version = null;
        try {
            // get version failed, return null version
            version = dbVersionSupplier == null ? null : dbVersionSupplier.get();
        } catch (Throwable e) {
        }
        return version;
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

    protected List<CheckViolation> changeColumnDataType(String dbVersion, Statement statement, CreateTable target,
            AlterTableAction action) {
        boolean isOb4x = StringUtils.isNotEmpty(dbVersion) && VersionUtils.isGreaterThan(dbVersion, "4.0.0");
        return changeColumn(action, changed -> {
            ColumnDefinition origin = extractColumnDefFrom(target, changed.getColumnReference());
            // only ob 4.x check online feature
            if (isOb4x) {
                boolean isAllNonNull = Stream.of(target, origin).allMatch(Objects::nonNull);
                if (isAllNonNull && isOnLineDDL(origin, changed, target.getTableOptions())) {
                    return null;
                }
            } else if (origin == null || Objects.equals(origin.getDataType(), changed.getDataType())) {
                return null;
            }
            return SqlCheckUtil.buildViolation(statement.getText(), action, getType(),
                    new Object[] {"MODIFY COLUMN DATA TYPE"});
        });
    }


    // check if data type change is online ddl
    // 1. modify （add/remove/change）default value
    // 2. change null flag null / not null
    // 3. increase precision of char/varchar/text/number
    protected boolean isOnLineDDL(ColumnDefinition origin, ColumnDefinition target, TableOptions tableOptions) {
        // actually origin should not be bull
        DataType originDataType = origin.getDataType();
        DataType targetDataType = target.getDataType();
        // check attribute
        if (isAttributeChanged(origin, target)) {
            return false;
        }
        // check foreign key constraint define, reference and be referenced
        if (!objectEquals(origin.getForeignReference(), target.getForeignReference())
                || !objectEquals(origin.getGenerateOption(), target.getGenerateOption())) {
            return false;
        }
        return isDataTypePrecisionExtend(originDataType, targetDataType, tableOptions)
                || isDataTypeCompatible(originDataType, targetDataType);
    }

    protected boolean isAttributeChanged(ColumnDefinition origin, ColumnDefinition target) {
        ColumnAttributes originAttributes = origin.getColumnAttributes();
        ColumnAttributes targetAttributes = target.getColumnAttributes();
        if (!objectEquals(getField(originAttributes, ColumnAttributes::getAutoIncrement),
                getField(targetAttributes, ColumnAttributes::getAutoIncrement))) {
            // auto increment change not support
            return true;
        } else if (!objectEquals(getField(originAttributes, ColumnAttributes::getCollation),
                getField(targetAttributes, ColumnAttributes::getCollation))) {
            // collation change not support
            return true;
        } else if (!collectionEquals(getField(originAttributes, ColumnAttributes::getForeignConstraints),
                getField(targetAttributes, ColumnAttributes::getForeignConstraints))) {
            // inline constraint changed
            return true;
        } else {
            // check constraint changed
            return !collectionEquals(getField(originAttributes, ColumnAttributes::getCheckConstraints),
                    getField(targetAttributes, ColumnAttributes::getCheckConstraints));
        }
    }

    private static <T extends Comparable<T>> int compare(T origin, T target) {
        if (null == origin && null == target) {
            return 0;
        }
        // default precision not decided, always return 1 mean shrink
        if (null == origin || null == target) {
            return 1;
        }
        return origin.compareTo(target);
    }

    private static <T> boolean objectEquals(T origin, T target) {
        if (null == origin && null == target) {
            return true;
        }
        if (null == origin || null == target) {
            return false;
        }
        return origin.equals(target);
    }

    private static <T extends Collection> boolean collectionEquals(T origin, T target) {
        boolean originEmpty = CollectionUtils.isEmpty(origin);
        boolean targetEmpty = CollectionUtils.isEmpty(target);
        if (originEmpty && targetEmpty) {
            return true;
        }
        if (originEmpty || targetEmpty) {
            return false;
        }
        return CollectionUtils.isEqualCollection(origin, target);
    }

    private static <T> T getField(ColumnAttributes attributes, Function<ColumnAttributes, T> valueSupplier) {
        if (null == attributes) {
            return null;
        }
        return valueSupplier.apply(attributes);
    }

    /**
     * only check number, varchar, char type
     *
     * @return
     */
    protected boolean isDataTypePrecisionExtend(DataType origin, DataType target, TableOptions tableOptions) {
        if (!StringUtils.equalsIgnoreCase(origin.getName(), target.getName())
                && !(isDecimalDataType(origin) && isDecimalDataType(target))) {
            return false;
        }
        if (origin instanceof NumberType && target instanceof NumberType) {
            // compare number type with same type name
            return isNumberExtend((NumberType) origin, (NumberType) target);
        } else if (origin instanceof CharacterType && target instanceof CharacterType) {
            // compare character type
            CharacterType originCharacter = (CharacterType) origin;
            CharacterType targetCharacter = (CharacterType) target;
            if (!isCharsetAndCollationCompatible(originCharacter, targetCharacter, tableOptions)
                    || !StringUtils.equalsIgnoreCase(originCharacter.getLengthOption(),
                            targetCharacter.getLengthOption())) {
                return false;
            }
            return compare(originCharacter.getLength(), (targetCharacter.getLength())) <= 0;
        } else {
            return false;
        }
    }

    protected boolean isNumberExtend(NumberType originNumber, NumberType targetNumber) {
        if (!objectEquals(originNumber.getSigned(), targetNumber.getSigned())
                || originNumber.isStarPresicion() != targetNumber.isStarPresicion()
                || originNumber.isZeroFill() != targetNumber.isZeroFill()) {
            return false;
        }
        // decimal(10, 4) -> decimal(10, 5) also a offline ddl
        return compare(originNumber.getScale() == null ? new BigDecimal(0) : originNumber.getScale(),
                targetNumber.getScale() == null ? new BigDecimal(0) : targetNumber.getScale()) == 0
                && compare(getDefaultPrecision(originNumber), getDefaultPrecision(targetNumber)) <= 0;
    }

    protected BigDecimal getDefaultPrecision(NumberType number) {
        if (number.getPrecision() != null) {
            return number.getPrecision();
        }
        int precision =
                INTEGER_RANKING_MAP.getOrDefault(number.getName().toUpperCase(), INVALID_INTEGER_RANKING_VALUE)[1];
        return new BigDecimal(precision);
    }


    // assume table's default charset gbk, default collate gbk_bin.
    // for create table column definition '`c6` varchar(60) COLLATE gbk_bin'.
    // 'modify c6 varchar(80) charset gbk' is offline ddl.
    // 'modify c6 varchar(80) charset gbk collate gbk_bin' is online ddl.
    // 'modify c6 varchar(80) collate gbk_bin' is online ddl.
    // 'modify c6 varchar(80)' is online ddl.
    // for create table column definition '`c6` varchar(60) charset gbk'.
    // 'modify c6 varchar(120) charset gbk' is online ddl.
    // 'modify c6 varchar(120) charset gbk COLLATE gbk_bin' is offline ddl.
    // 'modify c6 varchar(120) collate gbk_bin' is offline ddl.
    // 'modify c6 varchar(120)' is offline ddl.
    // so the point is collate change compare, if collate changed in ddl definition, it must be a
    // offline ddl modify ddl charset.
    // if collate can derived from table option depends on if charset is provided.
    protected boolean isCharsetAndCollationCompatible(CharacterType origin, CharacterType target,
            TableOptions tableOptions) {
        // first check collation, collation should not be derived if charset provided
        String originCollation = getCollationDependsOnCharset(origin, tableOptions.getCollation());
        String targetCollation = getCollationDependsOnCharset(target, tableOptions.getCollation());
        if (!StringUtils.equalsIgnoreCase(originCollation, targetCollation)) {
            return false;
        }
        // check charset
        String originCharset = getOrDefault(origin, CharacterType::getCharset, tableOptions.getCharset());
        String targetCharset = getOrDefault(target, CharacterType::getCharset, tableOptions.getCharset());
        return StringUtils.equalsIgnoreCase(originCharset, targetCharset);
    }

    protected String getCollationDependsOnCharset(CharacterType characterType, String defaultCollation) {
        String collation = null;
        if (StringUtils.isEmpty(characterType.getCharset())) {
            // if collation and charset not supplied, use given or default collation
            collation = getOrDefault(characterType, CharacterType::getCollation, defaultCollation);
        } else {
            // charset provided, collation not derived from table option
            collation = characterType.getCollation();
        }
        return collation;
    }

    protected String getOrDefault(CharacterType characterType, Function<CharacterType, String> valueFunc,
            String defaultValue) {
        String value = valueFunc.apply(characterType);
        if (StringUtils.isEmpty(value)) {
            value = defaultValue;
        }
        return value;
    }

    protected boolean isDecimalDataType(DataType dataType) {
        return StringUtils.equalsIgnoreCase(dataType.getName(), "DECIMAL")
                || StringUtils.equalsIgnoreCase(dataType.getName(), "DEC")
                || StringUtils.equalsIgnoreCase(dataType.getName(), "NUMBER");
    }

    protected boolean isDataTypeCompatible(DataType origin, DataType target) {
        if (origin instanceof NumberType && target instanceof NumberType) {
            // check integer
            int originRanking =
                    INTEGER_RANKING_MAP.getOrDefault(origin.getName().toUpperCase(), INVALID_INTEGER_RANKING_VALUE)[0];
            int targetRanking =
                    INTEGER_RANKING_MAP.getOrDefault(target.getName().toUpperCase(), INVALID_INTEGER_RANKING_VALUE)[0];
            // type, scale and precision should be extended as well
            return originRanking >= 0 && targetRanking >= 0 && originRanking <= targetRanking
                    && isNumberExtend((NumberType) origin, (NumberType) target);
        } else if (origin instanceof CharacterType && target instanceof CharacterType) {
            // check integer
            int originRanking = TEXT_RANKING_MAP.getOrDefault(origin.getName().toUpperCase(), -1);
            int targetRanking = TEXT_RANKING_MAP.getOrDefault(target.getName().toUpperCase(), -1);
            return originRanking >= 0 && targetRanking >= 0 && originRanking <= targetRanking;
        } else {
            return false;
        }
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
