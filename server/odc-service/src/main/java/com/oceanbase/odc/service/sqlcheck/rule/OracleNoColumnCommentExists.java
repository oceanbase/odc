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
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;
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
        List<Pair<CreateTable, Integer>> createTables = context.getAllCheckedStatements(CreateTable.class);
        List<Pair<SetComment, Integer>> setComments = context.getAllCheckedStatements(SetComment.class);
        if (statement instanceof SetComment) {
            setComments.add(new Pair<>((SetComment) statement, null));
        } else if (statement instanceof CreateTable) {
            createTables.add(new Pair<>((CreateTable) statement, null));
        }
        Map<String, List<Pair<SetComment, Integer>>> tblName2ColComments = setComments.stream()
                .filter(s -> s.left.getColumn() != null).collect(Collectors.groupingBy(s -> {
                    ColumnReference c = s.left.getColumn();
                    String tableName = SqlCheckUtil.unquoteOracleIdentifier(c.getRelation());
                    if (c.getSchema() == null) {
                        String currentSchema = schemaSupplier == null ? null : schemaSupplier.get();
                        return currentSchema == null ? tableName : currentSchema + "." + tableName;
                    }
                    return SqlCheckUtil.unquoteOracleIdentifier(c.getSchema()) + "." + tableName;
                }));
        return createTables.stream().map(p -> {
            CreateTable createTable = p.left;
            List<Pair<SetComment, Integer>> list = tblName2ColComments.get(getKey(createTable));
            if (CollectionUtils.isEmpty(list)) {
                return new Pair<>(p, createTable.getColumnDefinitions());
            }
            return new Pair<>(p, createTable.getColumnDefinitions().stream().filter(d -> {
                String colName = SqlCheckUtil.unquoteOracleIdentifier(d.getColumnReference().getColumn());
                return list.stream().noneMatch(p1 -> StringUtils.equals(colName,
                        SqlCheckUtil.unquoteOracleIdentifier(p1.left.getColumn().getColumn())));
            }).collect(Collectors.toList()));
        }).flatMap(p -> {
            Integer offset = p.left.right;
            String text = p.left.left.getText();
            return p.right.stream().map(d -> SqlCheckUtil.buildViolation(text, d, getType(), offset,
                    new Object[] {d.getColumnReference().getColumn()}));
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

