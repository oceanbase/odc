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
import java.util.stream.Collectors;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.alter.table.RenameTable;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;

import lombok.NonNull;

/**
 * {@link OracleRestrictTableNameCase}
 *
 * @author yh263208
 * @date 2023-06-27 11:08
 * @since ODC_release_4.2.0
 */
public class OracleRestrictTableNameCase implements SqlCheckRule {

    private final Boolean lowercase;
    private final Boolean uppercase;

    public OracleRestrictTableNameCase(Boolean lowercase, Boolean uppercase) {
        this.lowercase = lowercase;
        this.uppercase = uppercase;
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.RESTRICT_TABLE_NAME_CASE;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        if (statement instanceof CreateTable) {
            CreateTable createTable = (CreateTable) statement;
            if (!verify(unquoteIdentifier(createTable.getTableName()))) {
                return Collections.singletonList(SqlCheckUtil.buildViolation(
                        statement.getText(), createTable, getType(), new Object[] {createTable.getTableName()}));
            }
        } else if (statement instanceof AlterTable) {
            AlterTable alterTable = (AlterTable) statement;
            return alterTable.getAlterTableActions().stream()
                    .filter(a -> {
                        if (a.getRenameToTable() == null) {
                            return false;
                        }
                        return !verify(unquoteIdentifier(a.getRenameToTable().getRelation()));
                    }).map(a -> {
                        RelationFactor r = a.getRenameToTable();
                        return SqlCheckUtil.buildViolation(statement.getText(), r, getType(),
                                new Object[] {r.getRelation()});
                    }).collect(Collectors.toList());
        } else if (statement instanceof RenameTable) {
            RenameTable renameTable = (RenameTable) statement;
            return renameTable.getActions().stream()
                    .filter(r -> !verify(unquoteIdentifier(r.getTo().getRelation())))
                    .map(a -> SqlCheckUtil.buildViolation(statement.getText(), a.getTo(), getType(),
                            new Object[] {a.getTo().getRelation()}))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Collections.singletonList(DialectType.OB_ORACLE);
    }

    private boolean verify(@NonNull String name) {
        if (Boolean.TRUE.equals(this.lowercase)) {
            return name.toLowerCase().equals(name);
        }
        if (Boolean.TRUE.equals(this.uppercase)) {
            return name.toUpperCase().equals(name);
        }
        return true;
    }

    private String unquoteIdentifier(String identifier) {
        return SqlCheckUtil.unquoteOracleIdentifier(identifier);
    }

}
