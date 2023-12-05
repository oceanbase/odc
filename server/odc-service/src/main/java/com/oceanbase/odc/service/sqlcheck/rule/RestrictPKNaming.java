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
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTableAction;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnAttributes;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.InLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.SortColumn;

import lombok.NonNull;

/**
 * {@link RestrictPKNaming}
 *
 * @author yh263208
 * @date 2023-06-28 10:17
 * @since ODC_release_4.2.0
 */
public class RestrictPKNaming implements SqlCheckRule {

    private final Pattern namePattern;

    public RestrictPKNaming(String namePattern) {
        Pattern p;
        if (namePattern == null) {
            p = null;
        } else {
            try {
                p = Pattern.compile(namePattern, Pattern.CASE_INSENSITIVE);
            } catch (Exception e) {
                p = null;
            }
        }
        this.namePattern = p;
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.RESTRICT_PK_NAMING;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        if (statement instanceof CreateTable) {
            CreateTable createTable = (CreateTable) statement;
            String tableName = createTable.getTableName();
            List<Statement> statements = filterPKColumnDefs(tableName, createTable.getColumnDefinitions().stream());
            statements.addAll(createTable.getConstraints().stream().filter(c -> {
                if (!c.isPrimaryKey() || (c.getConstraintName() == null && c.getIndexName() == null)) {
                    return false;
                }
                String name = c.getConstraintName() == null ? c.getIndexName() : c.getConstraintName();
                return !matches(tableName, name, c.getColumns());
            }).collect(Collectors.toList()));
            return statements.stream().map(mapper(statement.getText())).collect(Collectors.toList());
        } else if (statement instanceof AlterTable) {
            AlterTable alterTable = (AlterTable) statement;
            String tableName = alterTable.getTableName();
            List<Statement> statements = filterPKColumnDefs(tableName, SqlCheckUtil.fromAlterTable(alterTable));
            statements.addAll(alterTable.getAlterTableActions().stream().filter(alterTableAction -> {
                OutOfLineConstraint c = alterTableAction.getAddConstraint();
                if (c == null || !c.isPrimaryKey()) {
                    return false;
                }
                return c.getConstraintName() != null
                        && !matches(tableName, c.getConstraintName(), c.getColumns());
            }).map(AlterTableAction::getAddConstraint).collect(Collectors.toList()));
            return statements.stream().map(mapper(statement.getText())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.OB_ORACLE, DialectType.MYSQL, DialectType.OB_MYSQL,
                DialectType.ODP_SHARDING_OB_MYSQL);
    }

    private boolean matches(String tableName, String indexName, List<SortColumn> columns) {
        String dest = StringUtils.unquoteMySqlIdentifier(indexName);
        dest = StringUtils.unquoteOracleIdentifier(dest);
        if (this.namePattern != null) {
            return namePattern.matcher(dest).matches();
        }
        return SqlCheckUtil.generateDefaultIndexName("pk", tableName, columns).equalsIgnoreCase(dest);
    }

    private String getPattern() {
        if (this.namePattern != null) {
            return this.namePattern.toString();
        }
        return "pk_${table-name}_${column-name-1}_${column-name-2}_...";
    }

    private Function<Statement, CheckViolation> mapper(String sql) {
        return s -> {
            String name = "";
            if (s instanceof InLineConstraint) {
                name = ((InLineConstraint) s).getConstraintName();
            } else if (s instanceof OutOfLineConstraint) {
                OutOfLineConstraint c = (OutOfLineConstraint) s;
                name = c.getConstraintName() == null ? c.getIndexName() : c.getConstraintName();
            }
            return SqlCheckUtil.buildViolation(sql, s, getType(), new Object[] {name, getPattern()});
        };
    }

    private List<Statement> filterPKColumnDefs(String tableName, Stream<ColumnDefinition> stream) {
        return stream.filter(d -> {
            ColumnAttributes ca = d.getColumnAttributes();
            if (ca == null) {
                return false;
            }
            return CollectionUtils.isNotEmpty(ca.getConstraints());
        })
                .flatMap(d -> d.getColumnAttributes().getConstraints().stream().map(c -> new Pair<>(d, c)))
                .filter(p -> {
                    InLineConstraint c = p.right;
                    List<SortColumn> s = Collections.singletonList(new SortColumn(p.left.getColumnReference()));
                    return c.isPrimaryKey()
                            && c.getConstraintName() != null
                            && !matches(tableName, c.getConstraintName(), s);
                }).map(c -> c.right).collect(Collectors.toList());
    }

}
