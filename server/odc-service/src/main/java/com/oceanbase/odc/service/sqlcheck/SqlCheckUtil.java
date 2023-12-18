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
package com.oceanbase.odc.service.sqlcheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.sqlcheck.model.CheckResult;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.SortColumn;
import com.oceanbase.tools.sqlparser.statement.delete.Delete;
import com.oceanbase.tools.sqlparser.statement.expression.CollectionExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;
import com.oceanbase.tools.sqlparser.statement.select.RelatedSelectBody;
import com.oceanbase.tools.sqlparser.statement.select.Select;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;
import com.oceanbase.tools.sqlparser.statement.update.Update;

import lombok.NonNull;

/**
 * {@link SqlCheckUtil}
 *
 * @author yh263208
 * @date 2022-12-14 16:47
 * @since ODC_release_4.1.0
 */
public class SqlCheckUtil {

    public static List<Expression> findAll(@NonNull Expression expr,
            @NonNull Predicate<Expression> predicate) {
        if (predicate.test(expr)) {
            List<Expression> list = new ArrayList<>();
            list.add(expr);
            return list;
        }
        if (!(expr instanceof CompoundExpression) && !(expr instanceof CollectionExpression)) {
            return new ArrayList<>();
        }
        if (expr instanceof CollectionExpression) {
            return ((CollectionExpression) expr).getExpressionList().stream()
                    .flatMap(e -> findAll(e, predicate).stream()).collect(Collectors.toList());
        }
        CompoundExpression c = (CompoundExpression) expr;
        List<Expression> list = findAll(c.getLeft(), predicate);
        if (c.getRight() != null) {
            list.addAll(findAll(c.getRight(), predicate));
        }
        return list;
    }

    public static boolean allMatch(@NonNull Expression expr, @NonNull Predicate<Expression> predicate) {
        if (!predicate.test(expr)) {
            return false;
        }
        if (!(expr instanceof CompoundExpression) && !(expr instanceof CollectionExpression)) {
            return true;
        }
        if (expr instanceof CollectionExpression) {
            return ((CollectionExpression) expr).getExpressionList().stream().allMatch(predicate);
        }
        CompoundExpression c = (CompoundExpression) expr;
        boolean matched = allMatch(c.getLeft(), predicate);
        if (!matched) {
            return false;
        }
        if (c.getRight() != null) {
            matched = allMatch(c.getRight(), predicate);
        }
        return matched;
    }

    public static List<Expression> getWhereClauses(Statement stmt) {
        if (stmt instanceof Select) {
            Select statement = (Select) stmt;
            SelectBody select = statement.getSelectBody();
            List<Expression> where = new ArrayList<>();
            where.add(select.getWhere());
            /**
             * 避免死循环
             */
            int exceed = 1000;
            RelatedSelectBody related = select.getRelatedSelect();
            while (related != null && (--exceed) > 0) {
                // 如果 select 语句 union 了其他 select，这里需要挨个检查 where 条件
                select = related.getSelect();
                where.add(select.getWhere());
                related = select.getRelatedSelect();
            }
            return where.stream().filter(Objects::nonNull).collect(Collectors.toList());
        } else if (stmt instanceof Update) {
            Update update = (Update) stmt;
            if (update.getWhere() != null) {
                return Collections.singletonList(update.getWhere());
            }
            return Collections.emptyList();
        } else if (stmt instanceof Delete) {
            Delete delete = (Delete) stmt;
            if (delete.getWhere() != null) {
                return Collections.singletonList(delete.getWhere());
            }
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    public static CheckViolation buildViolation(String originSql, Statement stmt,
            SqlCheckRuleType ruleType, Object[] args) {
        return new CheckViolation(originSql, stmt.getLine(),
                stmt.getCharPositionInLine(), stmt.getStart(), stmt.getStop(),
                ruleType, args == null ? new Object[] {} : args);
    }

    public static CheckViolation buildViolation(String originSql, Statement stmt,
            SqlCheckRuleType ruleType, Integer offset, Object[] args) {
        return new CheckViolation(originSql, stmt.getLine(),
                stmt.getCharPositionInLine(), stmt.getStart(), stmt.getStop(),
                ruleType, offset, args == null ? new Object[] {} : args);
    }

    public static List<CheckResult> buildCheckResults(@NonNull List<CheckViolation> violations) {
        return violations.stream().collect(Collectors.groupingBy(CheckViolation::getOffset)).entrySet()
                .stream()
                .sorted(Entry.comparingByKey())
                .map(i -> new CheckResult(i.getValue().get(0).getText(), i.getValue()))
                .collect(Collectors.toList());
    }

    public static Stream<ColumnDefinition> fromAlterTable(@NonNull AlterTable alterTable) {
        return alterTable.getAlterTableActions().stream()
                .filter(a -> {
                    if (CollectionUtils.isNotEmpty(a.getAddColumns())) {
                        return true;
                    } else if (CollectionUtils.isNotEmpty(a.getModifyColumns())) {
                        return true;
                    }
                    return a.getChangColumnDefinition() != null;
                }).flatMap(a -> {
                    if (CollectionUtils.isNotEmpty(a.getAddColumns())) {
                        return a.getAddColumns().stream();
                    } else if (CollectionUtils.isNotEmpty(a.getModifyColumns())) {
                        return a.getModifyColumns().stream();
                    }
                    return Stream.of(a.getChangColumnDefinition());
                });
    }

    public static String unquoteMySQLIdentifier(String identifier) {
        return identifier == null ? null : StringUtils.unquoteMySqlIdentifier(identifier);
    }

    public static String unquoteOracleIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        if (!StringUtils.startsWith(identifier, "\"") && !StringUtils.endsWith(identifier, "\"")) {
            return identifier.toUpperCase();
        }
        return StringUtils.unquoteOracleIdentifier(identifier);
    }

    public static String generateDefaultIndexName(String prefix, @NonNull String tableName,
            @NonNull List<SortColumn> columns) {
        String tbName = StringUtils.unquoteMySqlIdentifier(tableName);
        tbName = StringUtils.unquoteOracleIdentifier(tbName);
        AtomicInteger counter = new AtomicInteger(0);
        String p = prefix == null ? "" : (prefix + "_");
        return p + tbName + "_" + columns.stream().map(c -> {
            Expression e = c.getColumn();
            if (e instanceof ColumnReference) {
                String tmp = StringUtils.unquoteMySqlIdentifier(((ColumnReference) e).getColumn());
                return StringUtils.unquoteOracleIdentifier(tmp);
            } else if (e instanceof RelationReference) {
                String tmp = StringUtils.unquoteMySqlIdentifier(getColumnName((RelationReference) e));
                return StringUtils.unquoteOracleIdentifier(tmp);
            }
            return "expr" + counter.getAndIncrement();
        }).collect(Collectors.joining("_"));
    }

    public static String getColumnName(@NonNull RelationReference r) {
        String tmp = null;
        while (r != null) {
            tmp = r.getRelationName();
            if (r.getReference() == null) {
                break;
            }
            if (!(r.getReference() instanceof RelationReference)) {
                break;
            }
            r = (RelationReference) r.getReference();
        }
        return tmp;
    }

}
