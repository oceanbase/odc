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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnAttributes;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;

import lombok.NonNull;

public class NoPrimaryKeyNameExists implements SqlCheckRule {

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.NO_PRIMARY_KEY_NAME_EXISTS;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        if (statement instanceof CreateTable) {
            CreateTable createTable = (CreateTable) statement;
            List<Statement> statements = builds(createTable.getColumnDefinitions().stream());
            statements.addAll(createTable.getConstraints().stream().filter(c -> {
                if (c.getConstraintName() != null || c.getIndexName() != null) {
                    return false;
                }
                return c.isPrimaryKey();
            }).collect(Collectors.toList()));
            return statements.stream().map(s -> SqlCheckUtil.buildViolation(
                    statement.getText(), s, getType(), new Object[] {})).collect(Collectors.toList());
        } else if (statement instanceof AlterTable) {
            AlterTable alterTable = (AlterTable) statement;
            List<Statement> statements = builds(SqlCheckUtil.fromAlterTable(alterTable));
            statements.addAll(alterTable.getAlterTableActions().stream().filter(alterTableAction -> {
                OutOfLineConstraint c = alterTableAction.getAddConstraint();
                if (c == null || c.getConstraintName() != null || c.getIndexName() != null) {
                    return false;
                }
                return c.isPrimaryKey();
            }).collect(Collectors.toList()));
            return statements.stream().map(s -> SqlCheckUtil.buildViolation(
                    statement.getText(), s, getType(), new Object[] {})).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.MYSQL, DialectType.OB_MYSQL, DialectType.OB_ORACLE,
                DialectType.ODP_SHARDING_OB_MYSQL);
    }

    private List<Statement> builds(Stream<ColumnDefinition> stream) {
        return stream.filter(d -> {
            ColumnAttributes ca = d.getColumnAttributes();
            if (ca == null) {
                return false;
            }
            return CollectionUtils.isNotEmpty(ca.getConstraints());
        }).flatMap(d -> d.getColumnAttributes().getConstraints().stream()).filter(c -> {
            if (c.getConstraintName() != null) {
                return false;
            }
            return c.isPrimaryKey();
        }).collect(Collectors.toList());
    }

}
