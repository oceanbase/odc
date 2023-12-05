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
import java.util.regex.Pattern;
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
import com.oceanbase.tools.sqlparser.statement.createindex.CreateIndex;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineIndex;
import com.oceanbase.tools.sqlparser.statement.createtable.SortColumn;

import lombok.NonNull;

/**
 * {@link RestrictIndexNaming}
 *
 * @author yh263208
 * @date 2023-06-20 19:33
 * @since ODC_release_4.2.0
 */
public class RestrictIndexNaming implements SqlCheckRule {

    private final Pattern namePattern;

    public RestrictIndexNaming(String namePattern) {
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
        return SqlCheckRuleType.RESTRICT_INDEX_NAMING;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        int offset = context.getStatementOffset(statement);
        if (statement instanceof CreateTable) {
            CreateTable createTable = (CreateTable) statement;
            /**
             * 建表 DDL 中存在 out of line constraint
             *
             * <code>
             *     CREATE TABLE "CHZ"."TEST_TB" (
             *         "ID" VARCHAR2(64),
             *         CONSTRAINT "UK_AAA" UNIQUE ("ID")
             *     )
             * </code>
             */
            List<Statement> statements = createTable.getIndexes().stream().filter(c -> {
                if (c.getIndexName() == null) {
                    return false;
                }
                return !matches(createTable.getTableName(), c.getIndexName(), c.getColumns());
            }).collect(Collectors.toList());
            return statements.stream().map(s -> {
                OutOfLineIndex c = (OutOfLineIndex) s;
                return SqlCheckUtil.buildViolation(statement.getText(), s, getType(), offset,
                        new Object[] {c.getIndexName(), getPattern()});
            }).collect(Collectors.toList());
        } else if (statement instanceof CreateIndex) {
            CreateIndex createIndex = (CreateIndex) statement;
            if (createIndex.isUnique()) {
                return Collections.emptyList();
            }
            if (matches(createIndex.getOn().getRelation(), createIndex.getRelation().getRelation(),
                    createIndex.getColumns())) {
                return Collections.emptyList();
            }
            return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(),
                    createIndex, getType(), offset,
                    new Object[] {createIndex.getRelation().getRelation(), getPattern()}));
        } else if (statement instanceof AlterTable) {
            AlterTable alterTable = (AlterTable) statement;
            return alterTable.getAlterTableActions().stream().filter(alterTableAction -> {
                OutOfLineIndex c = alterTableAction.getAddIndex();
                if (c == null) {
                    return false;
                }
                return c.getIndexName() != null
                        && !matches(alterTable.getTableName(), c.getIndexName(), c.getColumns());
            }).map(a -> {
                OutOfLineIndex c = a.getAddIndex();
                return SqlCheckUtil.buildViolation(statement.getText(), a, getType(), offset,
                        new Object[] {c.getIndexName(), getPattern()});
            }).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.MYSQL, DialectType.OB_MYSQL, DialectType.OB_ORACLE,
                DialectType.ODP_SHARDING_OB_MYSQL);
    }

    private boolean matches(String tableName, String indexName, List<SortColumn> columns) {
        String dest = StringUtils.unquoteMySqlIdentifier(indexName);
        dest = StringUtils.unquoteOracleIdentifier(dest);
        if (this.namePattern != null) {
            return namePattern.matcher(dest).matches();
        }
        return SqlCheckUtil.generateDefaultIndexName("idx", tableName, columns).equalsIgnoreCase(dest);
    }

    private String getPattern() {
        if (this.namePattern != null) {
            return this.namePattern.toString();
        }
        return "idx_${table-name}_${column-name-1}_${column-name-2}_...";
    }

}
