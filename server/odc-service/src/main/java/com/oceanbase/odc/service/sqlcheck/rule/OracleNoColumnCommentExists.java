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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.SetComment;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;

import lombok.NonNull;

/**
 * {@link OracleNoColumnCommentExists}
 *
 * @author yh263208
 * @date 2023-07-31 15:35
 * @since ODC_release_4.2.0
 */
public class OracleNoColumnCommentExists implements SqlCheckRule {

    private final Supplier<String> schemaSupplier;

    public OracleNoColumnCommentExists(Supplier<String> schemaSupplier) {
        this.schemaSupplier = schemaSupplier;
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.NO_COLUMN_COMMENT_EXISTS;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        if (context.getCurrentStmtIndex() < context.getTotalStmtCount() - 1) {
            return Collections.emptyList();
        }
        // 已经检测到了最后一个 sql
        Map<String, List<ColumnDefinition>> tName2ColDefs = new HashMap<>();
        Map<String, List<CreateTable>> tName2CreateTables = new HashMap<>();
        context.getAllCheckedStatements(CreateTable.class).forEach(c -> {
            List<ColumnDefinition> defs = tName2ColDefs.computeIfAbsent(getKey(c), s -> new ArrayList<>());
            defs.addAll(c.getColumnDefinitions());
            List<CreateTable> createTables = tName2CreateTables.computeIfAbsent(getKey(c), s -> new ArrayList<>());
            createTables.add(c);
        });
        List<SetComment> setComments = context.getAllCheckedStatements(SetComment.class);
        if (statement instanceof CreateTable) {
            CreateTable c = (CreateTable) statement;
            List<CreateTable> createTables = tName2CreateTables.computeIfAbsent(getKey(c), s -> new ArrayList<>());
            createTables.add(c);
            List<ColumnDefinition> defs = tName2ColDefs.computeIfAbsent(getKey(c), s -> new ArrayList<>());
            defs.addAll(c.getColumnDefinitions());
        } else if (statement instanceof SetComment) {
            setComments.add((SetComment) statement);
        }
        setComments.stream().filter(s -> s.getColumn() != null).forEach(s -> {
            ColumnReference c = s.getColumn();
            String tableName = SqlCheckUtil.unquoteOracleIdentifier(c.getRelation());
            String key;
            if (c.getSchema() == null) {
                String currentSchema = this.schemaSupplier == null ? null : this.schemaSupplier.get();
                key = currentSchema == null ? tableName : currentSchema + "." + tableName;
            } else {
                key = SqlCheckUtil.unquoteOracleIdentifier(c.getSchema()) + "." + tableName;
            }
            List<ColumnDefinition> defs = tName2ColDefs.get(key);
            if (CollectionUtils.isEmpty(defs)) {
                return;
            }
            String columnName = SqlCheckUtil.unquoteOracleIdentifier(c.getColumn());
            defs.removeIf(d -> StringUtils.equals(
                    SqlCheckUtil.unquoteOracleIdentifier(d.getColumnReference().getColumn()), columnName));
        });
        return tName2ColDefs.entrySet().stream().flatMap(e -> {
            List<CreateTable> tables = tName2CreateTables.get(e.getKey());
            String text = CollectionUtils.isNotEmpty(tables) ? tables.get(0).getText() : "";
            return e.getValue().stream().map(d -> SqlCheckUtil.buildViolation(
                    text, d, getType(), new Object[] {d.getColumnReference().getColumn()}));
        }).collect(Collectors.toList());
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Collections.singletonList(DialectType.OB_ORACLE);
    }

    private String getKey(CreateTable c) {
        String tableName = SqlCheckUtil.unquoteOracleIdentifier(c.getTableName());
        if (c.getSchema() == null) {
            String currentSchema = this.schemaSupplier == null ? null : this.schemaSupplier.get();
            return currentSchema == null ? tableName : currentSchema + "." + tableName;
        }
        return SqlCheckUtil.unquoteOracleIdentifier(c.getSchema()) + "." + tableName;
    }

}

