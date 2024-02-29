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

package com.oceanbase.tools.sqlparser.statement.insert;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;
import com.oceanbase.tools.sqlparser.statement.expression.BaseExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.insert.mysql.SetColumn;
import com.oceanbase.tools.sqlparser.statement.select.PartitionUsage;
import com.oceanbase.tools.sqlparser.statement.select.Select;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link InsertTable}
 *
 * @author yh263208
 * @date 2023-11-08 17:20
 * @since ODC_release_4.2.3
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class InsertTable extends BaseStatement {

    private boolean nologging;
    private String alias;
    private PartitionUsage partitionUsage;
    private List<SetColumn> setColumns = Collections.emptyList();
    private List<ColumnReference> columns = Collections.emptyList();
    private List<List<Expression>> values = Collections.emptyList();
    private List<String> aliasColumns;
    private final SelectBody select;
    private final RelationFactor table;

    public InsertTable(@NonNull ParserRuleContext context,
            @NonNull RelationFactor table) {
        super(context);
        this.table = table;
        this.select = null;
    }

    public InsertTable(@NonNull ParserRuleContext context,
            @NonNull SelectBody select) {
        super(context);
        this.table = null;
        this.select = select;
    }

    public InsertTable(@NonNull TerminalNode begin,
            @NonNull ParserRuleContext end, @NonNull RelationFactor table) {
        super(begin, end);
        this.table = table;
        this.select = null;
    }

    public InsertTable(@NonNull TerminalNode begin,
            @NonNull ParserRuleContext end, @NonNull SelectBody select) {
        super(begin, end);
        this.table = null;
        this.select = select;
    }

    public InsertTable(@NonNull RelationFactor table) {
        this.table = table;
        this.select = null;
    }

    public InsertTable(@NonNull SelectBody select) {
        this.table = null;
        this.select = select;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("INTO");
        if (this.table != null) {
            builder.append(" ").append(this.table);
            if (this.partitionUsage != null) {
                builder.append(" ").append(this.partitionUsage);
            }
        } else if (this.select != null) {
            builder.append(" (").append(this.select).append(")");
        }
        if (this.alias != null && CollectionUtils.isEmpty(this.aliasColumns)) {
            builder.append(" ").append(this.alias);
        }
        if (this.nologging) {
            builder.append(" NOLOGGING");
        }
        if (CollectionUtils.isNotEmpty(this.columns)) {
            builder.append(" (").append(this.columns.stream()
                    .map(BaseExpression::toString).collect(Collectors.joining(",")))
                    .append(")");
        }
        if (CollectionUtils.isNotEmpty(this.values)) {
            if (this.values.size() == 1 && this.values.get(0).size() == 1) {
                Expression value = this.values.get(0).get(0);
                if ((value instanceof Select) || (value instanceof SelectBody)) {
                    builder.append(" ").append(value);
                } else {
                    builder.append(" VALUES ").append(value);
                }
            } else {
                builder.append(" VALUES ").append(this.values.stream()
                        .map(e -> "(" + e.stream().map(Object::toString)
                                .collect(Collectors.joining(",")) + ")")
                        .collect(Collectors.joining(",")));
            }
        } else if (CollectionUtils.isNotEmpty(this.setColumns)) {
            builder.append(" SET ").append(this.setColumns.stream()
                    .map(SetColumn::toString).collect(Collectors.joining(",")));
        }
        if (this.alias != null && CollectionUtils.isNotEmpty(this.aliasColumns)) {
            builder.append(" AS ").append(this.alias).append("(").append(String.join(",", this.aliasColumns))
                    .append(")");
        }
        return builder.toString();
    }

}
