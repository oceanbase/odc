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
import java.util.Set;
import java.util.stream.Collectors;

import com.oceanbase.odc.common.util.StringUtils;
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
 * {@link TableNameInBlackList}
 *
 * @author yh263208
 * @date 2023-06-19 10:39
 * @since ODC_release_4.2.0
 */
public class TableNameInBlackList implements SqlCheckRule {

    private final Set<String> blackList;

    public TableNameInBlackList(@NonNull Set<String> blackList) {
        this.blackList = blackList;
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.TABLE_NAME_IN_BLACK_LIST;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        int offset = context.getStatementOffset(statement);
        if (statement instanceof CreateTable) {
            CreateTable createTable = (CreateTable) statement;
            String name = createTable.getTableName();
            if (containsIgnoreCase(name)) {
                return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(),
                        statement, getType(), offset, new Object[] {name, String.join(",", blackList)}));
            }
        } else if (statement instanceof AlterTable) {
            AlterTable alterTable = (AlterTable) statement;
            return alterTable.getAlterTableActions().stream()
                    .filter(a -> {
                        if (a.getRenameToTable() == null) {
                            return false;
                        }
                        return containsIgnoreCase(a.getRenameToTable().getRelation());
                    }).map(a -> {
                        RelationFactor r = a.getRenameToTable();
                        return SqlCheckUtil.buildViolation(statement.getText(), r, getType(), offset,
                                new Object[] {r.getRelation(), String.join(",", blackList)});
                    }).collect(Collectors.toList());
        } else if (statement instanceof RenameTable) {
            RenameTable renameTable = (RenameTable) statement;
            return renameTable.getActions().stream()
                    .filter(r -> containsIgnoreCase(r.getTo().getRelation()))
                    .map(a -> SqlCheckUtil.buildViolation(statement.getText(), a.getTo(), getType(), offset,
                            new Object[] {a.getTo().getRelation(), String.join(",", blackList)}))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.OB_MYSQL, DialectType.MYSQL, DialectType.OB_ORACLE,
                DialectType.ODP_SHARDING_OB_MYSQL);
    }

    private String unquoteIdentifier(String name) {
        String dest = StringUtils.unquoteMySqlIdentifier(name);
        return StringUtils.unquoteOracleIdentifier(dest);
    }

    private boolean containsIgnoreCase(String name) {
        final String str = unquoteIdentifier(name);
        return this.blackList.stream().anyMatch(s -> StringUtils.equalsIgnoreCase(s, str));
    }

}
